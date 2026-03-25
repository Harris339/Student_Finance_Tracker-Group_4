package com.example.studentfinancetracker_wk4;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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


    User currentUser;
    ExpenseList expenseList;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_fixed_expenses);

        addButton = findViewById(R.id.addButton);

        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) expenseList = currentUser.getExpenseList();

        for (FixedExpense expense : expenseList.getAllExpenses()) {
            addExpenseBox(expense);
        }

        addButton.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);

            Spinner spCategory = dialogView.findViewById(R.id.spCategory);
            Spinner spFrequency = dialogView.findViewById(R.id.spFrequency);
            EditText etTitle = dialogView.findViewById(R.id.etTitle);
            EditText etDescription = dialogView.findViewById(R.id.etDescription);
            EditText etAmount = dialogView.findViewById(R.id.etAmount);
            Button btnSave = dialogView.findViewById(R.id.btnSaveExpense);

            ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.expense_categories,
                    android.R.layout.simple_spinner_item
            );

            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spCategory.setAdapter(catAdapter);

            ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.expense_frequencies,
                    android.R.layout.simple_spinner_item
            );

            freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spFrequency.setAdapter(freqAdapter);


            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();
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

                Toast.makeText(this, expenseList.toString(), Toast.LENGTH_SHORT).show();

                dialog.dismiss();
            });
        });
    }

    private void addExpenseBox(FixedExpense expense) {
        LinearLayout container = findViewById(R.id.expenseContainer);
        View box = getLayoutInflater().inflate(R.layout.expense_item, container, false);

        TextView tvTitle = box.findViewById(R.id.tvTitle);
        TextView tvAmount = box.findViewById(R.id.tvAmount);
        Button btnPay = box.findViewById(R.id.btnPay);
        TextView tvTotalPaid = box.findViewById(R.id.tvTotalPaid);
        TextView tvLastPayment = box.findViewById(R.id.tvLastPayment);
        EditText etAmount = box.findViewById(R.id.etAmount);
        ImageButton btnMenu = box.findViewById(R.id.btnMenu);

        // Set text
        tvTitle.setText(expense.getTitle());
        tvAmount.setText(String.format("€%.2f", expense.getAmount()));
        tvTotalPaid.setText(String.format("Total paid: €%.2f", expense.getTotalPaid()));

        Date lastPayment = expense.getLastPayment();
        if (lastPayment == null) {
            tvLastPayment.setText("N/A");
        } else {
            long diffMillis = new Date().getTime() - lastPayment.getTime();
            long daysElapsed = TimeUnit.MILLISECONDS.toDays(diffMillis);
            tvLastPayment.setText(String.valueOf(daysElapsed));
        }

        // Pay button
        btnPay.setOnClickListener(v -> {
            String input = etAmount.getText().toString().trim();
            double amountToPay = input.isEmpty() ? expense.getAmount() : Double.parseDouble(input);
            expense.setTotalPaid(expense.getTotalPaid() + amountToPay);
            tvTotalPaid.setText(String.format("Total paid: €%.2f", expense.getTotalPaid()));
            tvLastPayment.setText("0");
            expense.setLastPayment(new Date());
            etAmount.setText("");
        });

        // Menu button
        btnMenu.setOnClickListener(v -> {
            // Inflate dialog layout
            View editView = getLayoutInflater().inflate(R.layout.edit_expense, null);
            ImageButton btnExit = editView.findViewById(R.id.btnExit);
            Spinner spCategory = editView.findViewById(R.id.spCategory);

            ArrayAdapter<CharSequence> catEditAdapter = ArrayAdapter.createFromResource(
                    this,R.array.expense_categories,
                    R.layout.spinner_item_white
            );

            catEditAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white     );

            spCategory.setAdapter(catEditAdapter);
            spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

            // Create AlertDialog safely
            AlertDialog dialog = new AlertDialog.Builder(ExpensesActivity.this)
                    .setView(editView)
                    .create();

            dialog.show();

            // Set exit button listener if exists
            if (btnExit != null) {
                btnExit.setOnClickListener(eve -> dialog.dismiss());
            }
        });

        container.addView(box);
    }
}