package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText; // Check if your XML uses EditText or TextInputEditText
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // 2. Initialize Views
        final EditText emailField = findViewById(R.id.loginEmail); // Ensure this ID matches activity_login.xml
        final TextInputLayout passwordLayout = findViewById(R.id.passwordInputLayout);
        final TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);
        AppCompatButton loginButton = findViewById(R.id.btnLogin);
        TextView signUpRedirect = findViewById(R.id.tvSignUpLink);

        // Password Toggle Logic (Keeping your classmate's UI code)
        if (passwordLayout != null && passwordEditText != null) {
            passwordLayout.setEndIconVisible(false);
            passwordEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    passwordLayout.setEndIconVisible(s.length() > 0);
                }
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        // 3. Navigation to Sign Up
        if (signUpRedirect != null) {
            signUpRedirect.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            });
        }

        // 4. THE LOGIN LOGIC
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                String email = emailField.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Authenticate with Firebase
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                // Login successful!
                                Toast.makeText(LoginActivity.this, "Welcome back, Buddy!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            } else {
                                // Login failed
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login Failed";
                                Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }
    }
}