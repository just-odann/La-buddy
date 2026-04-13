package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Initialize Password Toggle Views
        final TextInputLayout passwordLayout = findViewById(R.id.passwordInputLayout);
        final TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);

        // 2. Initialize Navigation View (Check your XML ID for this)
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        // Password Icon Visibility Logic
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

        // 3. Navigation Logic: Sign Up -> Log In
        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                // We don't usually call finish() here so the user can go 'back' to Sign Up
            });
        }
    }
}