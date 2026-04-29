package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ChartActivity extends AppCompatActivity {

    Button previousBtn, nextBtn, backBtn;
    TextView tvMonthLabel;
    WebView barChartView, pieChartView, lineChartView;

    // Negative = past months, 0 = current month
    int currentOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        // Explicit casts make the type clear to the compiler, removing warnings
        previousBtn   = (Button)   findViewById(R.id.previousBtn);
        nextBtn       = (Button)   findViewById(R.id.nextBtn);
        backBtn       = (Button)   findViewById(R.id.backBtn);
        tvMonthLabel  = (TextView) findViewById(R.id.tvMonthLabel);
        barChartView  = (WebView)  findViewById(R.id.barChartView);
        pieChartView  = (WebView)  findViewById(R.id.pieChartView);
        lineChartView = (WebView)  findViewById(R.id.lineChartView);

        setupWebView(barChartView);
        setupWebView(pieChartView);
        setupWebView(lineChartView);

        loadChartData();

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
        float[] incomeByWeek = new float[weeks];
        float[] expenseByWeek = new float[weeks];
        float[] incomeByWeekLine = new float[weeks];

        float[] pie = {0f, 0f, 0f, 0f};

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("transactions")
                .whereGreaterThanOrEqualTo("createdAt", monthStart)
                .whereLessThanOrEqualTo("createdAt", monthEnd)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        loadBarChart(barChartView, "'Wk 1'", "[0]", "[0]");
                        loadPieChart(pieChartView, new float[]{0, 0, 0, 0});
                        loadLineChart(lineChartView, "'Wk 1'", "[0]");
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snap) {
                        Date date = doc.getDate("createdAt");
                        Double amt = doc.getDouble("amount");
                        String type = doc.getString("type");
                        String cat = doc.getString("category");
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
                            if (cat != null) {
                                if (cat.contains("Rent")) pie[0] += amt.floatValue();
                                else if (cat.contains("Sub")) pie[1] += amt.floatValue();
                                else if (cat.contains("Util")) pie[2] += amt.floatValue();
                                else pie[3] += amt.floatValue();
                            }
                        }
                    }

                    StringBuilder weekLabels = new StringBuilder();
                    for (int i = 0; i < weeks; i++) {
                        if (i > 0) weekLabels.append(",");
                        weekLabels.append("'Wk ").append(i + 1).append("'");
                    }

                    loadBarChart(barChartView, weekLabels.toString(),
                            toJsArray(incomeByWeek), toJsArray(expenseByWeek));
                    loadPieChart(pieChartView, pie);
                    loadLineChart(lineChartView, weekLabels.toString(),
                            toJsArray(incomeByWeekLine));
                });
    }

    private void loadBarChart(WebView wv, String labels,
                              String incomeData, String expenseData) {
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

    private void loadPieChart(WebView wv, float[] pie) {
        String html = "<!DOCTYPE html><html><head>"
                + "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>"
                + "<style>body{margin:0;background:#1C1C1E;}canvas{width:100%!important;max-height:160px;}</style>"
                + "</head><body>"
                + "<canvas id='c'></canvas><script>"
                + "new Chart(document.getElementById('c'),{"
                + "type:'doughnut',"
                + "data:{labels:['Rent','Subscriptions','Utilities','Other'],"
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