package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewTransactionsActivity extends AppCompatActivity {

    private static final String TAG = "viewTransactionsActivity";
    String categoryFilter, typeFilter;
    TextView textView;
    ImageButton backbtn;
    Button filterbtn, setBtn;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_transactions);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {

            String userId = user.getUid();

            textView = findViewById(R.id.View);
            backbtn = findViewById(R.id.btnBack);
            filterbtn = findViewById(R.id.filterBtn);

            filterbtn.setOnClickListener(v -> {

                View dialogView = getLayoutInflater()
                        .inflate(R.layout.filter_transaction_search, null);

                Spinner spinnerCat = dialogView.findViewById(R.id.spCategory);
                Spinner spinnerType = dialogView.findViewById(R.id.spType);
                ImageButton btnExit = dialogView.findViewById(R.id.btnExit);
                Button setBtn = dialogView.findViewById(R.id.setBtn);

                // Lists
                List<String> categories = new ArrayList<>();
                List<String> types = new ArrayList<>();

                // Adapters
                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                        this,
                        R.layout.spinner_item_white,
                        categories
                );
                catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
                spinnerCat.setAdapter(catAdapter);

                ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                        this,
                        R.layout.spinner_item_white,
                        types
                );
                typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
                spinnerType.setAdapter(typeAdapter);

                // Styling
                spinnerCat.setPopupBackgroundDrawable(
                        new ColorDrawable(Color.parseColor("#2C2C2C"))
                );
                spinnerType.setPopupBackgroundDrawable(
                        new ColorDrawable(Color.parseColor("#2C2C2C"))
                );

                // Load Firestore data
                db.collection("users")
                        .document(userId)
                        .collection("transactions")
                        .addSnapshotListener((snapshots, e) -> {

                            if (e != null || snapshots == null) return;

                            categories.clear();
                            types.clear();

                            // 👉 ADD "ANY"
                            categories.add("Any");
                            types.add("Any");

                            for (DocumentSnapshot doc : snapshots) {

                                String category = doc.getString("category");
                                String type = doc.getString("type");

                                if (category != null && !categories.contains(category)) {
                                    categories.add(category);
                                }

                                if (type != null && !types.contains(type)) {
                                    types.add(type);
                                }
                            }

                            catAdapter.notifyDataSetChanged();
                            typeAdapter.notifyDataSetChanged();
                        });

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();

                dialog.show();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }

                setBtn.setOnClickListener(view -> {

                    String categoryFilter = spinnerCat.getSelectedItem().toString();
                    String typeFilter = spinnerType.getSelectedItem().toString();

                    Query query = db.collection("users")
                            .document(userId)
                            .collection("transactions");

                    if (!categoryFilter.equals("Any")) {
                        query = query.whereEqualTo("category", categoryFilter);
                    }

                    if (!typeFilter.equals("Any")) {
                        query = query.whereEqualTo("type", typeFilter);
                    }

                    query.get().addOnSuccessListener(queryDocumentSnapshots -> {

                        StringBuilder builder = new StringBuilder();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            String type = document.getString("type");
                            String category = document.getString("category");
                            Double amount = document.getDouble("amount");
                            String date = document.getString("date");

                            builder.append("Type: ")
                                    .append(type != null ? type : "N/A").append("\n");

                            builder.append("Category: ")
                                    .append(category != null ? category : "N/A").append("\n");

                            builder.append("Amount: €")
                                    .append(amount != null ? amount : 0).append("\n");

                            builder.append("Date: ")
                                    .append(date != null ? date : "N/A").append("\n");

                            builder.append("----------------------\n\n");
                        }

                        textView.setText(builder.toString());
                    });

                    dialog.dismiss();
                });

                btnExit.setOnClickListener(v1 -> dialog.dismiss());
            });

            backbtn.setOnClickListener(v -> finish());

            // Initial load
            db.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .get()
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            StringBuilder builder = new StringBuilder();

                            for (QueryDocumentSnapshot document : task.getResult()) {

                                String type = document.getString("type");
                                String category = document.getString("category");
                                Double amount = document.getDouble("amount");
                                String date = document.getString("date");

                                builder.append("Type: ")
                                        .append(type != null ? type : "N/A").append("\n");

                                builder.append("Category: ")
                                        .append(category != null ? category : "N/A").append("\n");

                                builder.append("Amount: €")
                                        .append(amount != null ? amount : 0).append("\n");

                                builder.append("Date: ")
                                        .append(date != null ? date : "N/A").append("\n");

                                builder.append("----------------------\n\n");
                            }

                            textView.setText(builder.toString());

                        } else {
                            Log.d(TAG, "Error getting documents", task.getException());
                        }
                    });

        } else {
            Log.d(TAG, "No user logged in");
        }
    }
}
