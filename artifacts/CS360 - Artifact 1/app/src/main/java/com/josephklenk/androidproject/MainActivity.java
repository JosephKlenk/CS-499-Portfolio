package com.josephklenk.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    // View components
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialButton createAccountButton;
    
    // Model layer
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeDatabase();
        initializeViews();
        setupEventListeners();
        
        Log.d(TAG, "MainActivity initialized successfully");
    }

    /**
     * Initializes the database helper
     */
    private void initializeDatabase() {
        try {
            dbHelper = new DatabaseHelper(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database", e);
            showError("Database initialization failed. Please restart the app.");
        }
    }

    /**
     * Initializes UI components
     */
    private void initializeViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);
    }

    /**
     * Sets up event listeners for UI components
     */
    private void setupEventListeners() {
        loginButton.setOnClickListener(v -> handleLogin());
        createAccountButton.setOnClickListener(v -> handleCreateAccount());
    }

    /**
     * Handles user login attempt with validation
     */
    private void handleLogin() {
        String username = getInputText(usernameEditText);
        String password = getInputText(passwordEditText);

        // Clear any previous errors
        clearFieldErrors();

        // Validate input
        if (!validateLoginInput(username, password)) {
            return;
        }

        try {
            if (dbHelper.checkUser(username, password)) {
                long userId = dbHelper.getUserId(username);
                if (userId != -1) {
                    Log.d(TAG, "User logged in successfully: " + username);
                    navigateToWeightTracker(userId);
                } else {
                    showError("Login failed. Please try again.");
                }
            } else {
                Log.w(TAG, "Authentication failed for user: " + username);
                showError("Invalid username or password");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during login", e);
            showError("Login failed. Please try again.");
        }
    }

    /**
     * Handles user account creation with validation
     */
    private void handleCreateAccount() {
        String username = getInputText(usernameEditText);
        String password = getInputText(passwordEditText);

        // Clear any previous errors
        clearFieldErrors();

        // Validate input
        if (!validateRegistrationInput(username, password)) {
            return;
        }

        try {
            long userId = dbHelper.addUser(username, password);
            if (userId != -1) {
                Log.d(TAG, "Account created successfully for user: " + username);
                showSuccess("Account created successfully!");
                navigateToWeightTracker(userId);
            } else {
                Log.w(TAG, "Account creation failed for user: " + username);
                showError("Username already exists or account creation failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating account", e);
            showError("Account creation failed. Please try again.");
        }
    }

    /**
     * Validates login input
     * @param username Username to validate
     * @param password Password to validate
     * @return true if valid, false otherwise
     */
    private boolean validateLoginInput(String username, String password) {
        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            usernameEditText.requestFocus();
            return false;
        }
        
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return false;
        }
        
        return true;
    }

    /**
     * Validates registration input with additional requirements
     * @param username Username to validate
     * @param password Password to validate
     * @return true if valid, false otherwise
     */
    private boolean validateRegistrationInput(String username, String password) {
        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            usernameEditText.requestFocus();
            return false;
        }
        
        if (username.length() < 3) {
            usernameEditText.setError("Username must be at least 3 characters");
            usernameEditText.requestFocus();
            return false;
        }
        
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return false;
        }
        
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return false;
        }
        
        return true;
    }

    /**
     * Safely extracts text from EditText
     * @param editText EditText to extract from
     * @return Trimmed text or empty string
     */
    private String getInputText(TextInputEditText editText) {
        return editText.getText() != null ? 
            editText.getText().toString().trim() : "";
    }

    /**
     * Clears error messages from input fields
     */
    private void clearFieldErrors() {
        usernameEditText.setError(null);
        passwordEditText.setError(null);
    }

    /**
     * Displays error message to user
     * @param message Error message to display
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Displays success message to user
     * @param message Success message to display
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Navigates to weight tracker activity
     * @param userId User ID to pass to next activity
     */
    private void navigateToWeightTracker(long userId) {
        try {
            Intent intent = new Intent(this, WeightTrackerActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish(); // Prevent back navigation to login
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to weight tracker", e);
            showError("Navigation failed. Please try again.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up database connection
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}