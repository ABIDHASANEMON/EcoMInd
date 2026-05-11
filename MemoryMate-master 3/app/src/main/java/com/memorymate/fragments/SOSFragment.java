package com.memorymate.fragments;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.memorymate.R;
import com.memorymate.utils.SharedPrefs;

import java.util.ArrayList;

public class SOSFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private Button sosButton;
    private TextView statusText, countdownText;
    private SharedPrefs sharedPrefs;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isCountingDown = false;
    private Handler countdownHandler = new Handler();
    private int countdownSeconds = 5;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sos, container, false);

        sosButton = view.findViewById(R.id.btn_sos);
        statusText = view.findViewById(R.id.tv_status);
        countdownText = view.findViewById(R.id.tv_countdown);
        sharedPrefs = SharedPrefs.getInstance(getContext());

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());

        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCountingDown) {
                    startSOSCountdown();
                }
            }
        });

        // Test button


        // Check permissions
        checkPermissions();

        return view;
    }

    private void startSOSCountdown() {
        isCountingDown = true;
        countdownSeconds = 5;
        statusText.setText("SOS will activate in:");
        countdownText.setVisibility(View.VISIBLE);
        sosButton.setEnabled(false);
        sosButton.setBackgroundColor(getResources().getColor(R.color.gray));

        // Start countdown
        countdownHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (countdownSeconds > 0) {
                    countdownText.setText(countdownSeconds + " seconds");
                    countdownSeconds--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    // Countdown finished - trigger SOS
                    triggerSOS();
                    resetSOSButton();
                }
            }
        }, 0);
    }

    private void resetSOSButton() {
        isCountingDown = false;
        sosButton.setEnabled(true);
        sosButton.setBackgroundColor(getResources().getColor(R.color.sos_red));
        countdownText.setVisibility(View.GONE);
    }

    private void triggerSOS() {


        statusText.setText("SOS ACTIVATED! Sending alerts...");

        // 1. Vibrate phone
        vibratePhone();

        // 2. Get current location
        getCurrentLocationAndSendAlerts();
    }

    private void vibratePhone() {
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // Emergency vibration pattern: SOS in Morse Code (...---...)
                // This will vibrate once and stop (no repeat)
                long[] pattern = {0, 200, 200, 200, 200, 600, 600, 600, 200, 200, 200};
                vibrator.vibrate(pattern, -1);  // ← Change 0 to -1 (NO REPEAT)
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getCurrentLocationAndSendAlerts() {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Location permission not granted
            sendAlertsWithoutLocation();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Got location
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            sendAlertsWithLocation(latitude, longitude);
                        } else {
                            // Couldn't get location
                            sendAlertsWithoutLocation();
                        }
                    }
                });
    }

    private void sendAlertsWithLocation(double latitude, double longitude) {
        String emergencyContact = sharedPrefs.getEmergencyContact();
        String userName = sharedPrefs.getUserName();

        // Create Google Maps link
        String mapsLink = "https://maps.google.com/?q=" + latitude + "," + longitude;

        // Send TWO SEPARATE SMS
        // SMS 1: Emergency alert
        String sms1 = "🚨 EMERGENCY! " +
                (userName.isEmpty() ? "MemoryMate User" : userName) +
                " needs immediate help!";

        // SMS 2: Location link
        String sms2 = "Location: " + mapsLink + " - MemoryMate";

        // 1. Make emergency call
        makeEmergencyCall(emergencyContact);

        // 2. Send TWO SMS with location
        sendTwoSMSWithLocation(emergencyContact, sms1, sms2);

        // 3. Update status
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                statusText.setText("✓ SOS Activated!\n✓ Location Sent\n✓ Emergency Contact Alerted");
            }
        }, 2000);
    }

    private void sendAlertsWithoutLocation() {
        String emergencyContact = sharedPrefs.getEmergencyContact();
        String userName = sharedPrefs.getUserName();

        String smsMessage = "🚨 EMERGENCY! " +
                (userName.isEmpty() ? "MemoryMate User" : userName) +
                " needs help! (No location) - MemoryMate";

        makeEmergencyCall(emergencyContact);
        sendEmergencySMS(emergencyContact, smsMessage);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                statusText.setText("✓ SOS Activated!\n✓ Emergency Contact Alerted\n⚠ Location unavailable");
            }
        }, 2000);
    }

    private void makeEmergencyCall(String phoneNumber) {
        try {
            if (phoneNumber.isEmpty()) {
                phoneNumber = "112"; // Default emergency number
            }

            Intent callIntent = new Intent(Intent.ACTION_CALL);

            // Format phone number for Bangladesh (+880)
            phoneNumber = formatBangladeshiNumber(phoneNumber);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));

            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
                Toast.makeText(getContext(), "Making emergency call...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to make call", Toast.LENGTH_SHORT).show();
            Log.e("CALL", "Call failed: " + e.getMessage());
        }
    }

    private String formatBangladeshiNumber(String phoneNumber) {
        // Clean the number - remove all non-digits except +
        phoneNumber = phoneNumber.replaceAll("[^\\d+]", "");

        // Remove leading 0 if present (Bangladeshi numbers often start with 0)
        if (phoneNumber.startsWith("0")) {
            phoneNumber = phoneNumber.substring(1);
        }

        // Add Bangladesh country code (+880) if not present
        if (!phoneNumber.startsWith("+880") && !phoneNumber.startsWith("880")) {
            phoneNumber = "+880" + phoneNumber;
        } else if (phoneNumber.startsWith("880")) {
            phoneNumber = "+" + phoneNumber;
        }

        Log.d("PHONE_FORMAT", "Formatted number: " + phoneNumber);
        return phoneNumber;
    }

    // NEW METHOD: Send TWO SMS with location
    private void sendTwoSMSWithLocation(String phoneNumber, String message1, String message2) {
        Log.d("SMS_LOCATION", "Sending TWO SMS with location...");

        try {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(getContext(), "No emergency contact set", Toast.LENGTH_SHORT).show();
                return;
            }

            // Format phone number
            phoneNumber = formatBangladeshiNumber(phoneNumber);

            Log.d("SMS_LOCATION", "Phone: " + phoneNumber);
            Log.d("SMS_LOCATION", "SMS 1: " + message1);
            Log.d("SMS_LOCATION", "SMS 2: " + message2);

            // Check SMS permission
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "SMS permission required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get SMS manager
            final SmsManager smsManager = SmsManager.getDefault();
            final String finalPhoneNumber = phoneNumber;
            final String finalMessage2 = message2;

            // SEND FIRST SMS (Emergency alert)
            try {
                smsManager.sendTextMessage(phoneNumber, null, message1, null, null);
                Log.d("SMS_LOCATION", "First SMS sent: Emergency alert");

                // Wait 1 second, then send SECOND SMS (Location)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            smsManager.sendTextMessage(finalPhoneNumber, null, finalMessage2, null, null);
                            Log.d("SMS_LOCATION", "Second SMS sent: Location link");
                            Toast.makeText(getContext(),
                                    "Emergency SMS with location sent! (2 messages)",
                                    Toast.LENGTH_SHORT).show();
                        } catch (Exception e2) {
                            Log.e("SMS_LOCATION", "Failed to send location SMS: " + e2.getMessage());
                            Toast.makeText(getContext(),
                                    "Emergency sent, location SMS failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }, 1000); // 1 second delay

            } catch (Exception e1) {
                Log.e("SMS_LOCATION", "Failed to send emergency SMS: " + e1.getMessage());

                // Fallback: Try sending as one message
                try {
                    String combined = message1 + " " + message2;
                    smsManager.sendTextMessage(phoneNumber, null, combined, null, null);
                    Toast.makeText(getContext(), "Emergency SMS sent!", Toast.LENGTH_SHORT).show();
                } catch (Exception e3) {
                    // Open SMS app as last resort
                    tryOpenSMSApp(phoneNumber, message1 + "\n" + message2);
                }
            }

        } catch (Exception e) {
            Log.e("SMS_LOCATION", "SMS failed: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Failed to send SMS", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmergencySMS(String phoneNumber, String message) {
        Log.d("SMS", "Sending emergency SMS...");

        try {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(getContext(), "No emergency contact set", Toast.LENGTH_SHORT).show();
                return;
            }

            // Format phone number
            phoneNumber = formatBangladeshiNumber(phoneNumber);

            Log.d("SMS", "Phone: " + phoneNumber);
            Log.d("SMS", "Message: " + message);

            // Check SMS permission
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "SMS permission required", Toast.LENGTH_SHORT).show();
                showSMSCopyDialog(phoneNumber, message);
                return;
            }

            // Get SMS manager
            SmsManager smsManager = SmsManager.getDefault();

            // Try to send SMS
            try {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(getContext(), "Emergency SMS sent!", Toast.LENGTH_SHORT).show();
            } catch (Exception e1) {
                Log.e("SMS", "SMS failed: " + e1.getMessage());

                // Fallback: open SMS app
                tryOpenSMSApp(phoneNumber, message);
            }

        } catch (Exception e) {
            Log.e("SMS", "SMS failed: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Failed to send SMS", Toast.LENGTH_SHORT).show();
        }
    }

    private void tryOpenSMSApp(String phoneNumber, String message) {
        try {
            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
            String uri = "sms:" + phoneNumber + "?body=" + Uri.encode(message);
            smsIntent.setData(Uri.parse(uri));
            smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (smsIntent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivity(smsIntent);
                Toast.makeText(getContext(), "Opening SMS app...", Toast.LENGTH_SHORT).show();
            } else {
                showSMSCopyDialog(phoneNumber, message);
            }
        } catch (Exception e) {
            Log.e("SMS", "Opening SMS app failed: " + e.getMessage());
            showSMSCopyDialog(phoneNumber, message);
        }
    }

    private void showSMSCopyDialog(String phoneNumber, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle("Send SMS Manually")
                .setMessage("Please send this message manually:\n\n" +
                        "To: " + phoneNumber + "\n\n" +
                        "Message:\n" + message)
                .setPositiveButton("Copy to Clipboard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        copyToClipboard(phoneNumber, message);
                    }
                })
                .setNegativeButton("OK", null)
                .show();
    }

    private void copyToClipboard(String phoneNumber, String message) {
        try {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    "Emergency SMS",
                    "To: " + phoneNumber + "\n\n" + message
            );
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getContext(),
                    "Copied to clipboard! Open your SMS app and paste.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to copy to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void testDirectSMS() {
        String testNumber = sharedPrefs.getEmergencyContact();
        if (testNumber.isEmpty()) {
            Toast.makeText(getContext(), "Please set emergency contact in Settings", Toast.LENGTH_LONG).show();
            return;
        }

        testNumber = formatBangladeshiNumber(testNumber);
        String testMessage = "Test SMS from MemoryMate App";

        try {
            // Check permission
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "SMS permission not granted", Toast.LENGTH_LONG).show();
                return;
            }

            // Send test SMS
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(testNumber, null, testMessage, null, null);

            Toast.makeText(getContext(),
                    "Test SMS sent to: " + testNumber,
                    Toast.LENGTH_LONG).show();

            Log.d("TEST_SMS", "Test SMS sent successfully");
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Test failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            Log.e("TEST_SMS", "Test error: " + e.getMessage());
        }
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(getContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(getActivity(),
                    permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(getContext(),
                        "Some permissions denied. SOS may not work fully.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        countdownHandler.removeCallbacksAndMessages(null);
    }
}