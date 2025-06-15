package com.josephklenk.androidproject;

import com.google.android.material.button.MaterialButton;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeightTrackerActivity extends AppCompatActivity {
    private static final String TAG = "WeightTrackerActivity";
    
    // Model layer
    private DatabaseHelper dbHelper;
    private SMSNotificationHandler smsHandler;
    
    // View components
    private RecyclerView weightHistoryRecyclerView;
    private WeightAdapter adapter;
    private TextView currentWeightText;
    private TextView goalWeightText;
    
    // Data
    private long userId;
    
    // Constants for validation
    private static final double MIN_WEIGHT = 50.0;
    private static final double MAX_WEIGHT = 1000.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_tracker);

        initializeData();
        initializeComponents();
        setupUserInterface();
        loadInitialData();
        
        Log.d(TAG, "WeightTrackerActivity initialized for user: " + userId);
    }

    /**
     * Initializes data and helper components
     */
    private void initializeData() {
        try {
            dbHelper = new DatabaseHelper(this);
            smsHandler = new SMSNotificationHandler(this);
            userId = getIntent().getLongExtra("USER_ID", -1);
            
            if (userId == -1) {
                Log.e(TAG, "Invalid user ID received");
                showError("Invalid user session. Please log in again.");
                finish();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
            showError("Initialization failed. Please restart the app.");
            finish();
        }
    }

    /**
     * Initializes UI components
     */
    private void initializeComponents() {
        setupToolbar();
        initializeViews();
        setupBottomNavigation();
    }

    /**
     * Sets up the toolbar
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Weight Tracker Dashboard");
        }
    }

    /**
     * Initializes view components
     */
    private void initializeViews() {
        currentWeightText = findViewById(R.id.currentWeightText);
        goalWeightText = findViewById(R.id.goalWeightText);
        weightHistoryRecyclerView = findViewById(R.id.weightHistoryRecyclerView);
        
        // Setup RecyclerView
        weightHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup FAB
        FloatingActionButton addWeightFab = findViewById(R.id.addWeightFab);
        addWeightFab.setOnClickListener(v -> showAddWeightDialog());
    }

    /**
     * Sets up bottom navigation
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_goal) {
                showSetGoalDialog();
                return true;
            } else if (itemId == R.id.nav_settings) {
                showSettingsDialog();
                return true;
            }
            return false;
        });
    }

    /**
     * Sets up user interface and loads data
     */
    private void setupUserInterface() {
        updateWeightHistory();
        updateCurrentWeight();
        updateGoalWeight();
    }

    /**
     * Loads initial data for the interface
     */
    private void loadInitialData() {
        // Data loading is handled by individual update methods
        Log.d(TAG, "Initial data loaded for user: " + userId);
    }

    /**
     * Shows enhanced add weight dialog with validation
     */
    private void showAddWeightDialog() {
        try {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_weight, null);
            TextView weightInput = dialogView.findViewById(R.id.weightInput);
            StringBuilder currentInput = new StringBuilder();
            weightInput.setText("0");

            setupNumberPad(dialogView, weightInput, currentInput);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Add Weight Entry")
                    .setView(dialogView)
                    .setPositiveButton("Save", null) // Set to null initially to override
                    .setNegativeButton("Cancel", null)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    if (handleWeightSave(weightInput.getText().toString(), dialog)) {
                        dialog.dismiss();
                    }
                });
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing add weight dialog", e);
            showError("Failed to open weight entry dialog");
        }
    }

    /**
     * Sets up the number pad for weight input
     */
    private void setupNumberPad(View dialogView, TextView weightInput, StringBuilder currentInput) {
        View.OnClickListener numberClickListener = v -> {
            MaterialButton btn = (MaterialButton) v;
            if (currentInput.length() < 5) {
                if (currentInput.length() == 0 && weightInput.getText().toString().equals("0")) {
                    weightInput.setText("");
                }
                currentInput.append(btn.getText());
                weightInput.setText(currentInput.toString());
            }
        };

        // Setup number buttons
        int[] buttonIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                          R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        
        for (int buttonId : buttonIds) {
            dialogView.findViewById(buttonId).setOnClickListener(numberClickListener);
        }

        // Setup special buttons
        dialogView.findViewById(R.id.btnDot).setOnClickListener(v -> {
            if (!currentInput.toString().contains(".") && currentInput.length() > 0) {
                currentInput.append(".");
                weightInput.setText(currentInput.toString());
            }
        });

        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (currentInput.length() > 0) {
                currentInput.setLength(currentInput.length() - 1);
                weightInput.setText(currentInput.length() > 0 ? currentInput.toString() : "0");
            }
        });
    }

    /**
     * Handles weight save with validation
     * @param weightStr Weight string from input
     * @param dialog Dialog to dismiss if successful
     * @return true if save successful, false otherwise
     */
    private boolean handleWeightSave(String weightStr, AlertDialog dialog) {
        if (weightStr.isEmpty() || weightStr.equals("0")) {
            showError("Please enter a valid weight");
            return false;
        }

        try {
            double weight = Double.parseDouble(weightStr);
            
            // Validate weight range
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                showError("Weight must be between " + MIN_WEIGHT + " and " + MAX_WEIGHT + " lbs");
                return false;
            }

            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            long result = dbHelper.addWeight(userId, weight, date);

            if (result != -1) {
                Log.d(TAG, "Weight entry saved successfully: " + weight);
                updateWeightHistory();
                updateCurrentWeight();
                checkGoalWeight(weight);
                showSuccess("Weight recorded successfully!");
                return true;
            } else {
                showError("Failed to save weight entry");
                return false;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid weight format: " + weightStr, e);
            showError("Please enter a valid number");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error saving weight", e);
            showError("Failed to save weight entry");
            return false;
        }
    }

    /**
     * Updates current weight display with error handling
     */
    public void updateCurrentWeight() {
        try {
            double currentWeight = dbHelper.getLatestWeight(userId);
            if (currentWeight > 0) {
                currentWeightText.setText(String.format(Locale.getDefault(), "%.1f lbs", currentWeight));
                Log.d(TAG, "Updated current weight display: " + currentWeight);
            } else {
                currentWeightText.setText("No weight recorded");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating current weight", e);
            currentWeightText.setText("Error loading weight");
        }
    }

    /**
     * Shows set goal dialog with validation
     */
    private void showSetGoalDialog() {
        try {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_goal, null);
            EditText goalWeightInput = dialogView.findViewById(R.id.goalWeightInput);

            double currentGoal = dbHelper.getGoalWeight(userId);
            if (currentGoal > 0) {
                goalWeightInput.setText(String.format(Locale.getDefault(), "%.1f", currentGoal));
            }

            new AlertDialog.Builder(this)
                    .setTitle("Set Goal Weight")
                    .setView(dialogView)
                    .setPositiveButton("Save", (dialog, which) -> handleGoalSave(goalWeightInput))
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing goal dialog", e);
            showError("Failed to open goal setting dialog");
        }
    }

    /**
     * Handles goal weight save with validation
     */
    private void handleGoalSave(EditText goalWeightInput) {
        String weightStr = goalWeightInput.getText().toString().trim();
        
        if (weightStr.isEmpty()) {
            showError("Please enter a goal weight");
            return;
        }

        try {
            double goalWeight = Double.parseDouble(weightStr);
            
            if (goalWeight < MIN_WEIGHT || goalWeight > MAX_WEIGHT) {
                showError("Goal weight must be between " + MIN_WEIGHT + " and " + MAX_WEIGHT + " lbs");
                return;
            }

            long result = dbHelper.setGoalWeight(userId, goalWeight);
            if (result != -1) {
                updateGoalWeight();
                showSuccess("Goal weight updated successfully!");
                Log.d(TAG, "Goal weight set to: " + goalWeight);
            } else {
                showError("Failed to save goal weight");
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid goal weight format", e);
            showError("Please enter a valid number");
        } catch (Exception e) {
            Log.e(TAG, "Error saving goal weight", e);
            showError("Failed to save goal weight");
        }
    }

    /**
     * Shows settings dialog with enhanced validation
     */
    private void showSettingsDialog() {
        try {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
            EditText phoneNumberInput = dialogView.findViewById(R.id.phoneNumberInput);

            SharedPreferences prefs = getSharedPreferences("WeightTrackerPrefs", MODE_PRIVATE);
            String currentPhone = prefs.getString("phone_number", "");
            phoneNumberInput.setText(currentPhone);

            new AlertDialog.Builder(this)
                    .setTitle("Settings")
                    .setView(dialogView)
                    .setPositiveButton("Save", (dialog, which) -> handleSettingsSave(phoneNumberInput, prefs))
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing settings dialog", e);
            showError("Failed to open settings");
        }
    }

    /**
     * Handles settings save with validation
     */
    private void handleSettingsSave(EditText phoneNumberInput, SharedPreferences prefs) {
        try {
            String phoneNumber = phoneNumberInput.getText().toString().trim();
            
            // Basic phone number validation
            if (!phoneNumber.isEmpty() && !isValidPhoneNumber(phoneNumber)) {
                showError("Please enter a valid phone number");
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("phone_number", phoneNumber);
            editor.apply();

            if (!phoneNumber.isEmpty() && !smsHandler.checkSMSPermission()) {
                smsHandler.requestSMSPermission();
            }

            showSuccess("Settings saved successfully!");
            Log.d(TAG, "Settings updated");
        } catch (Exception e) {
            Log.e(TAG, "Error saving settings", e);
            showError("Failed to save settings");
        }
    }

    /**
     * Basic phone number validation
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Basic validation - adjust pattern as needed
        return phoneNumber.matches("^[+]?[1-9]\\d{1,14}$");
    }

    /**
     * Updates weight history with error handling
     */
    public void updateWeightHistory() {
        try {
            Cursor cursor = dbHelper.getWeights(userId);
            if (cursor != null) {
                if (adapter == null) {
                    adapter = new WeightAdapter(cursor);
                    adapter.setUpdateListener(() -> {
                        updateWeightHistory();
                        updateCurrentWeight();
                    });
                    weightHistoryRecyclerView.setAdapter(adapter);
                } else {
                    adapter.swapCursor(cursor);
                }
                Log.d(TAG, "Weight history updated with " + cursor.getCount() + " entries");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating weight history", e);
            showError("Failed to load weight history");
        }
    }

    /**
     * Updates goal weight display
     */
    private void updateGoalWeight() {
        try {
            double goalWeight = dbHelper.getGoalWeight(userId);
            if (goalWeight > 0) {
                goalWeightText.setText(String.format(Locale.getDefault(), "Goal: %.1f lbs", goalWeight));
            } else {
                goalWeightText.setText("No goal set");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating goal weight display", e);
            goalWeightText.setText("Error loading goal");
        }
    }

    /**
     * Checks if goal weight is achieved and sends notification
     */
    private void checkGoalWeight(double currentWeight) {
        try {
            double goalWeight = dbHelper.getGoalWeight(userId);
            Log.d(TAG, "Checking goal weight - Current: " + currentWeight + ", Goal: " + goalWeight);

            if (goalWeight > 0 && Math.abs(currentWeight - goalWeight) <= 0.5) {
                SharedPreferences prefs = getSharedPreferences("WeightTrackerPrefs", MODE_PRIVATE);
                String phoneNumber = prefs.getString("phone_number", "");

                if (!phoneNumber.isEmpty() && smsHandler.checkSMSPermission()) {
                    try {
                        String message = "Congratulations! You've reached your goal weight of " + goalWeight + " lbs!";
                        smsHandler.sendNotification(phoneNumber, message);
                        Log.d(TAG, "Goal achievement SMS sent");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send SMS notification", e);
                    }
                }

                new AlertDialog.Builder(this)
                        .setTitle("Congratulations!")
                        .setMessage("You've reached your goal weight of " + goalWeight + " lbs!")
                        .setPositiveButton("OK", null)
                        .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking goal weight", e);
        }
    }

    /**
     * Shows error message to user
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows success message to user
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (adapter != null && adapter.getCursor() != null) {
                adapter.getCursor().close();
            }
            if (dbHelper != null) {
                dbHelper.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}