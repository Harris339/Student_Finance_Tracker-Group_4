package com.example.studentfinancetracker_wk4;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrackersActivity extends AppCompatActivity {
    Button addButton;
    ImageButton backbtn;

    User currentUser;
    ExpenseList expenseList;
    SavingsList savingsList;
    SmartPercentageDivider percDivider;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_trackers);

        addButton = findViewById(R.id.addButton);
        backbtn   = findViewById(R.id.btnBack);

        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }
        expenseList = currentUser.getExpenseList();
        savingsList = currentUser.getSavingsList();
        percDivider = currentUser.getPercDivider();

        for (FixedExpense expense : expenseList.getAllExpenses()) {
            if ("Subscriptions".equals(expense.getCategory())) addSubscriptionBox(expense);
            else addExpenseBox(expense);
        }

        for (SavingsGoal goal : savingsList.getAllGoals()) {
            addSavingsGoalBox(goal);
        }

        addButton.setOnClickListener(v -> showAddChoiceDialog());
        backbtn.setOnClickListener(v -> finish());

        setupFilterBar();
    }

    private TextView activeFilter;

    private void setupFilterBar() {
        TextView filterAll           = findViewById(R.id.filterAll);
        TextView filterExpenses      = findViewById(R.id.filterExpenses);
        TextView filterSubscriptions = findViewById(R.id.filterSubscriptions);
        TextView filterSavings       = findViewById(R.id.filterSavings);

        setFilterActive(filterAll, Color.parseColor("#555555"));
        setFilterInactive(filterExpenses);
        setFilterInactive(filterSubscriptions);
        setFilterInactive(filterSavings);
        activeFilter = filterAll;

        filterAll.setOnClickListener(v -> {
            setFilterActive(filterAll, Color.parseColor("#555555"));
            setFilterInactive(filterExpenses);
            setFilterInactive(filterSubscriptions);
            setFilterInactive(filterSavings);
            activeFilter = filterAll;
            applyFilter("all");
        });

        filterExpenses.setOnClickListener(v -> {
            setFilterActive(filterExpenses, Color.parseColor("#FF9800"));
            setFilterInactive(filterAll);
            setFilterInactive(filterSubscriptions);
            setFilterInactive(filterSavings);
            activeFilter = filterExpenses;
            applyFilter("expenses");
        });

        filterSubscriptions.setOnClickListener(v -> {
            setFilterActive(filterSubscriptions, Color.parseColor("#5C6BC0"));
            setFilterInactive(filterAll);
            setFilterInactive(filterExpenses);
            setFilterInactive(filterSavings);
            activeFilter = filterSubscriptions;
            applyFilter("subscriptions");
        });

        filterSavings.setOnClickListener(v -> {
            setFilterActive(filterSavings, Color.parseColor("#4DB6AC"));
            setFilterInactive(filterAll);
            setFilterInactive(filterExpenses);
            setFilterInactive(filterSubscriptions);
            activeFilter = filterSavings;
            applyFilter("savings");
        });
    }

    private void applyFilter(String filter) {
        LinearLayout sectionExpenses      = findViewById(R.id.sectionExpenses);
        LinearLayout sectionSubscriptions = findViewById(R.id.sectionSubscriptions);
        LinearLayout sectionSavings       = findViewById(R.id.sectionSavings);

        boolean showAll  = filter.equals("all");
        sectionExpenses.setVisibility(showAll || filter.equals("expenses") ? View.VISIBLE : View.GONE);
        sectionSubscriptions.setVisibility(showAll || filter.equals("subscriptions") ? View.VISIBLE : View.GONE);
        sectionSavings.setVisibility(showAll || filter.equals("savings") ? View.VISIBLE : View.GONE);
    }

    private void setFilterActive(TextView tv, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(20));
        bg.setColor(color);
        tv.setBackground(bg);
    }

    private void setFilterInactive(TextView tv) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(20));
        bg.setColor(Color.parseColor("#2C2C2C"));
        bg.setStroke(dpToPx(1), Color.parseColor("#555555"));
        tv.setBackground(bg);
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    private void showAddChoiceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_choice, null);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.optionExpense).setOnClickListener(v -> {
            dialog.dismiss();
            showAddExpenseDialog(false);
        });

        dialogView.findViewById(R.id.optionSubscription).setOnClickListener(v -> {
            dialog.dismiss();
            showAddExpenseDialog(true);
        });

        dialogView.findViewById(R.id.optionSavings).setOnClickListener(v -> {
            dialog.dismiss();
            showAddSavingsDialog();
        });
    }

    private void showAddExpenseDialog(boolean isSubscription) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);

        Spinner      spCategory     = dialogView.findViewById(R.id.spCategory);
        Spinner      spFrequency    = dialogView.findViewById(R.id.spFrequency);
        EditText     etTitle        = dialogView.findViewById(R.id.etTitle);
        EditText     etDescription  = dialogView.findViewById(R.id.etDescription);
        EditText     etAmount       = dialogView.findViewById(R.id.etAmount);
        Button       btnSave        = dialogView.findViewById(R.id.btnSaveExpense);
        ImageButton  btnExit        = dialogView.findViewById(R.id.btnExit);
        LinearLayout subscriptionBox = dialogView.findViewById(R.id.subscriptionBox);
        Button       btnStartDate   = dialogView.findViewById(R.id.btnStartDate);

        Date[] startDate = {new Date()};
        btnStartDate.setText(sdf.format(startDate[0]));

        btnStartDate.setOnClickListener(vv -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate[0]);
            new DatePickerDialog(this, (dpView, year, month, day) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(year, month, day, 0, 0, 0);
                sel.set(Calendar.MILLISECOND, 0);
                startDate[0] = sel.getTime();
                btnStartDate.setText(sdf.format(startDate[0]));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        ArrayAdapter<String> catAdapter = buildCategoryAdapter();
        spCategory.setAdapter(catAdapter);
        spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

        if (isSubscription) {
            int subPos = catAdapter.getPosition("Subscriptions");
            if (subPos >= 0) spCategory.setSelection(subPos);
            subscriptionBox.setVisibility(View.VISIBLE);
        }

        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean isSub = "Subscriptions".equals(parent.getItemAtPosition(position).toString());
                subscriptionBox.setVisibility(isSub ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                subscriptionBox.setVisibility(View.GONE);
            }
        });

        ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                this, R.array.expense_frequencies, R.layout.spinner_item_white);
        freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spFrequency.setAdapter(freqAdapter);
        spFrequency.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        btnSave.setOnClickListener(view -> {
            String title       = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String amountStr   = etAmount.getText().toString().trim();
            String category    = spCategory.getSelectedItem().toString();
            String frequency   = spFrequency.getSelectedItem().toString();

            if (title.isEmpty() || description.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) { Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            FixedExpense expense = new FixedExpense(category, title, amount, frequency, description);
            if ("Subscriptions".equals(category)) expense.setLastPayment(startDate[0]);
            currentUser.addExpense(expense);

            if ("Subscriptions".equals(category)) addSubscriptionBox(expense);
            else addExpenseBox(expense);

            dialog.dismiss();
        });

        btnExit.setOnClickListener(event -> dialog.dismiss());
    }

    // ─── Add savings dialog ─────────────────────────────────────────────────────

    private void showAddSavingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_savings, null);

        EditText    etTitle  = dialogView.findViewById(R.id.etTitle);
        EditText    etTarget = dialogView.findViewById(R.id.etTarget);
        EditText    etNotes  = dialogView.findViewById(R.id.etNotes);
        Button      btnDate  = dialogView.findViewById(R.id.btnTargetDate);
        Button      btnSave  = dialogView.findViewById(R.id.btnSave);
        ImageButton btnExit  = dialogView.findViewById(R.id.btnExit);

        Date[] targetDate = {null};

        btnDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (targetDate[0] != null) cal.setTime(targetDate[0]);
            new DatePickerDialog(this, (dpView, year, month, day) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(year, month, day, 23, 59, 59);
                targetDate[0] = sel.getTime();
                btnDate.setText(sdf.format(targetDate[0]));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        btnSave.setOnClickListener(v -> {
            String title     = etTitle.getText().toString().trim();
            String targetStr = etTarget.getText().toString().trim();
            String notes     = etNotes.getText().toString().trim();

            if (title.isEmpty() || targetStr.isEmpty()) {
                Toast.makeText(this, "Please enter a name and target amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double target;
            try {
                target = Double.parseDouble(targetStr);
                if (target <= 0) { Toast.makeText(this, "Target must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid target amount", Toast.LENGTH_SHORT).show();
                return;
            }

            SavingsGoal goal = new SavingsGoal(title, notes, target, targetDate[0]);
            savingsList.addGoal(goal);
            addSavingsGoalBox(goal);
            dialog.dismiss();
        });

        btnExit.setOnClickListener(e -> dialog.dismiss());
    }

    // ─── Savings goal box ───────────────────────────────────────────────────────

    private void addSavingsGoalBox(SavingsGoal goal) { addSavingsGoalBox(goal, -1); }

    private void addSavingsGoalBox(SavingsGoal goal, int index) {
        LinearLayout container = findViewById(R.id.savingsContainer);
        View box = getLayoutInflater().inflate(R.layout.savings_item, container, false);

        TextView    tvTitle       = box.findViewById(R.id.tvTitle);
        TextView    tvCompleted   = box.findViewById(R.id.tvCompleted);
        TextView    tvTarget      = box.findViewById(R.id.tvTarget);
        ProgressBar progressBar   = box.findViewById(R.id.progressBar);
        TextView    tvSaved       = box.findViewById(R.id.tvSaved);
        TextView    tvPercent     = box.findViewById(R.id.tvPercent);
        TextView    tvDeadline    = box.findViewById(R.id.tvDeadline);
        TextView    tvNotes       = box.findViewById(R.id.tvNotes);
        EditText    etContribute  = box.findViewById(R.id.etContribute);
        Button      btnContribute = box.findViewById(R.id.btnContribute);
        TextView    tvLastContrib = box.findViewById(R.id.tvLastContrib);
        Button      btnHistory    = box.findViewById(R.id.btnHistory);
        ImageButton btnMenu       = box.findViewById(R.id.btnMenu);

        refreshSavingsCard(goal, tvTitle, tvCompleted, tvTarget, progressBar,
                tvSaved, tvPercent, tvDeadline, tvNotes, tvLastContrib);

        btnContribute.setOnClickListener(v -> {
            String input = etContribute.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "Enter an amount to add", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(input);
                if (amount <= 0) { Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            goal.addContribution(amount);
            etContribute.setText("");
            refreshSavingsCard(goal, tvTitle, tvCompleted, tvTarget, progressBar,
                    tvSaved, tvPercent, tvDeadline, tvNotes, tvLastContrib);
            if (goal.isCompleted())
                Toast.makeText(this, "Goal '" + goal.getTitle() + "' completed!", Toast.LENGTH_SHORT).show();
        });

        btnHistory.setOnClickListener(v -> showSavingsHistoryDialog(goal));

        btnMenu.setOnClickListener(v -> showSavingsEditDialog(goal, box, container,
                tvTitle, tvCompleted, tvTarget, progressBar, tvSaved, tvPercent,
                tvDeadline, tvNotes, tvLastContrib));

        if (index >= 0) container.addView(box, index);
        else container.addView(box);
    }

    private void refreshSavingsCard(SavingsGoal goal,
            TextView tvTitle, TextView tvCompleted, TextView tvTarget,
            ProgressBar progressBar, TextView tvSaved, TextView tvPercent,
            TextView tvDeadline, TextView tvNotes, TextView tvLastContrib) {

        tvTitle.setText(goal.getTitle());
        tvTarget.setText(String.format("€%.2f", goal.getTargetAmount()));
        progressBar.setProgress(goal.getProgressPercent());
        tvPercent.setText(goal.getProgressPercent() + "%");

        if (goal.isCompleted()) {
            tvCompleted.setVisibility(View.VISIBLE);
            tvSaved.setText(String.format("€%.2f saved · Goal reached!", goal.getCurrentAmount()));
        } else {
            tvCompleted.setVisibility(View.GONE);
            tvSaved.setText(String.format("€%.2f saved · €%.2f to go",
                    goal.getCurrentAmount(), goal.getRemainingAmount()));
        }

        int days = goal.getDaysToTarget();
        if (goal.getTargetDate() != null) {
            if (days == 0)
                tvDeadline.setText("Deadline: today");
            else if (days > 0)
                tvDeadline.setText("Deadline: " + sdf.format(goal.getTargetDate()) + " (" + days + " days left)");
            else
                tvDeadline.setText("Deadline passed: " + sdf.format(goal.getTargetDate()));
            tvDeadline.setVisibility(View.VISIBLE);
        } else {
            tvDeadline.setVisibility(View.GONE);
        }

        List<Date> dates = goal.getContributionDates();
        Date lastDate = null;
        for (int i = dates.size() - 1; i >= 0; i--) {
            if (dates.get(i) != null) { lastDate = dates.get(i); break; }
        }
        tvLastContrib.setText(lastDate != null ? "Last: " + sdf.format(lastDate) : "No contributions yet");

        if (goal.getNotes() != null && !goal.getNotes().isEmpty()) {
            tvNotes.setText(goal.getNotes());
            tvNotes.setVisibility(View.VISIBLE);
        } else {
            tvNotes.setVisibility(View.GONE);
        }
    }

    // ─── Savings edit dialog ────────────────────────────────────────────────────

    private void showSavingsEditDialog(SavingsGoal goal, View box, LinearLayout container,
            TextView tvTitle, TextView tvCompleted, TextView tvTarget,
            ProgressBar progressBar, TextView tvSaved, TextView tvPercent,
            TextView tvDeadline, TextView tvNotes, TextView tvLastContrib) {

        View editView = getLayoutInflater().inflate(R.layout.dialog_add_savings, null);

        EditText    etTitle   = editView.findViewById(R.id.etTitle);
        EditText    etTarget  = editView.findViewById(R.id.etTarget);
        EditText    etNotes   = editView.findViewById(R.id.etNotes);
        Button      btnDate   = editView.findViewById(R.id.btnTargetDate);
        Button      btnSave   = editView.findViewById(R.id.btnSave);
        ImageButton btnExit   = editView.findViewById(R.id.btnExit);
        TextView    tvHeading = editView.findViewById(R.id.tvHeadingAdd);

        if (tvHeading != null) tvHeading.setText("Edit Savings Goal");
        btnSave.setText("Save Changes");

        etTitle.setText(goal.getTitle());
        etTarget.setText(String.format(Locale.getDefault(), "%.2f", goal.getTargetAmount()));
        etNotes.setText(goal.getNotes());

        Date[] targetDate = {goal.getTargetDate()};
        btnDate.setText(targetDate[0] != null ? sdf.format(targetDate[0]) : "No deadline");

        btnDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (targetDate[0] != null) cal.setTime(targetDate[0]);
            new DatePickerDialog(this, (dpView, year, month, day) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(year, month, day, 23, 59, 59);
                targetDate[0] = sel.getTime();
                btnDate.setText(sdf.format(targetDate[0]));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(editView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        LinearLayout layout = (LinearLayout) editView;
        Button btnDelete = new Button(this);
        btnDelete.setText("Delete Goal");
        btnDelete.setTextColor(Color.parseColor("#FF6B6B"));
        layout.addView(btnDelete);

        btnDelete.setOnClickListener(e -> {
            View delView = getLayoutInflater().inflate(R.layout.delete_expense_dialog, null);
            Button btnCancel = delView.findViewById(R.id.btnCancel);
            Button btnConfirmDelete = delView.findViewById(R.id.btnDelete);

            AlertDialog delDialog = new AlertDialog.Builder(this).setView(delView).create();
            if (delDialog.getWindow() != null)
                delDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            delDialog.show();

            btnCancel.setOnClickListener(ev -> { delDialog.dismiss(); dialog.dismiss(); });
            btnConfirmDelete.setOnClickListener(ev -> {
                savingsList.removeGoal(goal);
                container.removeView(box);
                delDialog.dismiss();
                dialog.dismiss();
            });
        });

        btnSave.setOnClickListener(v -> {
            String titleStr  = etTitle.getText().toString().trim();
            String targetStr = etTarget.getText().toString().trim();
            String notesStr  = etNotes.getText().toString().trim();

            if (titleStr.isEmpty() || targetStr.isEmpty()) {
                Toast.makeText(this, "Name and target are required", Toast.LENGTH_SHORT).show();
                return;
            }

            double target;
            try {
                target = Double.parseDouble(targetStr);
                if (target <= 0) { Toast.makeText(this, "Target must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid target amount", Toast.LENGTH_SHORT).show();
                return;
            }

            goal.setTitle(titleStr);
            goal.setTargetAmount(target);
            goal.setNotes(notesStr);
            goal.setTargetDate(targetDate[0]);

            refreshSavingsCard(goal, tvTitle, tvCompleted, tvTarget, progressBar,
                    tvSaved, tvPercent, tvDeadline, tvNotes, tvLastContrib);
            dialog.dismiss();
        });

        btnExit.setOnClickListener(e -> dialog.dismiss());
    }

    // ─── Savings history dialog ─────────────────────────────────────────────────

    private void showSavingsHistoryDialog(SavingsGoal goal) {
        View histView = getLayoutInflater().inflate(R.layout.payment_history_dialog, null);
        LinearLayout histContainer = histView.findViewById(R.id.historyContainer);
        ImageButton  btnClose      = histView.findViewById(R.id.btnCloseHistory);

        List<Double> hist  = goal.getContributionHist();
        List<Date>   dates = goal.getContributionDates();

        if (hist.size() <= 1) {
            TextView empty = new TextView(this);
            empty.setText("No contributions recorded yet");
            empty.setTextColor(0xAAFFFFFF);
            empty.setTextSize(14);
            histContainer.addView(empty);
        } else {
            for (int i = 1; i < hist.size(); i++) {
                double delta = hist.get(i) - hist.get(i - 1);
                Date   date  = (i < dates.size()) ? dates.get(i) : null;
                TextView tv  = new TextView(this);
                tv.setText((date != null ? sdf.format(date) : "--") + "   +" + String.format("€%.2f", delta));
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(14);
                tv.setPadding(0, 8, 0, 8);
                histContainer.addView(tv);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setView(histView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        btnClose.setOnClickListener(vv -> dialog.dismiss());
    }

    // ─── Fixed expense box ──────────────────────────────────────────────────────

    private void addExpenseBox(FixedExpense expense) { addExpenseBox(expense, -1); }

    private void addExpenseBox(FixedExpense expense, int index) {
        LinearLayout container = findViewById(R.id.fixedExpenseContainer);
        View box = getLayoutInflater().inflate(R.layout.expense_item, container, false);

        TextView    tvCategory    = box.findViewById(R.id.tvCategory);
        TextView    tvTitle       = box.findViewById(R.id.tvTitle);
        TextView    tvAmount      = box.findViewById(R.id.tvAmount);
        Button      btnPay        = box.findViewById(R.id.btnPay);
        TextView    tvTotalPaid   = box.findViewById(R.id.tvTotalPaid);
        TextView    tvNextPayment = box.findViewById(R.id.tvNextPayment);
        EditText    etAmount      = box.findViewById(R.id.etAmount);
        ImageButton btnMenu       = box.findViewById(R.id.btnMenu);
        TextView    tvFreq        = box.findViewById(R.id.tvFreq);
        TextView    tvNotes       = box.findViewById(R.id.tvNotes);
        TextView    tvLastPayment = box.findViewById(R.id.tvLastPayment);
        Button      btnHistory    = box.findViewById(R.id.btnHistory);

        tvCategory.setText(expense.getCategory());
        tvTitle.setText(expense.getTitle());
        tvAmount.setText(String.format("€%.2f", expense.getAmount()));
        tvTotalPaid.setText(String.format("€%.2f", expense.getTotalPaid()));
        tvFreq.setText(expense.getFrequency());
        setDaysText(tvNextPayment, expense.getDaysUntilNextPayment());
        setNotesText(tvNotes, expense.getNotes());
        setLastPaymentText(tvLastPayment, expense.getLastPayment());

        btnHistory.setOnClickListener(v -> showHistoryDialog(expense));

        btnPay.setOnClickListener(ev -> {
            String input = etAmount.getText().toString().trim();
            double amountToPay;
            if (input.isEmpty()) {
                amountToPay = expense.getAmount();
            } else {
                try {
                    amountToPay = Double.parseDouble(input);
                    if (amountToPay <= 0) { Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount entered", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            expense.addPayment(amountToPay);
            tvTotalPaid.setText(String.format("€%.2f", expense.getTotalPaid()));
            expense.setLastPayment(new Date());
            setLastPaymentText(tvLastPayment, expense.getLastPayment());
            etAmount.setText("");
            setDaysText(tvNextPayment, expense.getDaysUntilNextPayment());
            recordTransaction(expense.getCategory(), amountToPay);
        });

        btnMenu.setOnClickListener(ve -> {
            View editView = getLayoutInflater().inflate(R.layout.edit_expense, null);

            TextView    tvEditTitle       = editView.findViewById(R.id.tvTitle);
            TextView    tvEditTotalAmount = editView.findViewById(R.id.tvEuroAmount);
            ImageButton btnExit           = editView.findViewById(R.id.btnExit);
            ImageButton btnUndo           = editView.findViewById(R.id.btnUndo);
            ImageButton btnRedo           = editView.findViewById(R.id.btnRedo);
            EditText    editAmount        = editView.findViewById(R.id.editAmount);
            Spinner     spCategory        = editView.findViewById(R.id.spCategory);
            Spinner     spFreq            = editView.findViewById(R.id.spFreq);
            EditText    etTitle           = editView.findViewById(R.id.etTitle);
            EditText    etDesc            = editView.findViewById(R.id.etDesc);
            EditText    etEditAmount      = editView.findViewById(R.id.etAmount);
            Button      btnSet            = editView.findViewById(R.id.btnSet);
            Button      btnDeleteDialog   = editView.findViewById(R.id.btnDelete);

            tvEditTitle.setText(expense.getTitle());
            tvEditTotalAmount.setText(String.format("%.2f", expense.getTotalPaid()));

            ArrayAdapter<String> catAdapter = buildCategoryAdapter();
            spCategory.setAdapter(catAdapter);
            spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));
            spCategory.setSelection(catAdapter.getPosition(expense.getCategory()));

            ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                    this, R.array.expense_frequencies, R.layout.spinner_item_white);
            freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spFreq.setAdapter(freqAdapter);
            spFreq.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));
            spFreq.setSelection(freqAdapter.getPosition(expense.getFrequency()));

            AlertDialog dialog = new AlertDialog.Builder(TrackersActivity.this).setView(editView).create();
            if (dialog.getWindow() != null)
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            if (btnExit != null) btnExit.setOnClickListener(eve -> dialog.dismiss());

            btnUndo.setOnClickListener(event -> {
                expense.setTotalPaid(expense.getHistAtCurIndex(-1));
                tvEditTotalAmount.setText(String.format("%.2f", expense.getTotalPaid()));
                updateUndoRedoTint(btnUndo, btnRedo, expense);
            });

            btnRedo.setOnClickListener(event -> {
                expense.setTotalPaid(expense.getHistAtCurIndex(1));
                tvEditTotalAmount.setText(String.format("%.2f", expense.getTotalPaid()));
                updateUndoRedoTint(btnUndo, btnRedo, expense);
            });

            btnSet.setOnClickListener(event -> {
                String titleStr     = etTitle.getText().toString();
                String descStr      = etDesc.getText().toString();
                String amountStr    = etEditAmount.getText().toString();
                String totalAmntStr = editAmount.getText().toString();
                String originalCategory = expense.getCategory();

                if (!totalAmntStr.isEmpty()) {
                    try {
                        double val = Double.parseDouble(totalAmntStr);
                        if (val < 0) { Toast.makeText(this, "Total cannot be negative", Toast.LENGTH_SHORT).show(); return; }
                        expense.setTotalPaid(val);
                        expense.addNewHist();
                    } catch (NumberFormatException e) { Toast.makeText(this, "Invalid total", Toast.LENGTH_SHORT).show(); return; }
                }
                if (!amountStr.isEmpty()) {
                    try {
                        double val = Double.parseDouble(amountStr);
                        if (val <= 0) { Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
                        expense.setAmount(val);
                    } catch (NumberFormatException e) { Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show(); return; }
                }
                if (!spCategory.getSelectedItem().toString().equals(expense.getCategory()))
                    expense.setCategory(spCategory.getSelectedItem().toString());
                if (!spFreq.getSelectedItem().toString().equals(expense.getFrequency()))
                    expense.setFrequency(spFreq.getSelectedItem().toString());
                if (!titleStr.isEmpty()) expense.setTitle(titleStr);
                if (!descStr.isEmpty()) expense.setNotes(descStr);

                boolean isNowSub = "Subscriptions".equals(expense.getCategory());
                boolean wasSub   = "Subscriptions".equals(originalCategory);
                if (wasSub != isNowSub) {
                    container.removeView(box);
                    dialog.dismiss();
                    if (isNowSub) addSubscriptionBox(expense);
                    else addExpenseBox(expense);
                    return;
                }

                tvCategory.setText(expense.getCategory());
                tvTitle.setText(expense.getTitle());
                tvAmount.setText(String.format("€%.2f", expense.getAmount()));
                tvTotalPaid.setText(String.format("€%.2f", expense.getTotalPaid()));
                tvFreq.setText(expense.getFrequency());
                setDaysText(tvNextPayment, expense.getDaysUntilNextPayment());
                setNotesText(tvNotes, expense.getNotes());
                setLastPaymentText(tvLastPayment, expense.getLastPayment());
                dialog.dismiss();
            });

            btnDeleteDialog.setOnClickListener(event -> {
                View delView = getLayoutInflater().inflate(R.layout.delete_expense_dialog, null);
                Button btnCancel = delView.findViewById(R.id.btnCancel);
                Button btnDelete = delView.findViewById(R.id.btnDelete);

                AlertDialog delDialog = new AlertDialog.Builder(TrackersActivity.this).setView(delView).create();
                if (delDialog.getWindow() != null)
                    delDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                delDialog.show();

                btnCancel.setOnClickListener(e -> { delDialog.dismiss(); dialog.dismiss(); });
                btnDelete.setOnClickListener(e -> {
                    expenseList.removeExpense(expense);
                    container.removeView(box);
                    delDialog.dismiss();
                    dialog.dismiss();
                });
            });
        });

        if (index >= 0) container.addView(box, index);
        else container.addView(box);
    }

    // ─── Subscription box ───────────────────────────────────────────────────────

    private void addSubscriptionBox(FixedExpense expense) { addSubscriptionBox(expense, -1); }

    private void addSubscriptionBox(FixedExpense expense, int index) {
        LinearLayout container = findViewById(R.id.subscriptionsContainer);
        View box = getLayoutInflater().inflate(R.layout.subscription_item, container, false);

        TextView    tvCategory     = box.findViewById(R.id.tvCategory);
        TextView    tvTitle        = box.findViewById(R.id.tvTitle);
        TextView    tvAmount       = box.findViewById(R.id.tvAmount);
        TextView    tvFreq         = box.findViewById(R.id.tvFreq);
        TextView    tvTotalPaid    = box.findViewById(R.id.tvTotalPaid);
        TextView    tvNextPayment  = box.findViewById(R.id.tvNextPayment);
        TextView    tvAnnualCost   = box.findViewById(R.id.tvAnnualCost);
        TextView    tvLastPayment  = box.findViewById(R.id.tvLastPayment);
        TextView    tvAutoRecorded = box.findViewById(R.id.tvAutoRecorded);
        TextView    tvNotes        = box.findViewById(R.id.tvNotes);
        ImageButton btnMenu        = box.findViewById(R.id.btnMenu);
        Button      btnHistory     = box.findViewById(R.id.btnHistory);

        tvCategory.setText(expense.getCategory());
        tvTitle.setText(expense.getTitle());
        tvAmount.setText(String.format("€%.2f", expense.getAmount()));
        tvFreq.setText(expense.getFrequency());
        tvAnnualCost.setText(String.format("€%.2f / year", expense.getAnnualCost()));
        setNotesText(tvNotes, expense.getNotes());

        int completed = expense.processCompletedCycles();
        if (completed > 0) {
            tvAutoRecorded.setText(completed + " payment(s) auto-recorded");
            tvAutoRecorded.setVisibility(View.VISIBLE);
            Toast.makeText(this, completed + " " + expense.getTitle() + " payment(s) auto-recorded", Toast.LENGTH_SHORT).show();
        }

        tvTotalPaid.setText(String.format("€%.2f", expense.getTotalPaid()));
        setDaysText(tvNextPayment, expense.getDaysUntilNextPayment());
        setLastPaymentText(tvLastPayment, expense.getLastPayment());

        btnHistory.setOnClickListener(v -> showHistoryDialog(expense));

        btnMenu.setOnClickListener(ev -> {
            View editView = getLayoutInflater().inflate(R.layout.edit_expense, null);

            TextView    tvEditTitle       = editView.findViewById(R.id.tvTitle);
            TextView    tvEditTotalAmount = editView.findViewById(R.id.tvEuroAmount);
            ImageButton btnExit           = editView.findViewById(R.id.btnExit);
            ImageButton btnUndo           = editView.findViewById(R.id.btnUndo);
            ImageButton btnRedo           = editView.findViewById(R.id.btnRedo);
            EditText    editAmount        = editView.findViewById(R.id.editAmount);
            Spinner     spCategory        = editView.findViewById(R.id.spCategory);
            Spinner     spFreq            = editView.findViewById(R.id.spFreq);
            EditText    etTitle           = editView.findViewById(R.id.etTitle);
            EditText    etDesc            = editView.findViewById(R.id.etDesc);
            EditText    etEditAmount      = editView.findViewById(R.id.etAmount);
            Button      btnSet            = editView.findViewById(R.id.btnSet);
            Button      btnDeleteDialog   = editView.findViewById(R.id.btnDelete);
            LinearLayout startDateRow     = editView.findViewById(R.id.startDateRow);
            Button      btnEditStartDate  = editView.findViewById(R.id.btnEditStartDate);

            tvEditTitle.setText(expense.getTitle());
            tvEditTotalAmount.setText(String.format("%.2f", expense.getTotalPaid()));

            startDateRow.setVisibility(View.VISIBLE);
            Date[] editStartDate = {expense.getLastPayment() != null ? expense.getLastPayment() : new Date()};
            btnEditStartDate.setText(sdf.format(editStartDate[0]));
            btnEditStartDate.setOnClickListener(vv -> {
                Calendar cal = Calendar.getInstance();
                cal.setTime(editStartDate[0]);
                new DatePickerDialog(this, (dpView, year, month, day) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, day, 0, 0, 0);
                    sel.set(Calendar.MILLISECOND, 0);
                    editStartDate[0] = sel.getTime();
                    btnEditStartDate.setText(sdf.format(editStartDate[0]));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });

            ArrayAdapter<String> catAdapter = buildCategoryAdapter();
            spCategory.setAdapter(catAdapter);
            spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));
            spCategory.setSelection(catAdapter.getPosition(expense.getCategory()));

            ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                    this, R.array.expense_frequencies, R.layout.spinner_item_white);
            freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spFreq.setAdapter(freqAdapter);
            spFreq.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));
            spFreq.setSelection(freqAdapter.getPosition(expense.getFrequency()));

            AlertDialog dialog = new AlertDialog.Builder(TrackersActivity.this).setView(editView).create();
            if (dialog.getWindow() != null)
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            if (btnExit != null) btnExit.setOnClickListener(eve -> dialog.dismiss());

            btnUndo.setOnClickListener(event -> {
                expense.setTotalPaid(expense.getHistAtCurIndex(-1));
                tvEditTotalAmount.setText(String.format("%.2f", expense.getTotalPaid()));
                updateUndoRedoTint(btnUndo, btnRedo, expense);
            });

            btnRedo.setOnClickListener(event -> {
                expense.setTotalPaid(expense.getHistAtCurIndex(1));
                tvEditTotalAmount.setText(String.format("%.2f", expense.getTotalPaid()));
                updateUndoRedoTint(btnUndo, btnRedo, expense);
            });

            btnSet.setOnClickListener(event -> {
                String titleStr     = etTitle.getText().toString();
                String descStr      = etDesc.getText().toString();
                String amountStr    = etEditAmount.getText().toString();
                String totalAmntStr = editAmount.getText().toString();
                String originalCategory = expense.getCategory();

                if (!totalAmntStr.isEmpty()) {
                    try {
                        double val = Double.parseDouble(totalAmntStr);
                        if (val < 0) { Toast.makeText(this, "Total cannot be negative", Toast.LENGTH_SHORT).show(); return; }
                        expense.setTotalPaid(val);
                        expense.addNewHist();
                    } catch (NumberFormatException e) { Toast.makeText(this, "Invalid total", Toast.LENGTH_SHORT).show(); return; }
                }
                if (!amountStr.isEmpty()) {
                    try {
                        double val = Double.parseDouble(amountStr);
                        if (val <= 0) { Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
                        expense.setAmount(val);
                    } catch (NumberFormatException e) { Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show(); return; }
                }
                if (!spCategory.getSelectedItem().toString().equals(expense.getCategory()))
                    expense.setCategory(spCategory.getSelectedItem().toString());
                if (!spFreq.getSelectedItem().toString().equals(expense.getFrequency()))
                    expense.setFrequency(spFreq.getSelectedItem().toString());
                if (!titleStr.isEmpty()) expense.setTitle(titleStr);
                if (!descStr.isEmpty()) expense.setNotes(descStr);

                expense.setLastPayment(editStartDate[0]);

                boolean isNowSub = "Subscriptions".equals(expense.getCategory());
                boolean wasSub   = "Subscriptions".equals(originalCategory);
                if (wasSub != isNowSub) {
                    container.removeView(box);
                    dialog.dismiss();
                    if (isNowSub) addSubscriptionBox(expense);
                    else addExpenseBox(expense);
                    return;
                }

                tvCategory.setText(expense.getCategory());
                tvTitle.setText(expense.getTitle());
                tvAmount.setText(String.format("€%.2f", expense.getAmount()));
                tvTotalPaid.setText(String.format("€%.2f", expense.getTotalPaid()));
                tvFreq.setText(expense.getFrequency());
                tvAnnualCost.setText(String.format("€%.2f / year", expense.getAnnualCost()));
                setDaysText(tvNextPayment, expense.getDaysUntilNextPayment());
                setLastPaymentText(tvLastPayment, expense.getLastPayment());
                setNotesText(tvNotes, expense.getNotes());
                dialog.dismiss();
            });

            btnDeleteDialog.setOnClickListener(event -> {
                View delView = getLayoutInflater().inflate(R.layout.delete_expense_dialog, null);
                Button btnCancel = delView.findViewById(R.id.btnCancel);
                Button btnDelete = delView.findViewById(R.id.btnDelete);

                AlertDialog delDialog = new AlertDialog.Builder(TrackersActivity.this).setView(delView).create();
                if (delDialog.getWindow() != null)
                    delDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                delDialog.show();

                btnCancel.setOnClickListener(e -> { delDialog.dismiss(); dialog.dismiss(); });
                btnDelete.setOnClickListener(e -> {
                    expenseList.removeExpense(expense);
                    container.removeView(box);
                    delDialog.dismiss();
                    dialog.dismiss();
                });
            });
        });

        if (index >= 0) container.addView(box, index);
        else container.addView(box);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private ArrayAdapter<String> buildCategoryAdapter() {
        List<String> items = new ArrayList<>();
        for (Category c : percDivider.getAllDividers()) {
            items.add(c.getCategory());
        }
        items.add("Subscriptions");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, items);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        return adapter;
    }

    private void setDaysText(TextView tv, int days) {
        if (days == 0) {
            tv.setText("Due today");
            tv.setTextColor(Color.RED);
        } else {
            tv.setText(String.valueOf(days));
            tv.setTextColor(Color.WHITE);
        }
    }

    private void setNotesText(TextView tv, String notes) {
        if (notes != null && !notes.isEmpty()) {
            tv.setText(notes);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void setLastPaymentText(TextView tv, Date date) {
        tv.setText(date != null ? "Last: " + sdf.format(date) : "No payments yet");
    }

    private void updateUndoRedoTint(ImageButton btnUndo, ImageButton btnRedo, FixedExpense expense) {
        btnUndo.setImageTintList(ColorStateList.valueOf(Color.parseColor(
                expense.getPos() == 0 ? "#B3B1B1" : "#FFFFFF")));
        btnRedo.setImageTintList(ColorStateList.valueOf(Color.parseColor(
                expense.getPos() == expense.getPaymentHist().size() - 1 ? "#B3B1B1" : "#FFFFFF")));
    }

    private void showHistoryDialog(FixedExpense expense) {
        View histView = getLayoutInflater().inflate(R.layout.payment_history_dialog, null);
        LinearLayout histContainer = histView.findViewById(R.id.historyContainer);
        ImageButton  btnClose      = histView.findViewById(R.id.btnCloseHistory);

        List<Double> hist  = expense.getPaymentHist();
        List<Date>   dates = expense.getPaymentDates();

        if (hist.size() <= 1) {
            TextView empty = new TextView(this);
            empty.setText("No payments recorded yet");
            empty.setTextColor(0xAAFFFFFF);
            empty.setTextSize(14);
            histContainer.addView(empty);
        } else {
            for (int i = 1; i < hist.size(); i++) {
                double delta = hist.get(i) - hist.get(i - 1);
                Date   date  = (i < dates.size()) ? dates.get(i) : null;
                TextView tv  = new TextView(this);
                tv.setText((date != null ? sdf.format(date) : "--") + "   +" + String.format("€%.2f", delta));
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(14);
                tv.setPadding(0, 8, 0, 8);
                histContainer.addView(tv);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setView(histView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        btnClose.setOnClickListener(vv -> dialog.dismiss());
    }

    private void recordTransaction(String category, double amount) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> tx = new HashMap<>();
        tx.put("amount", amount);
        tx.put("type", "Expense");
        tx.put("category", category);
        tx.put("createdAt", new Date());

        db.collection("users")
                .document(user.getUid())
                .collection("transactions")
                .add(tx)
                .addOnSuccessListener(doc ->
                        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
