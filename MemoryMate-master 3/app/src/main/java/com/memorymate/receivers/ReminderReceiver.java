package com.memorymate.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.memorymate.utils.NotificationHelper;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("reminder_title");
        String message = intent.getStringExtra("reminder_text");
        if (title == null) title = "Reminder";
        if (message == null) message = "Time for your reminder";
        NotificationHelper.showReminder(context, title, message);
    }
}