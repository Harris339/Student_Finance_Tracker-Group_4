package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TransactionActivity extends AppCompatActivity {

    EditText amountInput;
    Button addIncomeBtn, addExpenseBtn;

    double totalIncome = 0;
    double totalExpenses = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        amountInput = findViewById(R.id.amountInput);

        addIncomeBtn = findViewById(R.id.addIncomeBtn);
        addExpenseBtn = findViewById(R.id.addExpenseBtn);

        addIncomeBtn.setOnClickListener(v -> addIncome());
        addExpenseBtn.setOnClickListener(v -> addExpense());
    }

    private void addIncome() {
        double amount = Double.parseDouble(amountInput.getText().toString());
        totalIncome += amount;

        Toast.makeText(this, "Income Added", Toast.LENGTH_SHORT).show();
    }

    private void addExpense() {
        double amount = Double.parseDouble(amountInput.getText().toString());
        totalExpenses += amount;

        Toast.makeText(this, "Expense Added", Toast.LENGTH_SHORT).show();
    }
}