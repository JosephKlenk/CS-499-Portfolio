package com.josephklenk.androidproject;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SMSNotificationHandler {
    private static final int SMS_PERMISSION_CODE = 123;
    private Activity activity;

    public SMSNotificationHandler(Activity activity) {
        this.activity = activity;
    }

    public boolean checkSMSPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestSMSPermission() {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.SEND_SMS},
                SMS_PERMISSION_CODE);
    }

    public void sendNotification(String phoneNumber, String message) {
        if (checkSMSPermission()) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}