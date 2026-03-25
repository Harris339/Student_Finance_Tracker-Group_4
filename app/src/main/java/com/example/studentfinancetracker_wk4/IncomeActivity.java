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


public class IncomeActivity extends TransactionActivity {

    EditText amountInput, categoryInput;
    Button confirmBtn, Back;

    double totalIncome = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income);

        amountInput = findViewById(R.id.IamountInput);
        categoryInput = findViewById(R.id.IcategoryInput);
        confirmBtn = findViewById(R.id.incomeConfirm);
        Back = findViewById(R.id.incomeBack);

        confirmBtn.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString();
            double amount = Double.parseDouble(amountStr);
            String category = categoryInput.getText().toString();
            addIncome(category, amount);
            finish();
        });

        Back.setOnClickListener(v -> finish());
        }

    private void addIncome(String category, double amount) {
        totalIncome += amount;

        transaction(category, amount, "Income");

        Toast.makeText(this, "Income Added", Toast.LENGTH_SHORT).show();
    }
}
