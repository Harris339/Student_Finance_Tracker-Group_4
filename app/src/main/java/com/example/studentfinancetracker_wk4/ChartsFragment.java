package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartsFragment extends Fragment {

    private Button previousBtn, nextBtn;
    private TextView tvMonthLabel;
    private WebView barChartView, pieChartView, lineChartView;

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

        previousBtn  = view.findViewById(R.id.previousBtn);
        nextBtn      = view.findViewById(R.id.nextBtn);
        tvMonthLabel = view.findViewById(R.id.tvMonthLabel);
        barChartView = view.findViewById(R.id.barChartView);
        pieChartView = view.findViewById(R.id.pieChartView);
        lineChartView = view.findViewById(R.id.lineChartView);

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
        if (divider != null) {
            loadDividerFromFirebase();
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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