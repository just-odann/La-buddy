package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. AUTO-LOGIN CHECK
        // Check if the user is already logged in before even showing the login screen
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_login);

        // 2. INITIALIZE VIEWS
        final EditText emailField = findViewById(R.id.loginEmail);
        final TextInputLayout passwordLayout = findViewById(R.id.passwordInputLayout);
        final TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);
        AppCompatButton loginButton = findViewById(R.id.btnLogin);
        TextView signUpRedirect = findViewById(R.id.tvSignUpLink);

        // Password Toggle UI Logic
        if (passwordLayout != null && passwordEditText != null) {
            passwordLayout.setEndIconVisible(false);
            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    passwordLayout.setEndIconVisible(s.length() > 0);
                }
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // 3. NAVIGATION TO SIGN UP
        if (signUpRedirect != null) {
            signUpRedirect.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            });
        }

        // 4. LOGIN LOGIC
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                String email = emailField.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Welcome back, Buddy!", Toast.LENGTH_SHORT).show();
                                navigateToHome(); // Redirects to HomeActivity
                            } else {
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login Failed";
                                Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }
    }

    // HELPER METHOD FOR CLEAN NAVIGATION
    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);

        // FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK
        // This clears the "Back Stack" so the user can't press back to see the login screen again.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish(); // Destroys LoginActivity from memory
    }
}