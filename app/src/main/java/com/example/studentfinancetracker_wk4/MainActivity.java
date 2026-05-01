package com.example.studentfinancetracker_wk4;

import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Fragment activeFragment;
    private final HomeFragment homeFragment = new HomeFragment();
    private final TransactionsFragment transactionsFragment = new TransactionsFragment();
    private final TrackersFragment trackersFragment = new TrackersFragment();
    private final ChartsFragment chartsFragment = new ChartsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ── Notifications setup ──────────────────────────────────────────
        NotificationHelper.createNotificationChannel(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }
        // ────────────────────────────────────────────────────────────────

        // Push fragment content below the status bar
        View fragmentContainer = findViewById(R.id.fragmentContainer);
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, insets) -> {
            Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBar.top, 0, 0);
            return insets;
        });

        // Push bottom nav above the system navigation bar
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 4, 0, navBar.bottom);
            return insets;
        });

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, homeFragment, "home")
                .add(R.id.fragmentContainer, transactionsFragment, "transactions").hide(transactionsFragment)
                .add(R.id.fragmentContainer, trackersFragment, "trackers").hide(trackersFragment)
                .add(R.id.fragmentContainer, chartsFragment, "charts").hide(chartsFragment)
                .commit();

        activeFragment = homeFragment;

        bottomNav.setItemActiveIndicatorColor(
                ColorStateList.valueOf(Color.parseColor("#4DB6AC")));

        loadUserData();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected;
            int id = item.getItemId();
            if (id == R.id.nav_home) selected = homeFragment;
            else if (id == R.id.nav_transactions) selected = transactionsFragment;
            else if (id == R.id.nav_trackers) selected = trackersFragment;
            else if (id == R.id.nav_charts) selected = chartsFragment;
            else return false;

            getSupportFragmentManager().beginTransaction()
                    .hide(activeFragment)
                    .show(selected)
                    .commit();
            activeFragment = selected;
            return true;
        });
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;
        String uid = firebaseUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("percDividers")
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = SessionManager.getInstance().getCurrentUser();
                    if (user == null) return;

                    List<Category> categories = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        String name    = doc.getId();
                        double pct     = doc.getDouble("percentage")     != null ? doc.getDouble("percentage")     : 0.0;
                        double balance = doc.getDouble("balance")        != null ? doc.getDouble("balance")        : 0.0;
                        double alloc   = doc.getDouble("lastAllocation") != null ? doc.getDouble("lastAllocation") : 0.0;
                        Category c = new Category(name, pct);
                        c.setBalance(balance);
                        c.setLastAllocation(alloc);
                        categories.add(c);
                    }
                    user.getPercDivider().setAllDividers(categories);
                    homeFragment.refreshPools();
                    chartsFragment.refreshData();

                    // Chain tracker load so pools are guaranteed populated first
                    FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .collection("data").document("trackers")
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) return;

                                user.getExpenseList().getAllExpenses().clear();
                                user.getSavingsList().getAllGoals().clear();

                                List<Map<String, Object>> expenses =
                                        (List<Map<String, Object>>) doc.get("expenses");
                                if (expenses != null) {
                                    for (Map<String, Object> e : expenses) {
                                        String cat   = (String) e.get("category");
                                        String title = (String) e.get("title");
                                        double amt   = ((Number) e.get("amount")).doubleValue();
                                        String freq  = (String) e.get("frequency");
                                        String notes = (String) e.get("notes");
                                        double paid  = ((Number) e.get("totalPaid")).doubleValue();
                                        Date lastPay = e.get("lastPayment") instanceof Timestamp
                                                ? ((Timestamp) e.get("lastPayment")).toDate() : null;

                                        FixedExpense expense = new FixedExpense(cat, title, amt, freq, notes);
                                        expense.setTotalPaid(paid);
                                        expense.setLastPayment(lastPay);
                                        List<?> payHist  = (List<?>) e.get("paymentHist");
                                        List<?> payDates = (List<?>) e.get("paymentDates");
                                        if (payHist != null && !payHist.isEmpty()) {
                                            expense.getPaymentHist().clear();
                                            for (Object v : payHist) expense.getPaymentHist().add(((Number) v).doubleValue());
                                            expense.getPaymentDates().clear();
                                            if (payDates != null) {
                                                for (Object d : payDates) expense.getPaymentDates().add(d instanceof Timestamp ? ((Timestamp) d).toDate() : null);
                                            }
                                            expense.setPos(expense.getPaymentHist().size() - 1);
                                        }
                                        user.getExpenseList().addExpense(expense);
                                    }
                                }

                                List<Map<String, Object>> savings =
                                        (List<Map<String, Object>>) doc.get("savings");
                                if (savings != null) {
                                    for (Map<String, Object> s : savings) {
                                        String title    = (String) s.get("title");
                                        String notes    = (String) s.get("notes");
                                        double target   = ((Number) s.get("targetAmount")).doubleValue();
                                        double current  = ((Number) s.get("currentAmount")).doubleValue();
                                        Date targetDate = s.get("targetDate") instanceof Timestamp
                                                ? ((Timestamp) s.get("targetDate")).toDate() : null;

                                        SavingsGoal goal = new SavingsGoal(title, notes, target, targetDate);
                                        goal.setCurrentAmount(current);
                                        List<?> contribHist  = (List<?>) s.get("contributionHist");
                                        List<?> contribDates = (List<?>) s.get("contributionDates");
                                        if (contribHist != null && !contribHist.isEmpty()) {
                                            goal.getContributionHist().clear();
                                            for (Object v : contribHist) goal.getContributionHist().add(((Number) v).doubleValue());
                                            goal.getContributionDates().clear();
                                            if (contribDates != null) {
                                                for (Object d : contribDates) goal.getContributionDates().add(d instanceof Timestamp ? ((Timestamp) d).toDate() : null);
                                            }
                                            goal.setPos(goal.getContributionHist().size() - 1);
                                        }
                                        user.getSavingsList().addGoal(goal);
                                    }
                                }

                                trackersFragment.refreshTrackers();
                            });
                });
    }
}
