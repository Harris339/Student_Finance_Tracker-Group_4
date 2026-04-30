package com.example.studentfinancetracker_wk4;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionsFragment extends Fragment {

    private LinearLayout transactionContainer;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        transactionContainer = view.findViewById(R.id.transactionContainer);
        BalanceViewModel viewModel =
                new ViewModelProvider(requireActivity()).get(BalanceViewModel.class);

        view.findViewById(R.id.btnAddIncome).setOnClickListener(v -> showTransactionDialog(true));
        view.findViewById(R.id.btnAddExpense).setOnClickListener(v -> showTransactionDialog(false));
        view.findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());

        loadTransactions(null, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions(null, null);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) loadTransactions(null, null);
    }

    private void showTransactionDialog(boolean isIncome) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        layout.setPadding(pad, pad, pad, pad);

        EditText etAmount = new EditText(requireContext());
        etAmount.setHint("Amount (€)");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount, matchWidth(LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText etCategory = new EditText(requireContext());
        etCategory.setHint("Category (optional)");
        LinearLayout.LayoutParams categoryParams = matchWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        categoryParams.topMargin = dp(8);
        layout.addView(etCategory, categoryParams);

        Button btnConfirm = new Button(requireContext());
        btnConfirm.setText(isIncome ? "Add Income" : "Add Expense");
        btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(isIncome ? "#2E7D5E" : "#8B3A2A")));
        btnConfirm.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = matchWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
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
                    Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            String category = etCategory.getText().toString().trim();
            if (category.isEmpty()) category = isIncome ? "General" : "General";
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
            loadTransactions(null, null);
        });

        dialog.show();
    }

    private void showFilterDialog() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;
        String userId = firebaseUser.getUid();

        View dialogView = requireActivity().getLayoutInflater()
                .inflate(R.layout.filter_transaction_search, null);

        Spinner spinnerCat = dialogView.findViewById(R.id.spCategory);
        Spinner spinnerType = dialogView.findViewById(R.id.spType);
        View btnExit = dialogView.findViewById(R.id.btnExit);
        Button setBtn = dialogView.findViewById(R.id.setBtn);

        List<String> categories = new ArrayList<>();
        List<String> types = new ArrayList<>();

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_white, categories);
        catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerCat.setAdapter(catAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_white, types);
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerType.setAdapter(typeAdapter);

        spinnerCat.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));
        spinnerType.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

        db.collection("users").document(userId).collection("transactions")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    categories.clear();
                    types.clear();
                    categories.add("Any");
                    types.add("Any");
                    for (DocumentSnapshot doc : snapshots) {
                        String cat = doc.getString("category");
                        String type = doc.getString("type");
                        if (cat != null && !categories.contains(cat)) categories.add(cat);
                        if (type != null && !types.contains(type)) types.add(type);
                    }
                    catAdapter.notifyDataSetChanged();
                    typeAdapter.notifyDataSetChanged();
                });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        dialog.show();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        setBtn.setOnClickListener(v -> {
            String catFilter = spinnerCat.getSelectedItem().toString();
            String typeFilter = spinnerType.getSelectedItem().toString();
            loadTransactions(
                    catFilter.equals("Any") ? null : catFilter,
                    typeFilter.equals("Any") ? null : typeFilter);
            dialog.dismiss();
        });

        btnExit.setOnClickListener(v -> dialog.dismiss());
    }

    private void loadTransactions(@Nullable String categoryFilter, @Nullable String typeFilter) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        Query query = db.collection("users")
                .document(firebaseUser.getUid())
                .collection("transactions");

        if (categoryFilter != null) query = query.whereEqualTo("category", categoryFilter);
        if (typeFilter != null)     query = query.whereEqualTo("type", typeFilter);

        query.get().addOnSuccessListener(snap -> {
            if (!isAdded() || transactionContainer == null) return;
            transactionContainer.removeAllViews();

            if (snap.isEmpty()) {
                TextView empty = new TextView(requireContext());
                empty.setText("No transactions found.");
                empty.setTextColor(Color.parseColor("#BBFFFFFF"));
                empty.setTextSize(14);
                transactionContainer.addView(empty);
                return;
            }

            for (QueryDocumentSnapshot doc : snap) {
                String type     = doc.getString("type");
                String category = doc.getString("category");
                Double amount   = doc.getDouble("amount");
                Date createdAt  = doc.getDate("createdAt");
                String date     = createdAt != null ? dateFmt.format(createdAt) : "N/A";

                boolean isIncome = "Income".equalsIgnoreCase(type);
                String safeType     = type != null ? type : "N/A";
                String safeCategory = category != null ? category : "General";
                String amountStr    = "€" + (amount != null
                        ? String.format(Locale.getDefault(), "%.2f", amount) : "0.00");

                View card = requireActivity().getLayoutInflater()
                        .inflate(R.layout.transaction_item, transactionContainer, false);

                card.findViewById(R.id.accentStripe)
                        .setBackgroundColor(Color.parseColor(isIncome ? "#4CAF50" : "#F44336"));

                ((TextView) card.findViewById(R.id.tvCategory)).setText(safeCategory);
                ((TextView) card.findViewById(R.id.tvType)).setText(safeType);

                TextView tvAmount = card.findViewById(R.id.tvAmount);
                tvAmount.setText(amountStr);
                tvAmount.setTextColor(Color.parseColor(isIncome ? "#4CAF50" : "#F44336"));

                ((TextView) card.findViewById(R.id.tvDate)).setText(date);

                transactionContainer.addView(card);
            }
        }).addOnFailureListener(e -> {
            if (!isAdded() || transactionContainer == null) return;
            transactionContainer.removeAllViews();
            TextView err = new TextView(requireContext());
            err.setText("Failed to load transactions.");
            err.setTextColor(Color.parseColor("#BBFFFFFF"));
            transactionContainer.addView(err);
        });
    }

    private void saveTransaction(String category, double amount, String type) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> tx = new HashMap<>();
        tx.put("amount", amount);
        tx.put("type", type);
        tx.put("category", category);
        tx.put("createdAt", new Date());

        String uid = user.getUid();
        db.collection("users").document(uid).collection("transactions").add(tx);
        db.collection("users").document(uid)
                .update("Balance", FieldValue.increment(type.equals("Income") ? amount : -amount));
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private LinearLayout.LayoutParams matchWidth(int height) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
    }
}
