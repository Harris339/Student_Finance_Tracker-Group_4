package com.example.studentfinancetracker_wk4;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ExpensesActivity extends AppCompatActivity {
    Button addButton;
    ImageButton backbtn;

    User currentUser;
    ExpenseList expenseList;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_fixed_expenses);

        addButton = findViewById(R.id.addButton);
        backbtn = findViewById(R.id.btnBack);

        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) expenseList = currentUser.getExpenseList();

        if (expenseList != null) {
            for (FixedExpense expense : expenseList.getAllExpenses()) {
                if ("Subscriptions".equals(expense.getCategory())) {
                    addSubscriptionBox(expense);
                } else {
                    addExpenseBox(expense);
                }
            }
        }

        addButton.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);

            Spinner spCategory = dialogView.findViewById(R.id.spCategory);
            Spinner spFrequency = dialogView.findViewById(R.id.spFrequency);
            EditText etTitle = dialogView.findViewById(R.id.etTitle);
            EditText etDescription = dialogView.findViewById(R.id.etDescription);
            EditText etAmount = dialogView.findViewById(R.id.etAmount);
            Button btnSave = dialogView.findViewById(R.id.btnSaveExpense);
            ImageButton btnExit = dialogView.findViewById(R.id.btnExit);
            LinearLayout subscriptionBox = dialogView.findViewById(R.id.subscriptionBox);
            Button btnStartDate = dialogView.findViewById(R.id.btnStartDate);

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

            ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                    this, R.array.expense_categories, R.layout.spinner_item_white);
            catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spCategory.setAdapter(catAdapter);
            spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

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
                String title = etTitle.getText().toString();
                String description = etDescription.getText().toString();
                String amountStr = etAmount.getText().toString();
                String category = spCategory.getSelectedItem().toString();
                String frequency = spFrequency.getSelectedItem().toString();

                if (title.isEmpty() || description.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
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
        });

        backbtn.setOnClickListener(v -> finish());
    }

    // ─── Expense box ────────────────────────────────────────────────────────────

    private void addExpenseBox(FixedExpense expense) { addExpenseBox(expense, -1); }

    private void addExpenseBox(FixedExpense expense, int index) {
        LinearLayout container = findViewById(R.id.expenseContainer);
        View box = getLayoutInflater().inflate(R.layout.expense_item, container, false);

        TextView tvCategory    = box.findViewById(R.id.tvCategory);
        TextView tvTitle       = box.findViewById(R.id.tvTitle);
        TextView tvAmount      = box.findViewById(R.id.tvAmount);
        Button   btnPay        = box.findViewById(R.id.btnPay);
        TextView tvTotalPaid   = box.findViewById(R.id.tvTotalPaid);
        TextView tvNextPayment = box.findViewById(R.id.tvNextPayment);
        EditText etAmount      = box.findViewById(R.id.etAmount);
        ImageButton btnMenu    = box.findViewById(R.id.btnMenu);
        TextView tvFreq        = box.findViewById(R.id.tvFreq);
        TextView tvNotes       = box.findViewById(R.id.tvNotes);
        TextView tvLastPayment = box.findViewById(R.id.tvLastPayment);
        Button   btnHistory    = box.findViewById(R.id.btnHistory);

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
                    if (amountToPay <= 0) {
                        Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount entered", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            recordTransaction(tvCategory.getText().toString(), amountToPay);
            expense.addPayment(amountToPay);
            expense.addNewHist();
            tvTotalPaid.setText(String.format("€%.2f", expense.getTotalPaid()));
            expense.setLastPayment(new Date());
            setLastPaymentText(tvLastPayment, expense.getLastPayment());
            etAmount.setText("");
            setDaysText(tvNextPayment, expense.getDaysUntilNextPayment());
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

            ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                    this, R.array.expense_categories, R.layout.spinner_item_white);
            catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spCategory.setAdapter(catAdapter);
            spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));
            spCategory.setSelection(catAdapter.getPosition(expense.getCategory()));

            ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                    this, R.array.expense_frequencies, R.layout.spinner_item_white);
            freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spFreq.setAdapter(freqAdapter);
            spFreq.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));
            spFreq.setSelection(freqAdapter.getPosition(expense.getFrequency()));

            AlertDialog dialog = new AlertDialog.Builder(ExpensesActivity.this).setView(editView).create();
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
                        double v = Double.parseDouble(totalAmntStr);
                        if (v < 0) { Toast.makeText(this, "Total cannot be negative", Toast.LENGTH_SHORT).show(); return; }
                        expense.setTotalPaid(v);
                        expense.addNewHist();
                    } catch (NumberFormatException e) { Toast.makeText(this, "Invalid total", Toast.LENGTH_SHORT).show(); return; }
                }
                if (!amountStr.isEmpty()) {
                    try {
                        double v = Double.parseDouble(amountStr);
                        if (v <= 0) { Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
                        expense.setAmount(v);
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
                    int i = container.indexOfChild(box);
                    container.removeView(box);
                    dialog.dismiss();
                    if (isNowSub) addSubscriptionBox(expense, i);
                    else addExpenseBox(expense, i);
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

                AlertDialog delDialog = new AlertDialog.Builder(ExpensesActivity.this).setView(delView).create();
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
        LinearLayout container = findViewById(R.id.expenseContainer);
        View box = getLayoutInflater().inflate(R.layout.subscription_item, container, false);

        TextView    tvCategory      = box.findViewById(R.id.tvCategory);
        TextView    tvTitle         = box.findViewById(R.id.tvTitle);
        TextView    tvAmount        = box.findViewById(R.id.tvAmount);
        TextView    tvFreq          = box.findViewById(R.id.tvFreq);
        TextView    tvTotalPaid     = box.findViewById(R.id.tvTotalPaid);
        TextView    tvNextPayment   = box.findViewById(R.id.tvNextPayment);
        TextView    tvAnnualCost    = box.findViewById(R.id.tvAnnualCost);
        TextView    tvLastPayment   = box.findViewById(R.id.tvLastPayment);
        TextView    tvAutoRecorded  = box.findViewById(R.id.tvAutoRecorded);
        TextView    tvNotes         = box.findViewById(R.id.tvNotes);
        ImageButton btnMenu         = box.findViewById(R.id.btnMenu);
        Button      btnHistory      = box.findViewById(R.id.btnHistory);

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

            // Show and populate start date picker
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

            ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                    this, R.array.expense_categories, R.layout.spinner_item_white);
            catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spCategory.setAdapter(catAdapter);
            spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));
            spCategory.setSelection(catAdapter.getPosition(expense.getCategory()));

            ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                    this, R.array.expense_frequencies, R.layout.spinner_item_white);
            freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            spFreq.setAdapter(freqAdapter);
            spFreq.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));
            spFreq.setSelection(freqAdapter.getPosition(expense.getFrequency()));

            AlertDialog dialog = new AlertDialog.Builder(ExpensesActivity.this).setView(editView).create();
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
                        double v = Double.parseDouble(totalAmntStr);
                        if (v < 0) { Toast.makeText(this, "Total cannot be negative", Toast.LENGTH_SHORT).show(); return; }
                        expense.setTotalPaid(v);
                        expense.addNewHist();
                    } catch (NumberFormatException e) { Toast.makeText(this, "Invalid total", Toast.LENGTH_SHORT).show(); return; }
                }
                if (!amountStr.isEmpty()) {
                    try {
                        double v = Double.parseDouble(amountStr);
                        if (v <= 0) { Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
                        expense.setAmount(v);
                    } catch (NumberFormatException e) { Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show(); return; }
                }
                if (!spCategory.getSelectedItem().toString().equals(expense.getCategory()))
                    expense.setCategory(spCategory.getSelectedItem().toString());
                if (!spFreq.getSelectedItem().toString().equals(expense.getFrequency()))
                    expense.setFrequency(spFreq.getSelectedItem().toString());
                if (!titleStr.isEmpty()) expense.setTitle(titleStr);
                if (!descStr.isEmpty()) expense.setNotes(descStr);

                // Apply start date change
                expense.setLastPayment(editStartDate[0]);

                boolean isNowSub = "Subscriptions".equals(expense.getCategory());
                boolean wasSub   = "Subscriptions".equals(originalCategory);
                if (wasSub != isNowSub) {
                    int i = container.indexOfChild(box);
                    container.removeView(box);
                    dialog.dismiss();
                    if (isNowSub) addSubscriptionBox(expense, i);
                    else addExpenseBox(expense, i);
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

                AlertDialog delDialog = new AlertDialog.Builder(ExpensesActivity.this).setView(delView).create();
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
                TextView tv = new TextView(this);
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

//    private void recordTransaction(String category, double amount) {
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        if (user != null) {
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            Map<String, Object> tx = new HashMap<>();
//            tx.put("amount", amount);
//            tx.put("type", "Expense");
//            tx.put("category", category);
//            tx.put("createdAt", new Date());
//            db.collection("users").document(user.getUid()).collection("transactions").add(tx);
//
//        }
//    }
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
