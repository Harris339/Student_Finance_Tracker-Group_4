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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TransactionActivity extends AppCompatActivity {

    Button viewBtn, addIncomeBtn, addExpenseBtn, Backbtn;

    double totalIncome = 0;
    double totalExpenses = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        viewBtn = findViewById(R.id.transactions);
        addIncomeBtn = findViewById(R.id.addIncomeBtn);
        addExpenseBtn = findViewById(R.id.addExpenseBtn);
        Backbtn = findViewById(R.id.BackBtn);


        viewBtn.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, ViewTransactionsActivity.class);
            startActivity(intent);
        });
        addIncomeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, IncomeActivity.class);
            startActivity(intent);
        });

        addExpenseBtn.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, ExpenseActivity.class);
            startActivity(intent);
        });

        Backbtn.setOnClickListener(v -> finish());

    }


    protected void transaction(String category, double amount, String type) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Map<String, Object> transaction = new HashMap<>();
            transaction.put("amount", amount);
            transaction.put("type", type);
            transaction.put("category", category);
            transaction.put("createdAt", new Date());

            db.collection("users")
                    .document(user.getUid())
                    .collection("transactions")
                    .add(transaction);
        }
    }
}