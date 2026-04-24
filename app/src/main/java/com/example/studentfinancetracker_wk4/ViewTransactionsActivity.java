package com.example.studentfinancetracker_wk4;

import android.content.Intent;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class ViewTransactionsActivity extends AppCompatActivity {

    private static final String TAG = "viewTransactionsActivity";
    TextView textView;
    ImageButton backbtn;
    Button filterbtn;
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
                        View dialogView = getLayoutInflater().inflate(R.layout.filter_transaction_search, null);

                        Spinner spCategory = dialogView.findViewById(R.id.spCategory);
                        Spinner spFrequency = dialogView.findViewById(R.id.spFrequency);
//                        Button btnSave = dialogView.findViewById(R.id.btnSaveExpense);
                        ImageButton btnExit = dialogView.findViewById(R.id.btnExit);

                        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                                this,
                                R.array.expense_categories,
                                R.layout.spinner_item_white
                        );

                        catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);

                        spCategory.setAdapter(catAdapter);
                        //spCategory.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

                        ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                                this,
                                R.array.expense_frequencies,
                                R.layout.spinner_item_white
                        );

                        freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);

                        spFrequency.setAdapter(freqAdapter);
                        //spFrequency.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2C2C")));

                        AlertDialog dialog = new AlertDialog.Builder(this)
                            .setView(dialogView)
                            .create();

                        dialog.show();

                btnExit.setOnClickListener(event -> dialog.dismiss());
                        });

            backbtn.setOnClickListener(v -> finish());

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

                                builder.append("Type: ").append(type != null ? type : "N/A").append("\n");
                                builder.append("Category: ").append(category != null ? category : "N/A").append("\n");
                                builder.append("Amount: €").append(amount != null ? amount : 0).append("\n");
                                builder.append("Date: ").append(date != null ? date : "N/A").append("\n");
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
