package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
public class LoginActivity extends AppCompatActivity{

    EditText emailInput, passwordInput;
    Button loginButton;
    TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        errorText = findViewById(R.id.errorText);


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

    }

        private void handleLogin() {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();

            if(TextUtils.isEmpty(email)) {
                errorText.setText("Please enter your email");
                return;
            }

            if(TextUtils.isEmpty(password)) {
                errorText.setText("Please enter your password");
                return;
            }

            if (email.equals("test") && password.equals("test")) {
                errorText.setText("");

                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

                User user = new User("Kai", "Howley", email, password);
                SessionManager.getInstance().setCurrentUser(user);

                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            } else {
                errorText.setText("Invalid email or password");
            }
        }
    }



