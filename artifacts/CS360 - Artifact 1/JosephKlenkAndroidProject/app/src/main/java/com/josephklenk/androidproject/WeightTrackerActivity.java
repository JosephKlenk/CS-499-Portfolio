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
    private DatabaseHelper dbHelper;
    private RecyclerView weightHistoryRecyclerView;
    private WeightAdapter adapter;
    private long userId;
    private SMSNotificationHandler smsHandler;
    private TextView currentWeightText;
    private TextView goalWeightText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_tracker);

        dbHelper = new DatabaseHelper(this);
        smsHandler = new SMSNotificationHandler(this);
        userId = getIntent().getLongExtra("USER_ID", -1);

        setupToolbar();
        initializeViews();
        setupBottomNavigation();
        updateWeightHistory();
        updateCurrentWeight();
        updateGoalWeight();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Dashboard");
        }
    }

    private void initializeViews() {
        currentWeightText = findViewById(R.id.currentWeightText);
        goalWeightText = findViewById(R.id.goalWeightText);
        weightHistoryRecyclerView = findViewById(R.id.weightHistoryRecyclerView);
        weightHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton addWeightFab = findViewById(R.id.addWeightFab);
        addWeightFab.setOnClickListener(v -> showAddWeightDialog());
    }

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

    private void showAddWeightDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_weight, null);
        TextView weightInput = dialogView.findViewById(R.id.weightInput);
        StringBuilder currentInput = new StringBuilder();
        weightInput.setText("0");

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

        dialogView.findViewById(R.id.btn0).setOnClickListener(numberClickListener);
        dialogView.findViewById(R.id.btn1).setOnClickListener(numberClickListener);
        dialogView.findViewById(R.id.btn2).setOnClickListener(numberClickListener);
        dialogView.findViewById(R.id.btn3).setOnClickListener(numberClickListener);
        dialogView.findViewById(R.id.btn4).setOnClickListener(numberClickListener);
        dialogView.findViewById(R.id.btn5).setOnClickListener(numberClickListener);
        dialogView.findViewById(R.id.btn6).setOnClickListener(numberClickListener);
        dialogView.findViewById(R.id.btn7).setOnClickListener(numberClickListener);
        dialogView.findViewById(R.id.btn8).setOnClickListener(numberClickListener);
        dialogView.findViewById(R.id.btn9).setOnClickListener(numberClickListener);

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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Save", null) // Set to null initially
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String weightStr = weightInput.getText().toString();
                if (!weightStr.isEmpty() && !weightStr.equals("0")) {
                    try {
                        double weight = Double.parseDouble(weightStr);
                        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        long result = dbHelper.addWeight(userId, weight, date);

                        if (result != -1) {
                            updateWeightHistory();
                            updateCurrentWeight();
                            checkGoalWeight(weight);
                            dialog.dismiss();
                        }
                    } catch (NumberFormatException e) {
                        Log.e("WeightTracker", "Invalid weight format", e);
                    }
                }
            });
        });

        dialog.show();
    }

    public void updateCurrentWeight() {
        double currentWeight = dbHelper.getLatestWeight(userId);
        if (currentWeight > 0) {
            currentWeightText.setText(String.format(Locale.getDefault(), "%.1f lbs", currentWeight));
            Log.d("WeightTracker", "Updated current weight to: " + currentWeight);
        }

        // Force UI refresh
        weightHistoryRecyclerView.getAdapter().notifyDataSetChanged();
    }

    private void showSetGoalDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_goal, null);
        EditText goalWeightInput = dialogView.findViewById(R.id.goalWeightInput);

        double currentGoal = dbHelper.getGoalWeight(userId);
        if (currentGoal > 0) {
            goalWeightInput.setText(String.format(Locale.getDefault(), "%.1f", currentGoal));
        }

        new AlertDialog.Builder(this)
                .setTitle("Set Goal Weight")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String weightStr = goalWeightInput.getText().toString();
                    if (!weightStr.isEmpty()) {
                        double goalWeight = Double.parseDouble(weightStr);
                        dbHelper.setGoalWeight(userId, goalWeight);
                        updateGoalWeight();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        EditText phoneNumberInput = dialogView.findViewById(R.id.phoneNumberInput);

        SharedPreferences prefs = getSharedPreferences("WeightTrackerPrefs", MODE_PRIVATE);
        String currentPhone = prefs.getString("phone_number", "");
        phoneNumberInput.setText(currentPhone);

        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String phoneNumber = phoneNumberInput.getText().toString();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("phone_number", phoneNumber);
                    editor.apply();

                    if (!smsHandler.checkSMSPermission()) {
                        smsHandler.requestSMSPermission();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveWeight(double weight) {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long result = dbHelper.addWeight(userId, weight, date);
        if (result != -1) {
            updateWeightHistory();
            updateCurrentWeight();
            checkGoalWeight(weight);
        }
    }

    public void updateWeightHistory() {
        Cursor cursor = dbHelper.getWeights(userId);
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
    }

    private void updateGoalWeight() {
        double goalWeight = dbHelper.getGoalWeight(userId);
        if (goalWeight > 0) {
            goalWeightText.setText(String.format(Locale.getDefault(), "Goal: %.1f lbs", goalWeight));
        }
    }

    private void checkGoalWeight(double currentWeight) {
        double goalWeight = dbHelper.getGoalWeight(userId);
        Log.d("WeightTracker", "Checking goal weight - Current: " + currentWeight + ", Goal: " + goalWeight);

        if (goalWeight > 0 && Math.abs(currentWeight - goalWeight) <= 0.5) {
            SharedPreferences prefs = getSharedPreferences("WeightTrackerPrefs", MODE_PRIVATE);
            String phoneNumber = prefs.getString("phone_number", "");
            Log.d("WeightTracker", "Phone number from prefs: " + phoneNumber);

            if (!phoneNumber.isEmpty()) {
                if (smsHandler.checkSMSPermission()) {
                    try {
                        String message = "Congratulations! You've reached your goal weight of " + goalWeight + " lbs!";
                        smsHandler.sendNotification(phoneNumber, message);
                        Log.d("WeightTracker", "SMS sent to: " + phoneNumber);
                    } catch (Exception e) {
                        Log.e("WeightTracker", "Failed to send SMS", e);
                    }
                } else {
                    Log.d("WeightTracker", "No SMS permission");
                    smsHandler.requestSMSPermission();
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Congratulations!")
                    .setMessage("You've reached your goal weight!")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null && adapter.getCursor() != null) {
            adapter.getCursor().close();
        }
    }
}