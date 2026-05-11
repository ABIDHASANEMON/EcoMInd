package com.memorymate.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.memorymate.R;
import com.memorymate.models.Reminder;
import com.memorymate.receivers.ReminderReceiver;
import com.memorymate.utils.CloudSyncManager;
import com.memorymate.utils.DatabaseHelper;
import com.memorymate.utils.NotificationHelper;

import java.util.Calendar;
import java.util.Date;

public class AddReminderActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private RadioGroup rgType;
    private Button btnTime, btnSave;
    private long selectedTime;
    private DatabaseHelper databaseHelper;
    private Reminder pendingReminder;

    // Register for permission result
    private final ActivityResultLauncher<Intent> alarmPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Check if permission is now granted
                if (hasExactAlarmPermission()) {
                    if (pendingReminder != null) {
                        scheduleNotification(pendingReminder);
                        // Also sync to cloud after permission granted
                        CloudSyncManager.getInstance(this).syncRemindersToCloud();
                        Toast.makeText(this, "Reminder scheduled! Notification will appear at the set time.", Toast.LENGTH_LONG).show();
                        pendingReminder = null;
                        finish();
                    }
                } else {
                    Toast.makeText(this, "Permission denied. Reminder will not work.", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        // Create notification channel
        NotificationHelper.createChannel(this);

        databaseHelper = new DatabaseHelper(this);

        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);
        rgType = findViewById(R.id.rg_type);
        btnTime = findViewById(R.id.btn_time);
        btnSave = findViewById(R.id.btn_save);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        selectedTime = calendar.getTimeInMillis();
        updateTimeButton();

        btnTime.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> saveReminder());
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    cal.set(Calendar.MINUTE, minute1);
                    cal.set(Calendar.SECOND, 0);

                    if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    selectedTime = cal.getTimeInMillis();
                    updateTimeButton();
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private void updateTimeButton() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedTime);
        String time = String.format("%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
        btnTime.setText("Time: " + time);
    }

    private boolean hasExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            alarmPermissionLauncher.launch(intent);
        }
    }

    private void saveReminder() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Please enter a title");
            return;
        }

        if (selectedTime <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = "medicine";
        int checkedId = rgType.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_meal) type = "meal";
        else if (checkedId == R.id.rb_appointment) type = "appointment";

        Reminder reminder = new Reminder(type, title, description, selectedTime,
                true, 0, true, true);

        long id = databaseHelper.addReminder(reminder);
        if (id != -1) {
            reminder.setId((int) id);



            // ✅ SYNC TO FIREBASE CLOUD
            CloudSyncManager.getInstance(this).syncRemindersToCloud();

            if (hasExactAlarmPermission()) {
                scheduleNotification(reminder);
                Toast.makeText(this, "Reminder added and synced to cloud!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                pendingReminder = reminder;
                Toast.makeText(this, "Please grant Alarms permission for reminders to work", Toast.LENGTH_LONG).show();
                requestExactAlarmPermission();
            }
        } else {
            Toast.makeText(this, "Failed to add reminder", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleNotification(Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("reminder_title", reminder.getTitle());
        intent.putExtra("reminder_text", reminder.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                reminder.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.getTimestamp(), pendingIntent);
            Log.d("Reminder", "Alarm scheduled for: " + new Date(reminder.getTimestamp()));
        }
    }
}