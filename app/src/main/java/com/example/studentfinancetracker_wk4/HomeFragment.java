package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.graphics.Color;
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

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(FirebaseAuth.getInstance().getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot == null || !snapshot.exists()) return;
                    Double balance = snapshot.getDouble("Balance");
                    if (balance == null) balance = 0.0;
                    viewModel.setBalance(balance);
                });

        TextView tvUserEmail = view.findViewById(R.id.tvUserEmail);
        TextView tvBalance = view.findViewById(R.id.balance);
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
        }

        showBalance(tvBalance, viewModel);
        loadPoolsFromFirebase(view);
        loadRecentTransactions(view);

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
                        if (percent == null) percent = 0.0;
                        if (balance == null) balance = 0.0;
                        Category c = new Category(name, percent);
                        c.setBalance(balance);
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
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        layout.setPadding(pad, pad, pad, pad);

        EditText etAmount = new EditText(requireContext());
        etAmount.setHint("Amount (€)");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText etCategory = null;
        Spinner spCategory = null;

        if (isIncome) {
            etCategory = new EditText(requireContext());
            etCategory.setHint("Category (optional)");
            LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            catParams.topMargin = dp(8);
            layout.addView(etCategory, catParams);
        } else {
            User user = SessionManager.getInstance().getCurrentUser();
            List<String> poolNames = new java.util.ArrayList<>();
            if (user != null) {
                for (Category c : user.getPercDivider().getAllDividers()) {
                    poolNames.add(c.getCategory());
                }
            }
            spCategory = new Spinner(requireContext());
            spCategory.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.parseColor("#2A2A2C")));
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                    requireContext(), R.layout.spinner_item_white, poolNames);
            categoryAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spCategory.setAdapter(categoryAdapter);
            LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            spinnerParams.topMargin = dp(8);
            layout.addView(spCategory, spinnerParams);
        }

        Button btnConfirm = new Button(requireContext());
        btnConfirm.setText(isIncome ? "Add Income" : "Add Expense");
        btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(isIncome ? "#2E7D5E" : "#8B3A2A")));
        btnConfirm.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = dp(12);
        layout.addView(btnConfirm, btnParams);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isIncome ? "Add Income" : "Add Expense")
                .setView(layout)
                .create();

        final EditText finalEtCategory = etCategory;
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
                category = finalEtCategory.getText().toString().trim();
                if (category.isEmpty()) category = "General";
                User user = SessionManager.getInstance().getCurrentUser();
                if (user != null) {
                    user.getPercDivider().allocateIncome(amount);
                    IncomeActivity.saveDivider(user.getPercDivider());
                }
            } else {
                if (finalSpCategory == null || finalSpCategory.getSelectedItem() == null) {
                    Toast.makeText(requireContext(),
                            "No categories available. Please set up your income pools first.",
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
        poolsContainer.removeAllViews();

        if (currentUser == null) {
            renderUpcomingBills(view, null);
            return;
        }

        SmartPercentageDivider divider = currentUser.getPercDivider();
        List<Category> pools = divider.getAllDividers();

        renderSavings(view, currentUser);
        renderUpcomingBills(view, currentUser);

        if (pools.isEmpty()) {
            tvNoPools.setVisibility(View.VISIBLE);
            return;
        }

        tvNoPools.setVisibility(View.GONE);
        for (Category c : pools) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView tvName = new TextView(requireContext());
            tvName.setText(c.getCategory());
            tvName.setTextColor(Color.parseColor("#FFFFFF"));
            tvName.setTextSize(15);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView tvBalance = new TextView(requireContext());
            tvBalance.setText(String.format("€%.2f  (%.0f%%)", c.getBalance(), c.getPercentage()));
            tvBalance.setTextColor(Color.parseColor("#4DB6AC"));
            tvBalance.setTextSize(15);

            row.addView(tvName);
            row.addView(tvBalance);
            poolsContainer.addView(row);
        }
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

        java.util.List<FixedExpense> upcoming = new java.util.ArrayList<>();
        for (FixedExpense e : user.getExpenseList().getAllExpenses()) {
            if (e.getLastPayment() != null && e.getDaysUntilNextPayment() <= 7) {
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