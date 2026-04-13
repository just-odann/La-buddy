package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Initialize Views (Matching the XML IDs)
        final TextInputLayout passwordLayout = findViewById(R.id.passwordInputLayout);
        final TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);
        AppCompatButton loginButton = findViewById(R.id.btnLogin);
        TextView signUpRedirect = findViewById(R.id.tvSignUpLink);

        // 2. Password Toggle Visibility Logic
        if (passwordLayout != null && passwordEditText != null) {
            // Hide eye icon until user starts typing
            passwordLayout.setEndIconVisible(false);

            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    passwordLayout.setEndIconVisible(s.length() > 0);
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // 3. Navigation to Sign Up (MainActivity)
        if (signUpRedirect != null) {
            signUpRedirect.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                // Optional: finish() if you don't want the user to come back to login via 'back' button
                finish();
            });
        }

        // 4. Handle Login Button Click
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                // Add your login logic here (e.g., Firebase or API call)
            });
        }
    }
}