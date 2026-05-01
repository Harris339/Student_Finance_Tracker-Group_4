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

    EditText amountInput, categoryInput;
    Button confirmBtn, Back;

    double totalExpenses = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        amountInput = findViewById(R.id.EamountInput);
        categoryInput = findViewById(R.id.EcategoryInput);
        confirmBtn = findViewById(R.id.expenseConfirm);
        Back = findViewById(R.id.expenseBack);

        confirmBtn.setOnClickListener(v ->{

            String category = categoryInput.getText().toString();
            String amntStr = amountInput.getText().toString();
            double amount = Double.parseDouble(amntStr);

            addExpense(category, amount);
            finish();
        });

        Back.setOnClickListener(v -> finish());

    }

    private void addExpense(String category, double amount) {
        totalExpenses += amount;

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            user.getPercDivider().deductExpense(category, amount);
            IncomeActivity.saveDivider(user.getPercDivider());
        }

        transaction(category, amount, "Expense");
        Toast.makeText(this, "Expense Added", Toast.LENGTH_SHORT).show();
    }
}
