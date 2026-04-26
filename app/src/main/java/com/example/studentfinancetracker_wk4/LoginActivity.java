package com.example.studentfinancetracker_wk4;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity{

    private static final String TAG = "loginActivity";
    FirebaseAuth mAuth;
    EditText emailInput, passwordInput;
    Button loginButton, createAccount;
    TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        createAccount = findViewById(R.id.createAccount);
        errorText = findViewById(R.id.errorText);


//        loginButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                handleLogin();
//            }
//        });

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            User user = new User(email, password);
            SessionManager.getInstance().setCurrentUser(user);

            if (email.equals("test") && password.equals("test")) {
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        createAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, createUserActivity.class);
            startActivity(intent);
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

                User user = new User(email, password);
                SessionManager.getInstance().setCurrentUser(user);

                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            } else {
                errorText.setText("Invalid email or password");
            }
        }
    }



