package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

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

        TextView tvUserEmail = view.findViewById(R.id.tvUserEmail);
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
        }

        loadPoolsFromFirebase(view);

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

        EditText etCategory = new EditText(requireContext());
        etCategory.setHint("Category (optional)");
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        catParams.topMargin = dp(8);
        layout.addView(etCategory, catParams);

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

            String category = etCategory.getText().toString().trim();
            if (category.isEmpty()) category = "General";
            String type = isIncome ? "Income" : "Expense";

            if (isIncome) {
                User user = SessionManager.getInstance().getCurrentUser();
                if (user != null) {
                    user.getPercDivider().allocateIncome(amount);
                    IncomeActivity.saveCategories(user.getPercDivider());
                }
            }

            saveTransaction(category, amount, type);
            Toast.makeText(requireContext(), type + " added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            refreshPools();
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

        if (currentUser == null) return;

        SmartPercentageDivider divider = currentUser.getPercDivider();
        List<Category> pools = divider.getAllDividers();

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
}
