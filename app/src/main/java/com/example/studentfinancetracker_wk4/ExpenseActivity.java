package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;


public class ExpenseActivity extends TransactionActivity {

    EditText amountInput;
    Button addExpenseBtn, Back;

    double totalExpenses = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        addExpenseBtn = findViewById(R.id.expenseConfirm);
        Back = findViewById(R.id.expenseBack);

        Back.setOnClickListener(v -> finish());

    }

    private void addExpense() {
        double amount = Double.parseDouble(amountInput.getText().toString());
        totalExpenses += amount;

        Toast.makeText(this, "Expense Added", Toast.LENGTH_SHORT).show();
    }
}
