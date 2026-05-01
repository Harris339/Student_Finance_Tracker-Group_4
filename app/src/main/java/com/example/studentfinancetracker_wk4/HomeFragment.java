package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private ListenerRegistration balanceListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        User currentUser = SessionManager.getInstance().getCurrentUser();

        BalanceViewModel viewModel =
                new ViewModelProvider(requireActivity()).get(BalanceViewModel.class);

        FirebaseUser setupUser = FirebaseAuth.getInstance().getCurrentUser();
        if (setupUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(setupUser.getUid())
                    .addSnapshotListener((snapshot, error) -> {
                        if (snapshot == null || !snapshot.exists()) return;
                        Double balance = snapshot.getDouble("Balance");
                        if (balance == null) balance = 0.0;
                        viewModel.setBalance(balance);
                    });
        }

        TextView tvUserEmail = view.findViewById(R.id.tvUserEmail);
        TextView tvBalance = view.findViewById(R.id.balance);
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
        }

        showBalance(tvBalance, viewModel);
        loadPoolsFromFirebase(view);
        loadRecentTransactions(view);

        Button btnEditAll = view.findViewById(R.id.btnEditAll);
        Button btnAddCategory = view.findViewById(R.id.btnAddCategory);
        if (btnEditAll != null) btnEditAll.setOnClickListener(v -> showEditDialog(view));
        if (btnAddCategory != null) btnAddCategory.setOnClickListener(v -> showAddDialog(view));

        Button btnQuickIncome = view.findViewById(R.id.btnQuickIncome);
        Button btnQuickExpense = view.findViewById(R.id.btnQuickExpense);
        ImageButton btnLogout = view.findViewById(R.id.btnLogout);

        btnQuickIncome.setOnClickListener(v -> showTransactionDialog(true));
        btnQuickExpense.setOnClickListener(v -> showTransactionDialog(false));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SessionManager.getInstance().setCurrentUser(null);
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPools();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (balanceListener != null) {
            balanceListener.remove();
            balanceListener = null;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refreshPools();
    }

    public void refreshPools() {
        View view = getView();
        if (view != null) {
            loadPoolsFromFirebase(view);
        }
    }

    private void loadPoolsFromFirebase(View view) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.getUid())
                .collection("percDividers")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Category> list = new java.util.ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = doc.getId();
                        Double percent = doc.getDouble("percentage");
                        Double balance = doc.getDouble("balance");
                        Double last = doc.getDouble("lastAllocation");
                        if (percent == null) percent = 0.0;
                        if (balance == null) balance = 0.0;
                        if (last == null) last = 0.0;
                        Category c = new Category(name, percent);
                        c.setBalance(balance);
                        c.setLastAllocation(last);
                        list.add(c);
                    }
                    User user = SessionManager.getInstance().getCurrentUser();
                    if (user != null) {
                        user.getPercDivider().setAllDividers(list);
                    }
                    renderPools(view, user);
                });
    }

    private void showTransactionDialog(boolean isIncome) {
        int layout = isIncome ? R.layout.dialog_add_income : R.layout.dialog_quick_expense;
        View dialogView = LayoutInflater.from(requireContext()).inflate(layout, null);

        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etCategory = isIncome ? dialogView.findViewById(R.id.etCategory) : null;
        Spinner spCategory = null;

        if (!isIncome) {
            spCategory = dialogView.findViewById(R.id.spCategory);
            User user = SessionManager.getInstance().getCurrentUser();
            List<String> poolNames = new ArrayList<>();
            if (user != null) {
                for (Category c : user.getPercDivider().getAllDividers()) {
                    poolNames.add(c.getCategory());
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(), R.layout.spinner_item_white, poolNames);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spCategory.setAdapter(adapter);
        }

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        final Spinner finalSpCategory = spCategory;

        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be > 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            String category;
            String type = isIncome ? "Income" : "Expense";

            if (isIncome) {
                category = etCategory != null ? etCategory.getText().toString().trim() : "";
                if (category.isEmpty()) category = "General";
                User user = SessionManager.getInstance().getCurrentUser();
                if (user != null) {
                    user.getPercDivider().allocateIncome(amount);
                    IncomeActivity.saveDivider(user.getPercDivider());
                }
            } else {
                if (finalSpCategory == null || finalSpCategory.getSelectedItem() == null) {
                    Toast.makeText(requireContext(),
                            "No pools available. Add a pool first.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                category = finalSpCategory.getSelectedItem().toString();
                User user = SessionManager.getInstance().getCurrentUser();
                if (user != null) {
                    user.getPercDivider().deductExpense(category, amount);
                    IncomeActivity.saveDivider(user.getPercDivider());
                }
            }

            saveTransaction(category, amount, type);
            Toast.makeText(requireContext(), type + " added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            refreshPools();
            View root = getView();
            if (root != null) loadRecentTransactions(root);
        });

        dialog.show();
    }

    private void saveTransaction(String category, double amount, String type) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        Map<String, Object> tx = new HashMap<>();
        tx.put("amount", amount);
        tx.put("type", type);
        tx.put("category", category);
        tx.put("createdAt", new Date());

        String uid = firebaseUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).collection("transactions").add(tx);
        db.collection("users").document(uid)
                .update("Balance", FieldValue.increment(type.equals("Income") ? amount : -amount));
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void renderPools(View view, User currentUser) {
        LinearLayout poolsContainer = view.findViewById(R.id.poolsContainer);
        TextView tvNoPools = view.findViewById(R.id.tvNoPools);
        if (poolsContainer == null) return;
        poolsContainer.removeAllViews();

        renderSavings(view, currentUser);
        renderUpcomingBills(view, currentUser);

        if (currentUser == null) return;

        SmartPercentageDivider divider = currentUser.getPercDivider();
        List<Category> pools = divider.getAllDividers();

        if (pools.isEmpty()) {
            if (tvNoPools != null) tvNoPools.setVisibility(View.VISIBLE);
            return;
        }
        if (tvNoPools != null) tvNoPools.setVisibility(View.GONE);

        for (int idx = 0; idx < pools.size(); idx++) {
            Category c = pools.get(idx);

            LinearLayout outer = new LinearLayout(requireContext());
            outer.setOrientation(LinearLayout.VERTICAL);
            outer.setPadding(0, dp(8), 0, dp(8));

            // Name + % pill row
            LinearLayout topRow = new LinearLayout(requireContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(requireContext());
            tvName.setText(c.getCategory());
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(15);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvPill = new TextView(requireContext());
            tvPill.setText(String.format(Locale.US, "%.0f%%", c.getPercentage()));
            tvPill.setTextColor(Color.WHITE);
            tvPill.setTextSize(12);
            tvPill.setPadding(dp(8), dp(2), dp(8), dp(2));
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setColor(Color.parseColor("#333336"));
            pillBg.setCornerRadius(dp(12));
            tvPill.setBackground(pillBg);

            topRow.addView(tvName);
            topRow.addView(tvPill);

            // Balance
            TextView tvBal = new TextView(requireContext());
            tvBal.setText(String.format(Locale.US, "€%.2f", c.getBalance()));
            tvBal.setTextColor(Color.parseColor("#4DB6AC"));
            tvBal.setTextSize(14);
            tvBal.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams balParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            balParams.topMargin = dp(4);
            tvBal.setLayoutParams(balParams);

            // Progress bar
            ProgressBar pb = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress((int) c.getPercentage());
            pb.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#534AB7")));
            pb.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#333336")));
            LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(4));
            pbParams.topMargin = dp(6);
            pb.setLayoutParams(pbParams);

            // Last allocated subtitle
            TextView tvLast = new TextView(requireContext());
            tvLast.setText(String.format(Locale.US, "Last allocated: €%.2f", c.getLastAllocation()));
            tvLast.setTextColor(Color.parseColor("#777777"));
            tvLast.setTextSize(12);
            LinearLayout.LayoutParams lastParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lastParams.topMargin = dp(2);
            tvLast.setLayoutParams(lastParams);

            outer.addView(topRow);
            outer.addView(tvBal);
            outer.addView(pb);
            outer.addView(tvLast);
            poolsContainer.addView(outer);

            if (idx < pools.size() - 1) {
                View div = new View(requireContext());
                div.setBackgroundColor(Color.parseColor("#2A2A2A"));
                div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                poolsContainer.addView(div);
            }
        }
    }

    private void showEditDialog(View rootView) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        SmartPercentageDivider divider = user.getPercDivider();

        View editView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_divider, null);
        LinearLayout editContainer = editView.findViewById(R.id.editContainer);
        Button btnSave = editView.findViewById(R.id.btnSave);

        List<Category> removed = new ArrayList<>();

        for (Category c : new ArrayList<>(divider.getAllDividers())) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundColor(Color.parseColor("#1C1C1E"));
            row.setPadding(dp(12), dp(10), dp(4), dp(10));
            row.setTag(c);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = dp(4);
            row.setLayoutParams(rowParams);

            TextView tvCat = new TextView(requireContext());
            tvCat.setText(c.getCategory());
            tvCat.setTextColor(Color.WHITE);
            tvCat.setTextSize(14);
            tvCat.setTypeface(null, Typeface.BOLD);
            tvCat.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

            // % input with dark background
            LinearLayout pctWrapper = new LinearLayout(requireContext());
            pctWrapper.setOrientation(LinearLayout.HORIZONTAL);
            pctWrapper.setGravity(android.view.Gravity.CENTER_VERTICAL);
            pctWrapper.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            EditText etPct = new EditText(requireContext());
            etPct.setText(String.format(Locale.US, "%.0f", c.getPercentage()));
            etPct.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etPct.setBackground(null);
            etPct.setTextColor(Color.parseColor("#534AB7"));
            etPct.setTextSize(15);
            etPct.setTypeface(null, Typeface.BOLD);
            etPct.setGravity(android.view.Gravity.END);
            etPct.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvPct = new TextView(requireContext());
            tvPct.setText("%");
            tvPct.setTextColor(Color.parseColor("#534AB7"));
            tvPct.setTextSize(14);
            tvPct.setPadding(dp(2), 0, 0, 0);

            pctWrapper.addView(etPct);
            pctWrapper.addView(tvPct);

            Button btnRemove = new Button(requireContext());
            btnRemove.setText("×");
            btnRemove.setTextColor(Color.parseColor("#FF6B6B"));
            btnRemove.setTextSize(16);
            btnRemove.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnRemove.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));

            btnRemove.setOnClickListener(v -> {
                removed.add((Category) row.getTag());
                editContainer.removeView(row);
            });

            row.addView(tvCat);
            row.addView(pctWrapper);
            row.addView(btnRemove);
            editContainer.addView(row);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(editView)
                .create();

        btnSave.setOnClickListener(v -> {
            int count = editContainer.getChildCount();
            List<Category> updated = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                LinearLayout row = (LinearLayout) editContainer.getChildAt(i);
                Category original = (Category) row.getTag();
                LinearLayout pctWrapper = (LinearLayout) row.getChildAt(1);
                EditText etPct = (EditText) pctWrapper.getChildAt(0);

                String pctStr = etPct.getText().toString().trim();
                if (pctStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Fill all percentages", Toast.LENGTH_SHORT).show();
                    return;
                }
                double pct;
                try { pct = Double.parseDouble(pctStr); } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid percentage", Toast.LENGTH_SHORT).show();
                    return;
                }
                original.setPercentage(pct);
                updated.add(original);
            }

            divider.setAllDividers(updated);
            divider.rebalanceAll();

            double deletedBalance = 0;
            for (Category del : removed) deletedBalance += del.getBalance();
            if (deletedBalance > 0) divider.redistributeDeletedBalance(deletedBalance);

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                for (Category del : removed) {
                    db.collection("users").document(firebaseUser.getUid())
                            .collection("percDividers").document(del.getCategory()).delete();
                }
            }

            IncomeActivity.saveDivider(divider);
            dialog.dismiss();
            renderPools(rootView, user);
        });

        dialog.show();
    }

    private void showAddDialog(View rootView) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        SmartPercentageDivider divider = user.getPercDivider();

        View addView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_divider, null);
        Spinner spCategory = addView.findViewById(R.id.spCategory);
        EditText etPercent = addView.findViewById(R.id.etPercent);
        Button btnAdd = addView.findViewById(R.id.btnAdd);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.expense_categories, R.layout.spinner_item_white);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spCategory.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(addView)
                .create();

        btnAdd.setOnClickListener(v -> {
            String name = spCategory.getSelectedItem().toString();
            String pctStr = etPercent.getText().toString().trim();

            if (pctStr.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a percentage", Toast.LENGTH_SHORT).show();
                return;
            }
            double pct;
            try { pct = Double.parseDouble(pctStr); } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid percentage", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pct <= 0 || pct > 100) {
                Toast.makeText(requireContext(), "Enter 1–100%", Toast.LENGTH_SHORT).show();
                return;
            }
            for (Category c : divider.getAllDividers()) {
                if (c.getCategory().equals(name)) {
                    Toast.makeText(requireContext(), "Category already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser == null) return;

            Map<String, Object> data = new HashMap<>();
            data.put("percentage", pct);
            data.put("balance", 0.0);
            data.put("lastAllocation", 0.0);
            FirebaseFirestore.getInstance()
                    .collection("users").document(firebaseUser.getUid())
                    .collection("percDividers").document(name)
                    .set(data);

            divider.addDivider(name, pct);
            IncomeActivity.saveDivider(divider);
            dialog.dismiss();
            renderPools(rootView, user);
        });

        dialog.show();
    }

    private void renderSavings(View view, User user) {
        LinearLayout container = view.findViewById(R.id.savingsGoalsContainer);
        TextView tvNoSavings = view.findViewById(R.id.tvNoSavings);
        if (container == null || tvNoSavings == null) return;
        container.removeAllViews();

        if (user == null || user.getSavingsList().getAllGoals().isEmpty()) {
            tvNoSavings.setVisibility(View.VISIBLE);
            return;
        }

        tvNoSavings.setVisibility(View.GONE);
        for (SavingsGoal goal : user.getSavingsList().getAllGoals()) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(6), 0, dp(10));

            LinearLayout titleRow = new LinearLayout(requireContext());
            titleRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvName = new TextView(requireContext());
            tvName.setText(goal.getTitle());
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(15);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvAmount = new TextView(requireContext());
            tvAmount.setText(String.format(Locale.getDefault(),
                    "€%.0f / €%.0f (%d%%)", goal.getCurrentAmount(), goal.getTargetAmount(), goal.getProgressPercent()));
            tvAmount.setTextColor(goal.isCompleted() ? Color.parseColor("#4DB6AC") : Color.parseColor("#BBFFFFFF"));
            tvAmount.setTextSize(12);

            titleRow.addView(tvName);
            titleRow.addView(tvAmount);

            ProgressBar pb = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress(goal.getProgressPercent());
            pb.setProgressTintList(android.content.res.ColorStateList.valueOf(
                    goal.isCompleted() ? Color.parseColor("#4DB6AC") : Color.parseColor("#534AB7")));
            LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
            pbParams.topMargin = dp(6);
            pb.setLayoutParams(pbParams);

            row.addView(titleRow);
            row.addView(pb);
            container.addView(row);
        }
    }

    private void renderUpcomingBills(View view, User user) {
        LinearLayout container = view.findViewById(R.id.upcomingBillsContainer);
        TextView tvNoBills = view.findViewById(R.id.tvNoBills);
        if (container == null || tvNoBills == null) return;
        container.removeAllViews();

        if (user == null || user.getExpenseList().getAllExpenses().isEmpty()) {
            tvNoBills.setVisibility(View.VISIBLE);
            return;
        }

        java.util.Map<String,Integer> freqDays = new java.util.HashMap<>();
        freqDays.put("Daily", 1);
        freqDays.put("Weekly", 7);
        freqDays.put("Monthly", 30);
        freqDays.put("Yearly", 365);

        java.util.List<FixedExpense> upcoming = new java.util.ArrayList<>();
        for (FixedExpense e : user.getExpenseList().getAllExpenses()) {
            int cycleDays = freqDays.getOrDefault(e.getFrequency(), 30);
            int threshold = Math.min(7, cycleDays - 1);
            if (e.getLastPayment() != null && e.getDaysUntilNextPayment() <= threshold) {
                upcoming.add(e);
            }
        }
        upcoming.sort((a, b) -> Integer.compare(a.getDaysUntilNextPayment(), b.getDaysUntilNextPayment()));

        if (upcoming.isEmpty()) {
            tvNoBills.setVisibility(View.VISIBLE);
            return;
        }
        tvNoBills.setVisibility(View.GONE);

        for (FixedExpense expense : upcoming) {
            int days = expense.getDaysUntilNextPayment();

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 10, 0, 10);

            LinearLayout info = new LinearLayout(requireContext());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(requireContext());
            tvName.setText(expense.getTitle());
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(15);

            TextView tvDue = new TextView(requireContext());
            String dueLabel = days == 0 ? "Due today" : "Due in " + days + " day" + (days == 1 ? "" : "s");
            tvDue.setText(dueLabel + " · " + String.format(java.util.Locale.getDefault(), "€%.2f", expense.getAmount()));
            tvDue.setTextColor(days == 0 ? Color.parseColor("#FF6B6B") : Color.parseColor("#BBFFFFFF"));
            tvDue.setTextSize(12);

            info.addView(tvName);
            info.addView(tvDue);

            Button btnPay = new Button(requireContext());
            btnPay.setText("Pay");
            btnPay.setTextSize(13);
            btnPay.setTextColor(Color.WHITE);
            btnPay.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D5E")));

            btnPay.setOnClickListener(v -> {
                SmartPercentageDivider divider = user.getPercDivider();
                if (!divider.tryDeductExpense(expense.getCategory(), expense.getAmount())) {
                    Toast.makeText(requireContext(),
                            "Insufficient funds in " + expense.getCategory() + " pool",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                expense.addPayment(expense.getAmount());
                expense.setLastPayment(new java.util.Date());
                IncomeActivity.saveCategories(divider);
                IncomeActivity.saveDivider(divider);
                TrackersFragment.saveTrackers(user);
                saveTransaction(expense.getCategory(), expense.getAmount(), "Expense");
                Toast.makeText(requireContext(), expense.getTitle() + " paid", Toast.LENGTH_SHORT).show();
                renderUpcomingBills(view, user);
                refreshPools();
            });

            row.addView(info);
            row.addView(btnPay);
            container.addView(row);

            View divider = new View(requireContext());
            divider.setBackgroundColor(Color.parseColor("#2A2A2A"));
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            container.addView(divider);
        }
    }

    private void loadRecentTransactions(View view) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        TextView tvLastIncome = view.findViewById(R.id.tvLastIncome);
        TextView tvLastExpense = view.findViewById(R.id.tvLastExpense);
        if (tvLastIncome == null || tvLastExpense == null) return;

        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM", Locale.getDefault());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.getUid())
                .collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String lastIncome = null, lastExpense = null;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        String type = doc.getString("type");
                        Double amount = doc.getDouble("amount");
                        String category = doc.getString("category");
                        if (amount == null) continue;

                        Object raw = doc.get("createdAt");
                        String dateStr = "";
                        if (raw instanceof Timestamp) {
                            dateStr = " · " + fmt.format(((Timestamp) raw).toDate());
                        }

                        String label = String.format(Locale.getDefault(), "€%.2f%s%s",
                                amount,
                                category != null && !category.isEmpty() ? " · " + category : "",
                                dateStr);

                        if ("Income".equalsIgnoreCase(type) && lastIncome == null) lastIncome = label;
                        if ("Expense".equalsIgnoreCase(type) && lastExpense == null) lastExpense = label;
                        if (lastIncome != null && lastExpense != null) break;
                    }
                    tvLastIncome.setText(lastIncome != null ? lastIncome : "—");
                    tvLastExpense.setText(lastExpense != null ? lastExpense : "—");
                });
    }

    private void showBalance(TextView tvBalance, BalanceViewModel viewModel) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        balanceListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.getUid())
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e("FIRESTORE", "Listen failed", error);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Double balance = documentSnapshot.getDouble("Balance");
                        if (balance == null) balance = 0.0;
                        String newValue = String.format("Current Balance: €%.2f", balance);
                        viewModel.setBalance(balance);
                        tvBalance.animate()
                                .alpha(0f)
                                .setDuration(100)
                                .withEndAction(() -> {
                                    tvBalance.setText(newValue);
                                    tvBalance.animate().alpha(1f).setDuration(100);
                                });
                    }
                });
    }
}