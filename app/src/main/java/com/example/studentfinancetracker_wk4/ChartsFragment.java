package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChartsFragment extends Fragment {

    private Button previousBtn, nextBtn, editBtn, addBtn;
    private TextView tvMonthLabel;
    private WebView barChartView, pieChartView, lineChartView;
    private LinearLayout categoryContainer;

    private User user;
    private SmartPercentageDivider divider;
    private FirebaseFirestore db;

    private int currentOffset = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        divider = user.getPercDivider();

        previousBtn       = view.findViewById(R.id.previousBtn);
        nextBtn           = view.findViewById(R.id.nextBtn);
        editBtn           = view.findViewById(R.id.btnEditAll);
        addBtn            = view.findViewById(R.id.btnAddCategory);
        tvMonthLabel      = view.findViewById(R.id.tvMonthLabel);
        barChartView      = view.findViewById(R.id.barChartView);
        pieChartView      = view.findViewById(R.id.pieChartView);
        lineChartView     = view.findViewById(R.id.lineChartView);
        categoryContainer = view.findViewById(R.id.categoryContainer);

        Button backBtn = view.findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setVisibility(View.GONE);

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

        editBtn.setOnClickListener(v -> {
            View editView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_edit_divider, null);

            LinearLayout editContainer = editView.findViewById(R.id.editContainer);
            Button btnSave = editView.findViewById(R.id.btnSave);

            Set<Integer> editedIndices = new HashSet<>();
            Set<String> removedCategories = new HashSet<>();

            List<Category> existing = new ArrayList<>(divider.getAllDividers());

            for (int i = 0; i < existing.size(); i++) {
                Category c = existing.get(i);
                final int index = i;
                final String categoryName = c.getCategory();

                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 8, 0, 8);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setTag(categoryName);

                Spinner spinner = new Spinner(requireContext());
                LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 2);
                spinner.setLayoutParams(spinnerParams);
                spinner.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                        android.graphics.Color.parseColor("#2A2A2C")));
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                        requireContext(), R.array.expense_categories, R.layout.spinner_item_white);
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
                spinner.setAdapter(adapter);
                spinner.setSelection(adapter.getPosition(c.getCategory()));

                EditText percent = new EditText(requireContext());
                LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                percentParams.setMarginStart(8);
                percent.setLayoutParams(percentParams);
                percent.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                percent.setText(String.format(Locale.US, "%.0f", c.getPercentage()));
                percent.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                percent.setHintTextColor(android.graphics.Color.parseColor("#888888"));
                percent.setHint("%");

                percent.setOnFocusChangeListener((focusView, hasFocus) -> {
                    if (!hasFocus) editedIndices.add(index);
                });

                Button btnRemove = new Button(requireContext());
                btnRemove.setText("✕");
                btnRemove.setTextColor(android.graphics.Color.parseColor("#FF5555"));
                btnRemove.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                btnRemove.setLayoutParams(removeParams);
                btnRemove.setOnClickListener(vr -> {
                    int visibleCount = 0;
                    for (int j = 0; j < editContainer.getChildCount(); j++) {
                        if (editContainer.getChildAt(j).getVisibility() == View.VISIBLE) {
                            visibleCount++;
                        }
                    }
                    if (visibleCount <= 1) {
                        Toast.makeText(requireContext(),
                                "You must have at least one category",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    row.setVisibility(View.GONE);
                    removedCategories.add(categoryName);
                    editedIndices.remove(index);
                });

                row.addView(spinner);
                row.addView(percent);
                row.addView(btnRemove);
                editContainer.addView(row);
            }

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(editView)
                    .create();

            btnSave.setOnClickListener(v2 -> {
                View focused = dialog.getCurrentFocus();
                if (focused != null) focused.clearFocus();

                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser == null) return;
                String uid = firebaseUser.getUid();

                int childCount = editContainer.getChildCount();
                List<Category> updated = new ArrayList<>();
                List<Integer> activeIndices = new ArrayList<>();

                double deletedBalance = 0;
                for (Category c : existing) {
                    if (removedCategories.contains(c.getCategory())) {
                        deletedBalance += c.getBalance();
                    }
                }

                for (int i = 0; i < childCount; i++) {
                    LinearLayout row = (LinearLayout) editContainer.getChildAt(i);
                    if (row.getVisibility() == View.GONE) continue;

                    Spinner spinner = (Spinner) row.getChildAt(0);
                    EditText percent = (EditText) row.getChildAt(1);

                    String category = spinner.getSelectedItem().toString();
                    String valueStr = percent.getText().toString().trim();
                    if (valueStr.isEmpty()) {
                        Toast.makeText(requireContext(), "Fill all percentages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double value = Double.parseDouble(valueStr);
                    Category updated_c = new Category(category, value);
                    if (i < existing.size()) {
                        updated_c.setBalance(existing.get(i).getBalance());
                        updated_c.setLastAllocation(existing.get(i).getLastAllocation());
                    }
                    updated.add(updated_c);
                    activeIndices.add(i);
                }

                divider.setAllDividers(updated);

                Set<Integer> remappedEdited = new HashSet<>();
                for (int i = 0; i < activeIndices.size(); i++) {
                    if (editedIndices.contains(activeIndices.get(i))) {
                        remappedEdited.add(i);
                    }
                }

                if (remappedEdited.isEmpty() || !removedCategories.isEmpty()) {
                    divider.rebalanceAll();
                } else {
                    List<Category> editedPools = new ArrayList<>();
                    for (int idx : remappedEdited) {
                        if (idx < divider.getAllDividers().size()) {
                            editedPools.add(divider.getAllDividers().get(idx));
                        }
                    }
                    divider.rebalanceExceptMultiple(editedPools);
                }

                if (deletedBalance > 0) {
                    divider.redistributeDeletedBalance(deletedBalance);
                }

                for (String removedName : removedCategories) {
                    db.collection("users")
                            .document(uid)
                            .collection("percDividers")
                            .document(removedName)
                            .delete();
                }

                for (Category c : divider.getAllDividers()) {
                    Map<String, Object> update = new HashMap<>();
                    update.put("percentage", c.getPercentage());
                    update.put("balance", c.getBalance());
                    update.put("lastAllocation", c.getLastAllocation());
                    db.collection("users")
                            .document(uid)
                            .collection("percDividers")
                            .document(c.getCategory())
                            .update(update);
                }

                IncomeActivity.saveCategories(divider);
                renderCategories();
                loadChartData();
                Toast.makeText(requireContext(), "Updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        });

        addBtn.setOnClickListener(v -> {
            View addView = requireActivity().getLayoutInflater().inflate(R.layout.dialog_add_divider, null);

            Spinner spCategory = addView.findViewById(R.id.spCategory);
            EditText etPercent = addView.findViewById(R.id.etPercent);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    requireContext(), R.array.expense_categories, R.layout.spinner_item_white);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spCategory.setAdapter(adapter);

            Button btnAdd = addView.findViewById(R.id.btnAdd);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(addView)
                    .create();

            btnAdd.setOnClickListener(v2 -> {
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser == null) return;
                String uid = firebaseUser.getUid();

                String name = spCategory.getSelectedItem().toString();
                String percentStr = etPercent.getText().toString().trim();

                if (name.isEmpty() || percentStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                double percent;
                try {
                    percent = Double.parseDouble(percentStr);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Invalid percentage", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (percent <= 0 || percent > 100) {
                    Toast.makeText(requireContext(), "Enter 1–100%", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (Category c : divider.getAllDividers()) {
                    if (c.getCategory().equals(name)) {
                        Toast.makeText(requireContext(), "Category already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                divider.addDivider(name, percent);

                for (Category c : divider.getAllDividers()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("percentage", c.getPercentage());
                    data.put("balance", c.getBalance());
                    data.put("lastAllocation", c.getLastAllocation());
                    db.collection("users")
                            .document(uid)
                            .collection("percDividers")
                            .document(c.getCategory())
                            .set(data);
                }

                IncomeActivity.saveCategories(divider);
                renderCategories();
                loadChartData();
                Toast.makeText(requireContext(), "Category Added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        });
    }

    private void loadDividerFromFirebase() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        db.collection("users")
                .document(firebaseUser.getUid())
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

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refreshData();
    }

    public void refreshData() {
        if (divider == null) {
            User u = SessionManager.getInstance().getCurrentUser();
            if (u != null) divider = u.getPercDivider();
        }
        if (divider != null && categoryContainer != null) {
            loadDividerFromFirebase();
        }
    }

    private void renderCategories() {
        if (categoryContainer == null) return;
        categoryContainer.removeAllViews();
        for (Category c : divider.getAllDividers()) {
            TextView tv = new TextView(requireContext());
            tv.setText(c.getCategory() + " - "
                    + String.format("%.0f%%", c.getPercentage())
                    + " | Balance: €" + String.format("%.2f", c.getBalance())
                    + " | Last: €" + String.format("%.2f", c.getLastAllocation()));
            tv.setTextSize(16);
            tv.setTextColor(android.graphics.Color.parseColor("#DDDDDD"));
            tv.setPadding(8, 8, 8, 8);
            categoryContainer.addView(tv);
        }
    }

    private void setupWebView(WebView wv) {
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient());
        wv.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    }

    private void loadChartData() {
        if (tvMonthLabel == null) return;

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
                .addOnFailureListener(e -> {
                    if (isAdded() && tvMonthLabel != null)
                        Toast.makeText(requireContext(),
                                "Failed to load chart data: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                });
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