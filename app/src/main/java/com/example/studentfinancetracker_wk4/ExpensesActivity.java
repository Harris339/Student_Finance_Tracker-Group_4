package com.example.studentfinancetracker_wk4;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ExpensesActivity extends AppCompatActivity {
    Button addButton;
    Spinner spCategory;

    User currentUser;
    ExpenseList expenseList;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_expenses);

        addButton = findViewById(R.id.addButton);

        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) expenseList = currentUser.getExpenseList();

        addButton.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);

            EditText etTitle = dialogView.findViewById(R.id.etTitle);
            EditText etDescription = dialogView.findViewById(R.id.etDescription);
            EditText etAmount = dialogView.findViewById(R.id.etAmount);
            EditText etDate = dialogView.findViewById(R.id.etDate);
            Button btnSave = dialogView.findViewById(R.id.btnSaveExpense);

            spCategory = dialogView.findViewById(R.id.spCategory);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.expense_categories,
                    android.R.layout.simple_spinner_item
            );

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spCategory.setAdapter(adapter);

            etDate.setOnClickListener(ve -> {
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePicker = new DatePickerDialog(this, (view, y, m, d) -> {
                    m += 1;
                    String dateStr = String.format("%02d-%02d-%04d", d, m, y);
                    etDate.setText(dateStr);
                }, year, month, day);

                datePicker.show();
            });

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();
            dialog.show();

            btnSave.setOnClickListener(view -> {
                String title = etTitle.getText().toString();
                String description = etDescription.getText().toString();
                String amountStr = etAmount.getText().toString();
                String category = spCategory.getSelectedItem().toString();
                String dateStr = etDate.getText().toString();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                Date date = null;

                try {
                    date = sdf.parse(dateStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
                    return;
                }


                if (title.isEmpty() || description.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);

                currentUser.addExpense(new FixedExpense(category, title, amount, date, description));

                Toast.makeText(this, expenseList.toString(), Toast.LENGTH_SHORT).show();

                Log.d("ExpenseDebug", "Added expense: " + expenseList.toString());

                dialog.dismiss();
            });
        });
    }
}
