package com.example.studentfinancetracker_wk4;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
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

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ExpensesActivity extends AppCompatActivity {
    Button addButton;
    ImageButton backbtn;


    User currentUser;
    ExpenseList expenseList;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_fixed_expenses);

        addButton = findViewById(R.id.addButton);
        backbtn = findViewById(R.id.btnBack);

        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) expenseList = currentUser.getExpenseList();

        if(expenseList != null) {
            for (FixedExpense expense : expenseList.getAllExpenses()) {
                addExpenseBox(expense);
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

            ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.expense_categories,
                    R.layout.spinner_item_white
            );

            catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);

            spCategory.setAdapter(catAdapter);
            spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

            ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.expense_frequencies,
                    R.layout.spinner_item_white
            );

            freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);

            spFrequency.setAdapter(freqAdapter);
            spFrequency.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));


            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

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

                double amount = Double.parseDouble(amountStr);

                FixedExpense expense = new FixedExpense(category, title, amount, frequency, description);

                currentUser.addExpense(expense);

                addExpenseBox(expense);

                dialog.dismiss();
            });

            btnExit.setOnClickListener(event -> dialog.dismiss());
        });

        backbtn.setOnClickListener(v -> finish());
    }

    private void addExpenseBox(FixedExpense expense) {
        LinearLayout container = findViewById(R.id.expenseContainer);
        View box = getLayoutInflater().inflate(R.layout.expense_item, container, false);

        TextView tvTitle = box.findViewById(R.id.tvTitle);
        TextView tvAmount = box.findViewById(R.id.tvAmount);
        Button btnPay = box.findViewById(R.id.btnPay);
        TextView tvTotalPaid = box.findViewById(R.id.tvTotalPaid);
        TextView tvNextPayment = box.findViewById(R.id.tvNextPayment);
        EditText etAmount = box.findViewById(R.id.etAmount);
        ImageButton btnMenu = box.findViewById(R.id.btnMenu);
        TextView tvFreq = box.findViewById(R.id.tvFreq);

        // Set text
        tvTitle.setText(expense.getTitle());
        tvAmount.setText(String.format("€%.2f", expense.getAmount()));
        tvTotalPaid.setText(String.format("€%.2f", expense.getTotalPaid()));
        tvFreq.setText(expense.getFrequency());

        tvNextPayment.setText(String.valueOf(expense.getDaysUntilNextPayment()));

        // Pay button
        btnPay.setOnClickListener(v -> {
            String input = etAmount.getText().toString().trim();
            double amountToPay = input.isEmpty() ? expense.getAmount() : Double.parseDouble(input);
            expense.setTotalPaid(expense.getTotalPaid() + amountToPay);
            expense.addNewHist();
            System.out.println(expense.getAllHistory());
            tvTotalPaid.setText(String.format("€%.2f", expense.getTotalPaid()));
            expense.setLastPayment(new Date());
            etAmount.setText("");
            tvNextPayment.setText(String.valueOf(expense.getDaysUntilNextPayment()));
        });

        // Menu button
        btnMenu.setOnClickListener(v -> {
            View editView = getLayoutInflater().inflate(R.layout.edit_expense, null);

            TextView tvEditTitle = editView.findViewById(R.id.tvTitle);
            TextView tvEditTotalAmount = editView.findViewById(R.id.tvEuroAmount);

            ImageButton btnExit = editView.findViewById(R.id.btnExit);
            ImageButton btnUndo = editView.findViewById(R.id.btnUndo);
            ImageButton btnRedo = editView.findViewById(R.id.btnRedo);
            EditText editAmount = editView.findViewById(R.id.editAmount);
            Spinner spCategory = editView.findViewById(R.id.spCategory);
            Spinner spFreq = editView.findViewById(R.id.spFreq);
            EditText etTitle = editView.findViewById(R.id.etTitle);
            EditText etDesc = editView.findViewById(R.id.etDesc);
            EditText etEditAmount = editView.findViewById(R.id.etAmount);
            Button btnSet = editView.findViewById(R.id.btnSet);
            Button btnDeleteDialog = editView.findViewById(R.id.btnDelete);

            tvEditTitle.setText(expense.getTitle());
            tvEditTotalAmount.setText(String.format("%.2f", expense.getTotalPaid()));

            ArrayAdapter<CharSequence> catEditAdapter = ArrayAdapter.createFromResource(
                    this,R.array.expense_categories,
                    R.layout.spinner_item_white
            );

            catEditAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);

            spCategory.setAdapter(catEditAdapter);
            spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

            int catPos = catEditAdapter.getPosition(expense.getCategory());
            spCategory.setSelection(catPos);

            ArrayAdapter<CharSequence> freqEditAdapter =  ArrayAdapter.createFromResource(
                    this, R.array.expense_frequencies,
                    R.layout.spinner_item_white
            );

            freqEditAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);

            spFreq.setAdapter(freqEditAdapter);
            spFreq.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

            int freqPos = freqEditAdapter.getPosition(expense.getFrequency());
            spFreq.setSelection(freqPos);

            // Create AlertDialog safely
            AlertDialog dialog = new AlertDialog.Builder(ExpensesActivity.this)
                    .setView(editView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            dialog.show();

            // Set exit button listener if exists
            if (btnExit != null) {
                btnExit.setOnClickListener(eve -> dialog.dismiss());
            }

            btnUndo.setOnClickListener(event -> {
                expense.setTotalPaid(expense.getHistAtCurIndex(-1));

                tvEditTotalAmount.setText(String.format("%.2f", expense.getTotalPaid()));

                if(expense.getPos() < 0) {
                    btnUndo.setImageTintList(
                            ColorStateList.valueOf(Color.parseColor("#B3B1B1"))
                    );
                } else {
                    btnUndo.setImageTintList(
                            ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                    );
                }

                if(expense.getPos() >= expense.getPaymentHist().size()) {
                    btnRedo.setImageTintList(
                            ColorStateList.valueOf(Color.parseColor("#B3B1B1"))
                    );
                } else {
                    btnRedo.setImageTintList(
                            ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                    );
                }
            });


            btnRedo.setOnClickListener(event -> {
                expense.setTotalPaid(expense.getHistAtCurIndex(1));

                tvEditTotalAmount.setText(String.format("%.2f", expense.getTotalPaid()));
            });

            btnSet.setOnClickListener(event -> {
                String titleStr = etTitle.getText().toString();
                String descStr = etDesc.getText().toString();
                String amountStr = etEditAmount.getText().toString();
                String totalAmntStr = editAmount.getText().toString();

                if(!totalAmntStr.isEmpty()) {
                    expense.setTotalPaid(Double.parseDouble(totalAmntStr));
                }

                if(!spCategory.getSelectedItem().toString().equals(expense.getCategory())) {
                    expense.setCategory(spCategory.getSelectedItem().toString());
                }

                if(!spFreq.getSelectedItem().toString().equals(expense.getFrequency())) {
                    expense.setFrequency(spFreq.getSelectedItem().toString());
                }

                if(!titleStr.isEmpty()) {
                    expense.setTitle(titleStr);
                }

                if(!descStr.isEmpty()){
                    expense.setNotes(descStr);
                }

                if(!amountStr.isEmpty()) {
                    expense.setAmount(Double.parseDouble(amountStr));
                }

                tvTitle.setText(expense.getTitle());
                tvAmount.setText(String.format("€%.2f", expense.getAmount()));
                tvTotalPaid.setText(String.format("€%.2f", expense.getTotalPaid()));
                tvFreq.setText(expense.getFrequency());
                tvNextPayment.setText(String.valueOf(expense.getDaysUntilNextPayment()));

                dialog.dismiss();
            });

            btnDeleteDialog.setOnClickListener(event -> {
                View delView = getLayoutInflater().inflate(R.layout.delete_expense_dialog, null);

                Button btnCancel = delView.findViewById(R.id.btnCancel);
                Button btnDelete = delView.findViewById(R.id.btnDelete);

                AlertDialog delDialog = new AlertDialog.Builder(ExpensesActivity.this)
                        .setView(delView)
                        .create();

                if (delDialog.getWindow() != null) {
                    delDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

                delDialog.show();

                btnCancel.setOnClickListener(event1 -> {
                    delDialog.dismiss();
                    dialog.dismiss();
                });

                btnDelete.setOnClickListener(event1 -> {
                    expenseList.removeExpense(expense);
                    container.removeView(box);
                    delDialog.dismiss();
                    dialog.dismiss();
                });
            });
        });

        container.addView(box);
    }
}