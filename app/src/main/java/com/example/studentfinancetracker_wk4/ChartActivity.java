package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class ChartActivity extends AppCompatActivity {

    Button previousBtn, nextBtn, btnEditAll, btnAddCategory;;
    LinearLayout container;

    SmartPercentageDivider divider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);


        container = findViewById(R.id.categoryContainer);
        btnEditAll = findViewById(R.id.btnEditAll);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        previousBtn = findViewById(R.id.previousBtn);
        nextBtn = findViewById(R.id.nextBtn);

        btnEditAll.setOnClickListener(v -> showEditAllDialog());
        btnAddCategory.setOnClickListener(v -> showAddDividerDialog());

        divider = new SmartPercentageDivider();

        renderCategories(divider);
    }

    public void renderCategories(SmartPercentageDivider divider) {
        container.removeAllViews();

        for (Category c : divider.getAllDividers()) {
            View row = getLayoutInflater().inflate(R.layout.divider_item, container, false);

            TextView name = row.findViewById(R.id.tvCategoryName);
            TextView percent = row.findViewById(R.id.tvPercentage);
            TextView last = row.findViewById(R.id.tvLastAllocation);
            TextView total = row.findViewById(R.id.tvTotalAllocated);

            name.setText(c.getCategory());
            percent.setText(String.format("%.0f%%", c.getPercentage()));
            last.setText(String.format("Last: €%.2f", c.getLastAllocation()));
            total.setText(String.format("Total: €%.2f", c.getTotalAllocatedToDate()));

            container.addView(row);
        }
    }

    private void showEditAllDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_divider, null);
        LinearLayout editContainer = view.findViewById(R.id.editContainer);
        Button btnSave = view.findViewById(R.id.btnSave);

        java.util.List<EditText> inputs = new java.util.ArrayList<>();

        for (Category c : divider.getAllDividers()) {
            View row = getLayoutInflater().inflate(R.layout.edit_percentage_item, editContainer, false);

            TextView name = row.findViewById(R.id.tvName);
            EditText percent = row.findViewById(R.id.etPercent);

            name.setText(c.getCategory());
            percent.setText(String.valueOf(c.getPercentage()));

            inputs.add(percent);
            editContainer.addView(row);
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnSave.setOnClickListener(v -> {
            java.util.List<Double> newValues = new java.util.ArrayList<>();

            for (EditText et : inputs) {
                String val = et.getText().toString();
                if (val.isEmpty()) return;

                newValues.add(Double.parseDouble(val));
            }

            if (!divider.updateAllPercentages(newValues)) {
                Toast.makeText(this, "Total must equal 100%", Toast.LENGTH_SHORT).show();
                return;
            }

            renderCategories(divider);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showAddDividerDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_divider, null);

        EditText etName = view.findViewById(R.id.etName);
        EditText etPercent = view.findViewById(R.id.etPercent);
        Button btnAdd = view.findViewById(R.id.btnAdd);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String percentStr = etPercent.getText().toString().trim();

            if (name.isEmpty() || percentStr.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double percent = Double.parseDouble(percentStr);

            divider.addDivider(name, percent);

            renderCategories(divider);
            dialog.dismiss();
        });

        dialog.show();
    }
}