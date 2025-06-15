package com.josephklenk.androidproject;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Enhanced SMS Notification Handler with improved error handling and logging
 * Manages SMS permissions and message sending functionality
 */
public class SMSNotificationHandler {
    private static final String TAG = "SMSNotificationHandler";
    private static final int SMS_PERMISSION_CODE = 123;
    private Activity activity;

    public SMSNotificationHandler(Activity activity) {
        this.activity = activity;
    }

    /**
     * Checks if SMS permission is granted
     * @return true if permission granted, false otherwise
     */
    public boolean checkSMSPermission() {
        try {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking SMS permission", e);
            return false;
        }
    }

    /**
     * Requests SMS permission from user
     */
    public void requestSMSPermission() {
        try {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_CODE);
            Log.d(TAG, "SMS permission requested");
        } catch (Exception e) {
            Log.e(TAG, "Error requesting SMS permission", e);
        }
    }

    /**
     * Sends SMS notification with enhanced error handling and validation
     * @param phoneNumber Recipient phone number
     * @param message Message to send
     */
    public void sendNotification(String phoneNumber, String message) {
        if (!validateInputs(phoneNumber, message)) {
            return;
        }

        if (!checkSMSPermission()) {
            Log.w(TAG, "SMS permission not granted");
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // Check if message needs to be split for long messages
            if (message.length() > 160) {
                Log.d(TAG, "Sending multipart SMS");
                smsManager.sendMultipartTextMessage(phoneNumber, null, 
                    smsManager.divideMessage(message), null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }
            
            Log.d(TAG, "SMS sent successfully to: " + phoneNumber);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception sending SMS - permission denied", e);
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS notification", e);
        }
    }

    /**
     * Validates phone number and message inputs
     * @param phoneNumber Phone number to validate
     * @param message Message to validate
     * @return true if inputs are valid, false otherwise
     */
    private boolean validateInputs(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.w(TAG, "Invalid phone number provided");
            return false;
        }
        
        if (message == null || message.trim().isEmpty()) {
            Log.w(TAG, "Invalid message provided");
            return false;
        }
        
        // Basic phone number format validation
        if (!phoneNumber.matches("^[+]?[1-9]\\d{1,14}$")) {
            Log.w(TAG, "Phone number format validation failed: " + phoneNumber);
            return false;
        }
        
        return true;
    }
}