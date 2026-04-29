package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartActivity extends AppCompatActivity {

    ImageButton backBtn;
    Button previousBtn, nextBtn, editBtn, addBtn;
    TextView tvMonthLabel, tvSummaryMonth, tvTotalIncome, tvTotalExpenses, tvNetBalance;
    WebView barChartView, pieChartView, lineChartView;
    SmartPercentageDivider divider = new SmartPercentageDivider();
    LinearLayout categoryContainer;

    int currentOffset = 0;

    float totalFixedExpenses = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        backBtn           = (ImageButton) findViewById(R.id.backBtn);
        previousBtn       = (Button)      findViewById(R.id.previousBtn);
        nextBtn           = (Button)      findViewById(R.id.nextBtn);
        editBtn           = (Button)      findViewById(R.id.btnEditAll);
        addBtn            = (Button)      findViewById(R.id.btnAddCategory);
        tvMonthLabel      = (TextView)    findViewById(R.id.tvMonthLabel);
        tvSummaryMonth    = (TextView)    findViewById(R.id.tvSummaryMonth);
        tvTotalIncome     = (TextView)    findViewById(R.id.tvTotalIncome);
        tvTotalExpenses   = (TextView)    findViewById(R.id.tvTotalExpenses);
        tvNetBalance      = (TextView)    findViewById(R.id.tvNetBalance);
        barChartView      = (WebView)     findViewById(R.id.barChartView);
        pieChartView      = (WebView)     findViewById(R.id.pieChartView);
        lineChartView     = (WebView)     findViewById(R.id.lineChartView);
        categoryContainer = findViewById(R.id.categoryContainer);

        setupWebView(barChartView);
        setupWebView(pieChartView);
        setupWebView(lineChartView);

        loadFixedExpenseData();
        loadChartData();

        backBtn.setOnClickListener(v -> finish());

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
                percent.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
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
                List<Category> updated = new java.util.ArrayList<>();
                for (int i = 0; i < editContainer.getChildCount(); i++) {
                    LinearLayout row = (LinearLayout) editContainer.getChildAt(i);
                    Spinner spinner  = (Spinner)  row.getChildAt(0);
                    EditText percent = (EditText) row.getChildAt(1);
                    String valueStr  = percent.getText().toString().trim();
                    if (valueStr.isEmpty()) {
                        Toast.makeText(this, "Fill all percentages", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updated.add(new Category(spinner.getSelectedItem().toString(),
                            Double.parseDouble(valueStr)));
                }
                divider.setAllDividers(updated);
                renderCategories();
                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        });

        addBtn.setOnClickListener(v -> {
            View addView       = getLayoutInflater().inflate(R.layout.dialog_add_divider, null);
            Spinner spCategory = addView.findViewById(R.id.spCategory);
            EditText etPercent = addView.findViewById(R.id.etPercent);
            Button btnAdd      = addView.findViewById(R.id.btnAdd);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this, R.array.expense_categories, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCategory.setAdapter(adapter);

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

                if (percent < 100 && divider.getAllDividers().isEmpty())
                    divider.addDivider("Other", 100 - percent);

                for (Category c : divider.getAllDividers()) {
                    if (c.getCategory().equals(name)) {
                        Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                divider.addDivider(name, percent);
                renderCategories();
                Toast.makeText(this, "Category Added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        });
    }

    // Pull fixed expenses

    private void loadFixedExpenseData() {
        totalFixedExpenses = 0f;

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        ExpenseList expenseList = user.getExpenseList();
        if (expenseList == null) return;

        for (FixedExpense expense : expenseList.getAllExpenses()) {
            totalFixedExpenses += (float) expense.getAmount();
        }
    }

    // Render

    private void renderCategories() {
        categoryContainer.removeAllViews();
        for (Category c : divider.getAllDividers()) {
            TextView tv = new TextView(this);
            tv.setText(c.getCategory() + " - "
                    + String.format("%.0f%%", c.getPercentage())
                    + " | Balance: €" + String.format("%.2f", c.getBalance())
                    + " | Last: €"    + String.format("%.2f", c.getLastAllocation()));
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

    private void updateSummaryStats(float totalIncome, float totalExpenses, String monthLabel) {
        float net = totalIncome - totalExpenses;
        tvSummaryMonth.setText(monthLabel);
        tvTotalIncome.setText(String.format(Locale.getDefault(), "€%.2f", totalIncome));
        tvTotalExpenses.setText(String.format(Locale.getDefault(), "€%.2f", totalExpenses));
        tvNetBalance.setText(String.format(Locale.getDefault(), "€%.2f", net));
        tvNetBalance.setTextColor(net >= 0
                ? getResources().getColor(android.R.color.holo_green_light, getTheme())
                : getResources().getColor(android.R.color.holo_red_light, getTheme()));
    }

    // Load Firebase data

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
        String monthLabel = fmt.format(monthStart);
        tvMonthLabel.setText(monthLabel);

        nextBtn.setEnabled(currentOffset < 0);
        nextBtn.setAlpha(currentOffset < 0 ? 1f : 0.4f);

        int weeks = getWeeksInMonth(monthStart);
        float[] incomeByWeek     = new float[weeks];
        float[] expenseByWeek    = new float[weeks];
        float[] incomeByWeekLine = new float[weeks];

        // pie[0] = Housing, pie[1] = Subscriptions, pie[2] = Utilities, pie[3] = Other
        float[] pie = {0f, 0f, 0f, 0f};

        // Spread fixed expenses evenly across weeks
        float fixedPerWeek  = totalFixedExpenses / weeks;
        float[] fixedByWeek = new float[weeks];
        for (int i = 0; i < weeks; i++) fixedByWeek[i] = fixedPerWeek;

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(fbUser.getUid())
                .collection("transactions")
                .whereGreaterThanOrEqualTo("createdAt", monthStart)
                .whereLessThanOrEqualTo("createdAt", monthEnd)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        updateSummaryStats(0f, 0f, monthLabel);
                        loadBarChart(barChartView, "'Wk 1'", "[0]", "[0]",
                                "[" + fixedPerWeek + "]");
                        loadPieChart(pieChartView, new float[]{0, 0, 0, 0});
                        loadLineChart(lineChartView, "'Wk 1'", "[0]");
                        return;
                    }

                    float totalIncome = 0f, totalExpenses = 0f;

                    for (QueryDocumentSnapshot doc : snap) {
                        Date   date = doc.getDate("createdAt");
                        Double amt  = doc.getDouble("amount");
                        String type = doc.getString("type");
                        String cat  = doc.getString("category");
                        if (date == null || amt == null || type == null) continue;

                        int week = (int) ((date.getTime() - monthStart.getTime())
                                / (7L * 24 * 60 * 60 * 1000));
                        week = Math.max(0, Math.min(week, weeks - 1));

                        if ("Income".equals(type)) {
                            incomeByWeek[week]     += amt.floatValue();
                            incomeByWeekLine[week] += amt.floatValue();
                            totalIncome            += amt.floatValue();
                        }
                        if ("Expense".equals(type)) {
                            expenseByWeek[week] += amt.floatValue();
                            totalExpenses       += amt.floatValue();
                            if (cat != null) {
                                // Match exactly against the category strings shown in the app
                                switch (cat) {
                                    case "Housing":
                                        pie[0] += amt.floatValue(); break;
                                    case "Subscriptions":
                                        pie[1] += amt.floatValue(); break;
                                    case "Utilities":
                                        pie[2] += amt.floatValue(); break;
                                    default:
                                        pie[3] += amt.floatValue(); break;
                                }
                            }
                        }
                    }

                    updateSummaryStats(totalIncome, totalExpenses, monthLabel);

                    StringBuilder weekLabels = new StringBuilder();
                    for (int i = 0; i < weeks; i++) {
                        if (i > 0) weekLabels.append(",");
                        weekLabels.append("'Wk ").append(i + 1).append("'");
                    }

                    loadBarChart(barChartView, weekLabels.toString(),
                            toJsArray(incomeByWeek), toJsArray(expenseByWeek),
                            toJsArray(fixedByWeek));
                    loadPieChart(pieChartView, pie);
                    loadLineChart(lineChartView, weekLabels.toString(),
                            toJsArray(incomeByWeekLine));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
                );
    }

    // Just before the helper methods these methods load chart

    private void loadBarChart(WebView wv, String labels,
                              String incomeData, String expenseData,
                              String fixedData) {
        String html = "<!DOCTYPE html><html><head>"
                + "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>"
                + "<style>body{margin:0;background:#1C1C1E;}canvas{width:100%!important;}</style>"
                + "</head><body><canvas id='c'></canvas><script>"
                + "new Chart(document.getElementById('c'),{"
                + "type:'bar',"
                + "data:{labels:[" + labels + "],"
                + "datasets:["
                + "{label:'Income',data:" + incomeData + ",backgroundColor:'#1D9E75',borderRadius:6},"
                + "{label:'Daily Spend',data:" + expenseData + ",backgroundColor:'#D85A30',borderRadius:6},"
                + "{label:'Fixed Bills',data:" + fixedData + ",backgroundColor:'#534AB7',borderRadius:6}"
                + "]},"
                + "options:{responsive:true,plugins:{legend:{labels:{color:'#fff'}}},"
                + "scales:{x:{ticks:{color:'#fff'},grid:{color:'#333'}},"
                + "y:{ticks:{color:'#fff'},grid:{color:'#333'}}}}});"
                + "</script></body></html>";
        wv.loadDataWithBaseURL("https://cdn.jsdelivr.net", html, "text/html", "UTF-8", null);
    }

    private void loadPieChart(WebView wv, float[] pie) {
        String html = "<!DOCTYPE html><html><head>"
                + "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>"
                + "<style>body{margin:0;background:#1C1C1E;}canvas{width:100%!important;max-height:160px;}</style>"
                + "</head><body><canvas id='c'></canvas><script>"
                + "new Chart(document.getElementById('c'),{"
                + "type:'doughnut',"
                + "data:{labels:['Housing','Subscriptions','Utilities','Other'],"
                + "datasets:[{data:[" + pie[0] + "," + pie[1] + "," + pie[2] + "," + pie[3] + "],"
                + "backgroundColor:['#534AB7','#1D9E75','#D85A30','#BA7517'],"
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
                + "</head><body><canvas id='c'></canvas><script>"
                + "new Chart(document.getElementById('c'),{"
                + "type:'line',"
                + "data:{labels:[" + labels + "],"
                + "datasets:[{"
                + "label:'Income',data:" + incomeData + ","
                + "borderColor:'#1D9E75',backgroundColor:'rgba(29,158,117,0.15)',"
                + "pointBackgroundColor:'#1D9E75',tension:0.4,fill:true"
                + "}]},"
                + "options:{responsive:true,plugins:{legend:{labels:{color:'#fff'}}},"
                + "scales:{x:{ticks:{color:'#fff'},grid:{color:'#333'}},"
                + "y:{ticks:{color:'#fff'},grid:{color:'#333'}}}}});"
                + "</script></body></html>";
        wv.loadDataWithBaseURL("https://cdn.jsdelivr.net", html, "text/html", "UTF-8", null);
    }

    // Helper methods below

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