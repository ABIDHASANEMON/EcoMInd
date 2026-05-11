package com.memorymate.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.memorymate.R;

public class SMSDialog extends Dialog {

    private Context context;
    private String phoneNumber;
    private String message;

    public SMSDialog(Context context, String phoneNumber, String message) {
        super(context);
        this.context = context;
        this.phoneNumber = phoneNumber;
        this.message = message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_sms);

        // Initialize views
        TextView tvPhone = findViewById(R.id.tv_phone);
        TextView tvMessage = findViewById(R.id.tv_message);
        Button btnSendSMS = findViewById(R.id.btn_send_sms);
        Button btnOpenSMSApp = findViewById(R.id.btn_open_sms_app);
        Button btnCopy = findViewById(R.id.btn_copy);

        // Set data
        tvPhone.setText("To: " + phoneNumber);
        tvMessage.setText("Message:\n" + message);

        // Send SMS button (direct - may work on some devices)
        btnSendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDirectSMS();
            }
        });

        // Open SMS app button
        btnOpenSMSApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSMSApp();
            }
        });

        // Copy button
        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard();
            }
        });
    }

    private void sendDirectSMS() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(context, "SMS sent successfully!", Toast.LENGTH_SHORT).show();
            dismiss();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openSMSApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phoneNumber));
            intent.putExtra("sms_body", message);
            context.startActivity(intent);
            dismiss();
        } catch (Exception e) {
            Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyToClipboard() {
        try {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    "Emergency SMS",
                    "To: " + phoneNumber + "\n\n" + message
            );
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
            dismiss();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to copy", Toast.LENGTH_SHORT).show();
        }
    }
}