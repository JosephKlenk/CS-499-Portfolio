package com.josephklenk.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialButton createAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> handleLogin());
        createAccountButton.setOnClickListener(v -> handleCreateAccount());
    }

    private void handleLogin() {
        String username = usernameEditText.getText() != null ?
                usernameEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ?
                passwordEditText.getText().toString().trim() : "";

        if (validateInput(username, password)) {
            if (dbHelper.checkUser(username, password)) {
                long userId = dbHelper.getUserId(username);
                launchWeightTracker(userId);
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleCreateAccount() {
        String username = usernameEditText.getText() != null ?
                usernameEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ?
                passwordEditText.getText().toString().trim() : "";

        if (validateInput(username, password)) {
            long userId = dbHelper.addUser(username, password);
            if (userId != -1) {
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
                launchWeightTracker(userId);
            } else {
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validateInput(String username, String password) {
        if (username.isEmpty()) {
            usernameEditText.setError("Username required");
            return false;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password required");
            return false;
        }
        return true;
    }

    private void launchWeightTracker(long userId) {
        Intent intent = new Intent(this, WeightTrackerActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }
}