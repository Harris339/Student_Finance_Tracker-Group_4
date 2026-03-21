package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class ChartActivity extends AppCompatActivity{

        Button previousBtn, nextBtn;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_chart);

            previousBtn = findViewById(R.id.previousBtn);
            nextBtn = findViewById(R.id.nextBtn);
        }

    }
