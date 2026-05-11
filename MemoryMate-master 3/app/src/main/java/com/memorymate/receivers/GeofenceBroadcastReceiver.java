package com.memorymate.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.memorymate.R;
import com.memorymate.utils.SharedPrefs;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "geofence_alerts";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null) return;

        if (event.hasError()) return;

        int transition = event.getGeofenceTransition();

        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            sendExitNotification(context);
            sendExitAlertToCaregiver(context);
        }
    }

    private void sendExitNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Safe Zone Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ Safe Zone Alert")
                .setContentText("You have left your safe zone!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1002, builder.build());
        }
    }

    private void sendExitAlertToCaregiver(Context context) {
        SharedPrefs prefs = SharedPrefs.getInstance(context);
        String emergencyContact = prefs.getEmergencyContact();

        if (emergencyContact != null && !emergencyContact.isEmpty()) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                String message = "ALERT: MemoryMate user has left their safe zone! Please check on them.";
                smsManager.sendTextMessage(emergencyContact, null, message, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}