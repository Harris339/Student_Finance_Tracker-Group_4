package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

        tvTitle.setText(expense.getTitle());
        tvAmount.setText(String.format("€%.2f", expense.getAmount()));
        tvTotalPaid.setText(String.format("Total paid: €%.2f", expense.getTotalPaid()));

        btnPay.setOnClickListener(v -> {
            expense.setTotalPaid(expense.getTotalPaid() + expense.getAmount());
            tvTotalPaid.setText(String.format("Total paid: €%.2f", expense.getTotalPaid()));
        });

        container.addView(box);
    }
}
