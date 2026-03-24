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


public class IncomeActivity extends AppCompatActivity {

    EditText amountInput;
    Button addIncomeBtn, Back;

    double totalIncome = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income);

        addIncomeBtn = findViewById(R.id.incomeConfirm);
        Back = findViewById(R.id.incomeBack);

        Back.setOnClickListener(v -> {
            Intent intent = new Intent(IncomeActivity.this, TransactionActivity.class);
            startActivity(intent);
        });

        }

    private void addIncome() {
        double amount = Double.parseDouble(amountInput.getText().toString());
        totalIncome += amount;

        Toast.makeText(this, "Income Added", Toast.LENGTH_SHORT).show();
    }
}
