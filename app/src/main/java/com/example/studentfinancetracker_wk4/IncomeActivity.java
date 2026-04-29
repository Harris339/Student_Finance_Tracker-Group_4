package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncomeActivity extends TransactionActivity {

    EditText amountInput, categoryInput;
    Button confirmBtn, Back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income);

        amountInput = findViewById(R.id.IamountInput);
        categoryInput = findViewById(R.id.IcategoryInput);
        confirmBtn = findViewById(R.id.incomeConfirm);
        Back = findViewById(R.id.incomeBack);

        confirmBtn.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) { Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show(); return; }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            String category = categoryInput.getText().toString().trim();
            addIncome(category.isEmpty() ? "General" : category, amount);
            finish();
        });

        Back.setOnClickListener(v -> finish());
    }

    private void addIncome(String category, double amount) {
        // Distribute income across pools
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            user.getPercDivider().allocateIncome(amount);
            saveCategories(user.getPercDivider());
        }

        SmartPercentageDivider divider =
                SessionManager.getInstance().getCurrentUser().getPercDivider();

        divider.allocateIncome(amount);

        saveDivider(divider);

        transaction(category, amount, "Income");
        Toast.makeText(this, "Income added and pools updated", Toast.LENGTH_SHORT).show();
    }

    static void saveCategories(SmartPercentageDivider divider) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        List<Map<String, Object>> items = new ArrayList<>();
        for (Category c : divider.getAllDividers()) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", c.getCategory());
            map.put("percentage", c.getPercentage());
            map.put("balance", c.getBalance());
            map.put("lastAllocation", c.getLastAllocation());
            items.add(map);
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("items", items);

        FirebaseFirestore.getInstance()
                .collection("users").document(firebaseUser.getUid())
                .collection("data").document("categories")
                .set(doc);
    }

    private void saveDivider(SmartPercentageDivider divider) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (Category c : divider.getAllDividers()) {

            Map<String, Object> data = new HashMap<>();
            data.put("percentage", c.getPercentage());
            data.put("balance", c.getBalance());
            data.put("lastAllocation", c.getLastAllocation());

            db.collection("users")
                    .document(user.getUid())
                    .collection("percDividers")
                    .document(c.getCategory())
                    .set(data);
        }
    }
}
