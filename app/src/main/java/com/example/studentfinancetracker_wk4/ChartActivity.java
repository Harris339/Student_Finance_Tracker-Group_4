package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChartActivity extends AppCompatActivity {

    Button previousBtn, nextBtn, backBtn, editBtn, addBtn;
    TextView tvMonthLabel;
    WebView barChartView, pieChartView, lineChartView;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser user = mAuth.getCurrentUser();
    User sessionUser = SessionManager.getInstance().getCurrentUser();
    SmartPercentageDivider divider = sessionUser.getPercDivider();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    LinearLayout categoryContainer;

    int currentOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        previousBtn       = (Button)   findViewById(R.id.previousBtn);
        nextBtn           = (Button)   findViewById(R.id.nextBtn);
        backBtn           = (Button)   findViewById(R.id.backBtn);
        editBtn           = (Button)   findViewById(R.id.btnEditAll);
        addBtn            = (Button)   findViewById(R.id.btnAddCategory);
        tvMonthLabel      = (TextView) findViewById(R.id.tvMonthLabel);
        barChartView      = (WebView)  findViewById(R.id.barChartView);
        pieChartView      = (WebView)  findViewById(R.id.pieChartView);
        lineChartView     = (WebView)  findViewById(R.id.lineChartView);
        categoryContainer = findViewById(R.id.categoryContainer);

        setupWebView(barChartView);
        setupWebView(pieChartView);
        setupWebView(lineChartView);

        // Only call loadDividerFromFirebase() — it calls loadChartData() once data is ready
        loadDividerFromFirebase();

        previousBtn.setOnClickListener(v -> {
            currentOffset--;
            loadChartData();
        });

        nextBtn.setOnClickListener(v -> {
            if (currentOffset < 0) {
                currentOffset++;
                loadChartData();
            }
        });

        backBtn.setOnClickListener(v -> finish());

        editBtn.setOnClickListener(v -> {
            View editView = getLayoutInflater().inflate(R.layout.dialog_edit_divider, null);

            LinearLayout editContainer = editView.findViewById(R.id.editContainer);
            Button btnSave = editView.findViewById(R.id.btnSave);

            for (Category c : divider.getAllDividers()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 8, 0, 8);

                Spinner spinner = new Spinner(this);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                        this, R.array.expense_categories, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                spinner.setSelection(adapter.getPosition(c.getCategory()));

                EditText percent = new EditText(this);
                percent.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                percent.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                percent.setText(String.valueOf(c.getPercentage()));

                row.addView(spinner);
                row.addView(percent);
                editContainer.addView(row);
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Edit Categories")
                    .setView(editView)
                    .create();

            btnSave.setOnClickListener(v2 -> {
                int childCount = editContainer.getChildCount();
                List<Category> updated = new ArrayList<>();

                for (int i = 0; i < childCount; i++) {
                    LinearLayout row = (LinearLayout) editContainer.getChildAt(i);
                    Spinner spinner  = (Spinner)      row.getChildAt(0);
                    EditText percent = (EditText)      row.getChildAt(1);

                    String category = spinner.getSelectedItem().toString();
                    String valueStr = percent.getText().toString().trim();
                    if (valueStr.isEmpty()) {
                        Toast.makeText(this, "Fill all percentages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double value = Double.parseDouble(valueStr);
                    updated.add(new Category(category, value));
                }

                divider.setAllDividers(updated);
                loadDividerFromFirebase();
                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        });

        addBtn.setOnClickListener(v -> {
            View addView = getLayoutInflater().inflate(R.layout.dialog_add_divider, null);

            Spinner  spCategory = addView.findViewById(R.id.spCategory);
            EditText etPercent  = addView.findViewById(R.id.etPercent);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this, R.array.expense_categories, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCategory.setAdapter(adapter);

            Button btnAdd = addView.findViewById(R.id.btnAdd);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Add Category")
                    .setView(addView)
                    .create();

            btnAdd.setOnClickListener(v2 -> {
                String name       = spCategory.getSelectedItem().toString();
                String percentStr = etPercent.getText().toString().trim();

                if (name.isEmpty() || percentStr.isEmpty()) {
                    Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                double percent;
                try {
                    percent = Double.parseDouble(percentStr);
                } catch (Exception e) {
                    Toast.makeText(this, "Invalid percentage", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (percent <= 0 || percent > 100) {
                    Toast.makeText(this, "Enter 1–100%", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (Category c : divider.getAllDividers()) {
                    if (c.getCategory().equals(name)) {
                        Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                Map<String, Object> category = new HashMap<>();
                category.put("percentage", percent);
                category.put("balance", 0);
                category.put("lastAllocation", 0);

                db.collection("users")
                        .document(user.getUid())
                        .collection("percDividers")
                        .document(name)
                        .set(category);

                divider.addDivider(name, percent);
                loadDividerFromFirebase();
                Toast.makeText(this, "Category Added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        });
    }

    private void loadDividerFromFirebase() {
        db.collection("users")
                .document(user.getUid())
                .collection("percDividers")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Category> list = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name    = doc.getId();
                        Double percent = doc.getDouble("percentage");
                        Double balance = doc.getDouble("balance");
                        Double last    = doc.getDouble("lastAllocation");

                        if (percent == null) percent = 0.0;
                        if (balance == null) balance = 0.0;
                        if (last    == null) last    = 0.0;

                        Category c = new Category(name, percent);
                        c.setBalance(balance);
                        c.setLastAllocation(last);
                        list.add(c);
                    }

                    divider.setAllDividers(list);
                    renderCategories();
                    // loadChartData() called here ONLY — after divider is populated
                    loadChartData();
                });
    }

    private void renderCategories() {
        categoryContainer.removeAllViews();
        for (Category c : divider.getAllDividers()) {
            TextView tv = new TextView(this);
            tv.setText(c.getCategory() + " - "
                    + String.format("%.0f%%", c.getPercentage())
                    + " | Balance: €" + String.format("%.2f", c.getBalance())
                    + " | Last: €" + String.format("%.2f", c.getLastAllocation()));
            tv.setTextSize(16);
            tv.setPadding(8, 8, 8, 8);
            categoryContainer.addView(tv);
        }
    }

    private void setupWebView(WebView wv) {
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient());
        wv.setBackgroundColor(0x00000000);
    }

    private void loadChartData() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MONTH, currentOffset);
        Date monthStart = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date monthEnd = cal.getTime();

        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthLabel.setText(fmt.format(monthStart));

        nextBtn.setEnabled(currentOffset < 0);
        nextBtn.setAlpha(currentOffset < 0 ? 1f : 0.4f);

        int weeks = getWeeksInMonth(monthStart);
        float[] incomeByWeek     = new float[weeks];
        float[] expenseByWeek    = new float[weeks];
        float[] incomeByWeekLine = new float[weeks];

        // Pie chart reads directly from SmartPercentage balances
        List<Category> cats = new ArrayList<>(divider.getAllDividers());
        float[] pie = new float[cats.size()];
        for (int i = 0; i < cats.size(); i++) {
            pie[i] = (float) cats.get(i).getBalance();
        }
        loadPieChart(pieChartView, pie, cats);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        // Bar and Line charts read from transactions
        FirebaseFirestore.getInstance()
                .collection("users").document(firebaseUser.getUid())
                .collection("transactions")
                .whereGreaterThanOrEqualTo("createdAt", monthStart)
                .whereLessThanOrEqualTo("createdAt", monthEnd)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        loadBarChart(barChartView, "'Wk 1'", "[0]", "[0]");
                        loadLineChart(lineChartView, "'Wk 1'", "[0]");
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snap) {
                        Date date   = doc.getDate("createdAt");
                        Double amt  = doc.getDouble("amount");
                        String type = doc.getString("type");
                        if (date == null || amt == null || type == null) continue;

                        int week = (int) ((date.getTime() - monthStart.getTime())
                                / (7L * 24 * 60 * 60 * 1000));
                        week = Math.max(0, Math.min(week, weeks - 1));

                        if ("Income".equals(type)) {
                            incomeByWeek[week] += amt.floatValue();
                            incomeByWeekLine[week] += amt.floatValue();
                        }
                        if ("Expense".equals(type)) {
                            expenseByWeek[week] += amt.floatValue();
                        }
                    }

                    StringBuilder weekLabels = new StringBuilder();
                    for (int i = 0; i < weeks; i++) {
                        if (i > 0) weekLabels.append(",");
                        weekLabels.append("'Wk ").append(i + 1).append("'");
                    }

                    loadBarChart(barChartView, weekLabels.toString(),
                            toJsArray(incomeByWeek), toJsArray(expenseByWeek));
                    loadLineChart(lineChartView, weekLabels.toString(),
                            toJsArray(incomeByWeekLine));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load chart data: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void loadBarChart(WebView wv, String labels, String incomeData, String expenseData) {
        String html = "<!DOCTYPE html><html><head>"
                + "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>"
                + "<style>body{margin:0;background:#1C1C1E;}canvas{width:100%!important;}</style>"
                + "</head><body>"
                + "<canvas id='c'></canvas><script>"
                + "new Chart(document.getElementById('c'),{"
                + "type:'bar',"
                + "data:{labels:[" + labels + "],"
                + "datasets:["
                + "{label:'Income',data:" + incomeData + ",backgroundColor:'#1D9E75',borderRadius:6},"
                + "{label:'Expenses',data:" + expenseData + ",backgroundColor:'#D85A30',borderRadius:6}"
                + "]},"
                + "options:{responsive:true,plugins:{legend:{labels:{color:'#fff'}}},"
                + "scales:{x:{ticks:{color:'#fff'},grid:{color:'#333'}},"
                + "y:{ticks:{color:'#fff'},grid:{color:'#333'}}}}});"
                + "</script></body></html>";
        wv.loadDataWithBaseURL("https://cdn.jsdelivr.net", html, "text/html", "UTF-8", null);
    }

    private void loadPieChart(WebView wv, float[] pie, List<Category> cats) {
        String[] colors = {
                "#534AB7", "#1D9E75", "#D85A30", "#BA7517",
                "#E91E63", "#FF9800", "#00BCD4", "#9C27B0"
        };

        StringBuilder labels   = new StringBuilder();
        StringBuilder data     = new StringBuilder();
        StringBuilder colorArr = new StringBuilder();

        for (int i = 0; i < cats.size(); i++) {
            if (i > 0) { labels.append(","); data.append(","); colorArr.append(","); }
            labels.append("'").append(cats.get(i).getCategory()).append("'");
            data.append(String.format(Locale.US, "%.2f", pie[i]));
            colorArr.append("'").append(colors[i % colors.length]).append("'");
        }

        String html = "<!DOCTYPE html><html><head>"
                + "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>"
                + "<style>body{margin:0;background:#1C1C1E;}canvas{width:100%!important;max-height:160px;}</style>"
                + "</head><body>"
                + "<canvas id='c'></canvas><script>"
                + "new Chart(document.getElementById('c'),{"
                + "type:'doughnut',"
                + "data:{labels:[" + labels + "],"
                + "datasets:[{data:[" + data + "],"
                + "backgroundColor:[" + colorArr + "],"
                + "borderWidth:0}]},"
                + "options:{responsive:true,plugins:{legend:"
                + "{position:'bottom',labels:{color:'#fff',padding:16}}}}});"
                + "</script></body></html>";
        wv.loadDataWithBaseURL("https://cdn.jsdelivr.net", html, "text/html", "UTF-8", null);
    }

    private void loadLineChart(WebView wv, String labels, String incomeData) {
        String html = "<!DOCTYPE html><html><head>"
                + "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>"
                + "<style>body{margin:0;background:#1C1C1E;}canvas{width:100%!important;}</style>"
                + "</head><body>"
                + "<canvas id='c'></canvas><script>"
                + "new Chart(document.getElementById('c'),{"
                + "type:'line',"
                + "data:{labels:[" + labels + "],"
                + "datasets:[{label:'Income',data:" + incomeData + ","
                + "borderColor:'#1D9E75',backgroundColor:'rgba(29,158,117,0.15)',"
                + "pointBackgroundColor:'#1D9E75',tension:0.4,fill:true}]},"
                + "options:{responsive:true,plugins:{legend:{labels:{color:'#fff'}}},"
                + "scales:{x:{ticks:{color:'#fff'},grid:{color:'#333'}},"
                + "y:{ticks:{color:'#fff'},grid:{color:'#333'}}}}});"
                + "</script></body></html>";
        wv.loadDataWithBaseURL("https://cdn.jsdelivr.net", html, "text/html", "UTF-8", null);
    }

    private String toJsArray(float[] data) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < data.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(Locale.US, "%.2f", data[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private int getWeeksInMonth(Date monthStart) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(monthStart);
        return (int) Math.ceil(cal.getActualMaximum(Calendar.DAY_OF_MONTH) / 7.0);
    }
}