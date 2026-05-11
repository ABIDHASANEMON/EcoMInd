package com.memorymate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.memorymate.R;
import com.memorymate.activities.AddReminderActivity;
import com.memorymate.models.Reminder;
import com.memorymate.utils.CloudSyncManager;
import com.memorymate.utils.DatabaseHelper;
import com.memorymate.utils.SharedPrefs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersFragment extends Fragment {

    private LinearLayout remindersContainer;
    private DatabaseHelper databaseHelper;
    private SharedPrefs sharedPrefs;
    private CloudSyncManager cloudSyncManager;

    public RemindersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reminders, container, false);

        // Initialize views
        remindersContainer = view.findViewById(R.id.reminders_container);
        Button btnAddReminder = view.findViewById(R.id.btn_add_reminder);

        // Initialize helpers
        databaseHelper = new DatabaseHelper(getContext());
        sharedPrefs = SharedPrefs.getInstance(getContext());
        cloudSyncManager = CloudSyncManager.getInstance(getContext());

        // Load reminders
        loadReminders();

        // Setup button listener - OPEN AddReminderActivity
        btnAddReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), AddReminderActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    private void loadReminders() {
        // Clear existing views
        remindersContainer.removeAllViews();

        // Get reminders from database
        List<Reminder> reminders = databaseHelper.getActiveReminders();

        if (reminders.isEmpty()) {
            // Show empty state
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("No reminders yet. Add your first reminder!");
            tvEmpty.setTextSize(16);
            tvEmpty.setPadding(20, 50, 20, 50);
            tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            remindersContainer.addView(tvEmpty);
        } else {
            // Display each reminder
            for (Reminder reminder : reminders) {
                addReminderView(reminder);
            }
        }
    }

    private void addReminderView(Reminder reminder) {
        // Create reminder card
        View reminderView = getLayoutInflater().inflate(R.layout.item_reminder, null);

        TextView tvTitle = reminderView.findViewById(R.id.tv_reminder_title);
        TextView tvTime = reminderView.findViewById(R.id.tv_reminder_time);
        TextView tvType = reminderView.findViewById(R.id.tv_reminder_type);
        Button btnToggle = reminderView.findViewById(R.id.btn_toggle_reminder);

        // Set data
        tvTitle.setText(reminder.getTitle());
        tvType.setText(reminder.getType());

        // Format time
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, MMM dd", Locale.getDefault());
        String time = sdf.format(new Date(reminder.getTimestamp()));
        tvTime.setText(time);

        // Set toggle button
        btnToggle.setText(reminder.isActive() ? "Disable" : "Enable");
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newState = !reminder.isActive();
                reminder.setActive(newState);
                databaseHelper.updateReminderActiveStatus(reminder.getId(), newState);
                loadReminders(); // Refresh list

                // Sync to cloud when reminder status changes
                syncRemindersToCloud();
            }
        });

        remindersContainer.addView(reminderView);
    }

    // Sync reminders to cloud
    private void syncRemindersToCloud() {
        if (cloudSyncManager != null) {
            cloudSyncManager.syncRemindersToCloud();
            Log.d("RemindersFragment", "Reminders synced to cloud");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReminders();
    }
}