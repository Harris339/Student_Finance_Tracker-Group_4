package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button transactionBtn = findViewById(R.id.transactionBtn);
        Button mainBtn = findViewById(R.id.mainBtn);
        Button expensesBtn = findViewById(R.id.expenseBtn);
        Button chartBtn = findViewById(R.id.chartBtn);

        transactionBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TransactionActivity.class);
            startActivity(intent);
        });

        mainBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
        });

        expensesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TrackersActivity.class);
            startActivity(intent);
        });

        chartBtn.setOnClickListener(v ->{
            Intent intent = new Intent(HomeActivity.this, ChartActivity.class);
            startActivity(intent);
        });

    }
}