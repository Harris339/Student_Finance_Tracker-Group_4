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

public class TransactionActivity extends AppCompatActivity {

    Button addIncomeBtn, addExpenseBtn;

    double totalIncome = 0;
    double totalExpenses = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        addIncomeBtn = findViewById(R.id.addIncomeBtn);
        addExpenseBtn = findViewById(R.id.addExpenseBtn);

        addIncomeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, IncomeActivity.class);
            startActivity(intent);
        });

        addExpenseBtn.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, ExpenseActivity.class);
            startActivity(intent);
        });

    }


    protected void transaction(String category, double amount, String type) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("type", type);
        data.put("category", category);

        db.collection("transactions")
                .add(data)
                .addOnSuccessListener(doc ->
                        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show()
                );
    }
}