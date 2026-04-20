package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_login);

        final EditText emailField = findViewById(R.id.loginEmail);
        final TextInputLayout passwordLayout = findViewById(R.id.passwordInputLayout);
        final TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);
        AppCompatButton loginButton = findViewById(R.id.btnLogin);
        TextView signUpRedirect = findViewById(R.id.tvSignUpLink);

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

        if (signUpRedirect != null) {
            signUpRedirect.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            });
        }

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
                                navigateToHome();
                            } else {
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login Failed";
                                Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }

        // GOOGLE SIGN-IN PLACEHOLDER
        View btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> {
                Toast.makeText(LoginActivity.this, "Google Sign-In is currently in development!", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}