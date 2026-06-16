package com.example.studentfinancetracker_wk4;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

public class TransactionsFragment extends Fragment {

    private LinearLayout transactionContainer;
    private static final String TAG = "BALANCE_LISTENER";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ── Real-time listener ──────────────────────────────────────────────────
    // ListenerRegistration lets us cleanly stop the listener when the fragment
    // is destroyed so it does not leak memory or fire after the user leaves.
    private ListenerRegistration balanceListener;

    // Tracks whether a warning dialog is already on screen so we do not stack them.
    private boolean warningShowing = false;

    // ─────────────────────────────────────────────────────────────────────────
    //  LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────────

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
        new ViewModelProvider(requireActivity()).get(BalanceViewModel.class);

        view.findViewById(R.id.btnAddIncome).setOnClickListener(v -> showTransactionDialog(true));
        view.findViewById(R.id.btnAddExpense).setOnClickListener(v -> showTransactionDialog(false));
        view.findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());

        loadTransactions(null, null);

        // Start the permanent real-time balance watcher
        startBalanceListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions(null, null);
        // Re-attach listener if it was detached while off-screen
        if (balanceListener == null) startBalanceListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Detach while off-screen to avoid background popups
        stopBalanceListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopBalanceListener();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadTransactions(null, null);
            if (balanceListener == null) startBalanceListener();
        } else {
            stopBalanceListener();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REAL-TIME BALANCE LISTENER
    //
    //  addSnapshotListener registers a permanent callback with Firestore.
    //  Every time the user document changes (including the Balance field),
    //  Firestore pushes the new snapshot to this callback automatically, and then there is no polling, no manual threading required.
    //
    //  The moment saveTransaction() writes a new negative Balance to Firestore,
    //  this listener fires and shows the warning dialog.
    // ─────────────────────────────────────────────────────────────────────────

    private void startBalanceListener() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        Log.d(TAG, "▶️ Balance listener attached");

        balanceListener = db.collection("users")
                .document(firebaseUser.getUid())
                .addSnapshotListener((snapshot, e) -> {

                    if (e != null) {
                        Log.e(TAG, "❌ Listener error: " + e.getMessage());
                        return;
                    }

                    if (snapshot == null || !snapshot.exists() || !isAdded()) return;

                    Double balance = snapshot.getDouble("Balance");
                    if (balance == null) return;

                    Log.d(TAG, "🔔 Balance update received: €" + balance);

                    // Only show if negative AND a dialog is not already visible
                    if (balance < 0 && !warningShowing) {
                        showNegativeBalanceWarning(balance);
                    }
                });
    }

    private void stopBalanceListener() {
        if (balanceListener != null) {
            balanceListener.remove();
            balanceListener = null;
            Log.d(TAG, "⏹️ Balance listener removed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NEGATIVE BALANCE WARNING DIALOG
    //  Triggered automatically by the listener the instant balance goes negative.
    // ─────────────────────────────────────────────────────────────────────────

    private void showNegativeBalanceWarning(double balance) {
        if (!isAdded() || warningShowing) return;
        warningShowing = true;

        // Toast for immediate attention
        Toast.makeText(requireContext(),
                "⚠️ Your balance is negative!", Toast.LENGTH_LONG).show();

        // Build dialog layout
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, pad);
        layout.setBackgroundColor(Color.parseColor("#1E1E1E"));

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("⚠️  Warning: Negative Balance");
        tvTitle.setTextSize(16f);
        tvTitle.setTextColor(Color.parseColor("#FF6B6B"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        layout.addView(tvTitle);

        View divider = new View(requireContext());
        divider.setBackgroundColor(Color.parseColor("#8B3A2A"));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divParams.topMargin = dp(10);
        divParams.bottomMargin = dp(14);
        layout.addView(divider, divParams);

        TextView tvMessage = new TextView(requireContext());
        tvMessage.setText(String.format(Locale.getDefault(),
                "Your balance has dropped to €%.2f.\n\nPlease review your spending.",
                balance));
        tvMessage.setTextSize(14f);
        tvMessage.setTextColor(Color.parseColor("#CCCCCC"));
        tvMessage.setLineSpacing(0f, 1.4f);
        layout.addView(tvMessage);

        AlertDialog warningDialog = new AlertDialog.Builder(requireContext())
                .setView(layout)
                .setCancelable(false)        // user must actively dismiss it
                .setPositiveButton("OK, got it", (d, which) -> {
                    warningShowing = false;  // allow the dialog to show again next time
                    d.dismiss();
                })
                .create();

        if (warningDialog.getWindow() != null) {
            warningDialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.parseColor("#1E1E1E")));
        }

        warningDialog.show();
        warningDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Color.parseColor("#FF6B6B"));
    }



    private void showTransactionDialog(boolean isIncome) {
        int layoutRes = isIncome ? R.layout.dialog_add_income : R.layout.dialog_quick_expense;
        View dialogView = LayoutInflater.from(requireContext()).inflate(layoutRes, null);

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
                    Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
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
                            "No pools available. Add a pool first.", Toast.LENGTH_LONG).show();
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
            loadTransactions(null, null);
        });

        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FILTER DIALOG
    // ─────────────────────────────────────────────────────────────────────────

    private void showFilterDialog() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;
        String userId = firebaseUser.getUid();

        View dialogView = requireActivity().getLayoutInflater()
                .inflate(R.layout.filter_transaction_search, null);

        Spinner spinnerCat  = dialogView.findViewById(R.id.spCategory);
        Spinner spinnerType = dialogView.findViewById(R.id.spType);
        View    btnExit     = dialogView.findViewById(R.id.btnExit);
        Button  setBtn      = dialogView.findViewById(R.id.setBtn);

        List<String> categories = new ArrayList<>();
        List<String> types      = new ArrayList<>();

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
                        String cat  = doc.getString("category");
                        String type = doc.getString("type");
                        if (cat  != null && !categories.contains(cat))  categories.add(cat);
                        if (type != null && !types.contains(type))       types.add(type);
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
            String catFilter  = spinnerCat.getSelectedItem().toString();
            String typeFilter = spinnerType.getSelectedItem().toString();
            loadTransactions(
                    catFilter.equals("Any")  ? null : catFilter,
                    typeFilter.equals("Any") ? null : typeFilter);
            dialog.dismiss();
        });

        btnExit.setOnClickListener(v -> dialog.dismiss());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOAD TRANSACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private void loadTransactions(@Nullable String categoryFilter, @Nullable String typeFilter) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        Query query = db.collection("users")
                .document(firebaseUser.getUid())
                .collection("transactions");

        if (categoryFilter != null) query = query.whereEqualTo("category", categoryFilter);
        if (typeFilter     != null) query = query.whereEqualTo("type",     typeFilter);

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
                String type      = doc.getString("type");
                String category  = doc.getString("category");
                Double amount    = doc.getDouble("amount");
                Date   createdAt = doc.getDate("createdAt");
                String date      = createdAt != null ? dateFmt.format(createdAt) : "N/A";

                String safeType = type != null ? type : "N/A";
                boolean isIncome = "Income".equalsIgnoreCase(type);
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

    // ─────────────────────────────────────────────────────────────────────────
    //  SAVE TRANSACTION
    //  Writes to Firestore — this triggers the snapshot listener automatically.
    // ─────────────────────────────────────────────────────────────────────────

    private void saveTransaction(String category, double amount, String type) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No firebase user found");
            return;
        }

        Map<String, Object> tx = new HashMap<>();
        tx.put("amount",    amount);
        tx.put("type",      type);
        tx.put("category",  category);
        tx.put("createdAt", new Date());

        String uid   = user.getUid();
        double delta = type.equals("Income") ? amount : -amount;

        db.collection("users").document(uid).collection("transactions").add(tx);

        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) return;

                    Double currentBalance = snapshot.getDouble("Balance");
                    if (currentBalance == null) currentBalance = 0.0;
                    double newBalance = currentBalance + delta;

                    // Writing this update is what triggers startBalanceListener()
                    db.collection("users").document(uid).update("Balance", newBalance);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        boolean hasPermission = requireContext().checkSelfPermission(
                                android.Manifest.permission.POST_NOTIFICATIONS)
                                == PackageManager.PERMISSION_GRANTED;
                        Log.d(TAG, "Notification permission granted: " + hasPermission);
                    }

                    if (newBalance < 0 && type.equals("Expense") && isAdded()) {
                        NotificationHelper.sendOverspendNotification(requireContext(), newBalance);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to fetch user document: " + e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private LinearLayout.LayoutParams matchWidth(int height) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
    }
}
