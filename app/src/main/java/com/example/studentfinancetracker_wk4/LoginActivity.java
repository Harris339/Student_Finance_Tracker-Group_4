package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    FirebaseAuth mAuth;
    EditText emailInput, passwordInput;
    Button loginButton, createAccount;
    TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Auto-login: Firebase persists sessions across restarts
        FirebaseUser existing = mAuth.getCurrentUser();
        if (existing != null) {
            User user = new User(existing.getEmail(), "");
            SessionManager.getInstance().setCurrentUser(user);
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        emailInput    = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton   = findViewById(R.id.loginButton);
        createAccount = findViewById(R.id.createAccount);
        errorText     = findViewById(R.id.errorText);

        loginButton.setOnClickListener(v -> {
            String email    = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            errorText.setText("");

            if (email.isEmpty() || password.isEmpty()) {
                errorText.setText("Please enter your email and password");
                return;
            }

            // Test credentials shortcut
            if (email.equals("test") && password.equals("test")) {
                User user = new User(email, password);
                SessionManager.getInstance().setCurrentUser(user);
                goToMain();
                return;
            }

            loginButton.setEnabled(false);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            loginButton.setEnabled(true);
                            if (task.isSuccessful()) {
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                User user = new User(firebaseUser.getEmail(), "");
                                SessionManager.getInstance().setCurrentUser(user);
                                goToMain();
                            } else {
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                errorText.setText("Incorrect email or password");
                            }
                        }
                    });
        });

        createAccount.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, createUserActivity.class)));
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
