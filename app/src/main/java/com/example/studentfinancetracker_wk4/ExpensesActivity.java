package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ExpensesActivity extends AppCompatActivity {
    Button addButton;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_expenses);

        addButton = findViewById(R.id.addButton);

        addButton.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);

            EditText etTitle = dialogView.findViewById(R.id.etTitle);
            EditText etDescription = dialogView.findViewById(R.id.etDescription);
            EditText etAmount = dialogView.findViewById(R.id.etAmount);
            Button btnSave = dialogView.findViewById(R.id.btnSaveExpense);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            btnSave.setOnClickListener(view -> {
                String title = etTitle.getText().toString();
                String description = etDescription.getText().toString();
                String amountStr = etAmount.getText().toString();

                if (title.isEmpty() || description.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);


            });
        });
    }
}
