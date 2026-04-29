package com.example.studentfinancetracker_wk4;

import android.content.res.ColorStateList;
import android.graphics.Color;
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

import java.util.ArrayList;
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

        FirebaseFirestore.getInstance()
                .collection("users").document(firebaseUser.getUid())
                .collection("data").document("categories")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    List<Map<String, Object>> items =
                            (List<Map<String, Object>>) doc.get("items");
                    if (items == null) return;

                    User user = SessionManager.getInstance().getCurrentUser();
                    if (user == null) return;

                    List<Category> categories = new ArrayList<>();
                    for (Map<String, Object> item : items) {
                        String name = (String) item.get("name");
                        double percentage  = ((Number) item.get("percentage")).doubleValue();
                        double balance     = ((Number) item.get("balance")).doubleValue();
                        double lastAlloc   = ((Number) item.get("lastAllocation")).doubleValue();
                        Category c = new Category(name, percentage);
                        c.setBalance(balance);
                        c.setLastAllocation(lastAlloc);
                        categories.add(c);
                    }

                    user.getPercDivider().setAllDividers(categories);
                    homeFragment.refreshPools();
                    chartsFragment.refreshData();
                });
    }
}
