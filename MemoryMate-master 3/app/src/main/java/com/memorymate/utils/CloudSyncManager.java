package com.memorymate.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.memorymate.models.CloudPerson;
import com.memorymate.models.CloudReminder;
import com.memorymate.models.Person;
import com.memorymate.models.Reminder;

import java.util.ArrayList;
import java.util.List;

public class CloudSyncManager {
    private static final String TAG = "CloudSync";
    private static CloudSyncManager instance;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DatabaseHelper localDb;
    private Context context;

    private CloudSyncManager(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        localDb = new DatabaseHelper(context);
    }

    public static synchronized CloudSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new CloudSyncManager(context);
        }
        return instance;
    }

    private String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "getCurrentUserId: User is null!");
            return null;
        }
        String uid = user.getUid();
        Log.d(TAG, "getCurrentUserId: " + uid);
        return uid;
    }

    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== USER PROFILE SYNC ====================

    // Sync User Profile to Cloud (store as a Person with relationship "Self")
    public void syncUserProfileToCloud(String name, String address, String emergencyContact) {
        String userId = getCurrentUserId();
        if (userId == null) {
            showToast("Not logged in");
            return;
        }

        // Create a Person object for the user themselves
        Person selfPerson = new Person();
        selfPerson.setId(0); // Special ID for user profile
        selfPerson.setName(name);
        selfPerson.setRelationship("Self");
        selfPerson.setPhoneNumber(emergencyContact);
        selfPerson.setPhotoPath(address); // Store address in photoPath temporarily
        selfPerson.setVoiceNotePath("");

        CloudPerson cloudPerson = new CloudPerson(selfPerson, userId);

        db.collection("users")
                .document(userId)
                .collection("profile")
                .document("userProfile")
                .set(cloudPerson)
                .addOnSuccessListener(aVoid -> {
                    showToast("Profile saved to cloud");
                    Log.d(TAG, "Profile synced for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    showToast("Profile sync failed: " + e.getMessage());
                    Log.e(TAG, "Profile sync failed", e);
                });
    }

    // Load User Profile from Cloud
    public void loadUserProfileFromCloud(final CloudLoadListener<Person> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) listener.onError("User not logged in");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("profile")
                .document("userProfile")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        CloudPerson cloudPerson = task.getResult().toObject(CloudPerson.class);
                        if (cloudPerson != null) {
                            Person person = new Person();
                            person.setName(cloudPerson.getName());
                            person.setPhoneNumber(cloudPerson.getPhoneNumber());
                            person.setRelationship(cloudPerson.getRelationship());
                            // Address was stored in photoUrl
                            String address = cloudPerson.getPhotoUrl();
                            person.setPhotoPath(address != null ? address : "");

                            Log.d(TAG, "Loaded user profile from cloud");
                            if (listener != null) listener.onSuccess(person);
                        } else {
                            if (listener != null) listener.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Failed to load profile: " + task.getException().getMessage());
                        if (listener != null) listener.onError(task.getException().getMessage());
                    }
                });
    }

    // ==================== REMINDERS SYNC ====================

    // Sync Reminders to Cloud
    public void syncRemindersToCloud() {
        String userId = getCurrentUserId();
        Log.d(TAG, "syncRemindersToCloud: userId = " + userId);

        if (userId == null) {
            Log.e(TAG, "syncRemindersToCloud: User not logged in!");
            return;
        }

        List<Reminder> reminders = localDb.getActiveReminders();
        Log.d(TAG, "syncRemindersToCloud: Found " + reminders.size() + " reminders to sync");

        if (reminders.isEmpty()) {
            Log.d(TAG, "syncRemindersToCloud: No reminders to sync");
            return;
        }

        CollectionReference remindersRef = db.collection("users")
                .document(userId)
                .collection("reminders");

        for (Reminder reminder : reminders) {
            CloudReminder cloudReminder = new CloudReminder(reminder, userId);
            Log.d(TAG, "syncRemindersToCloud: Syncing reminder ID=" + reminder.getId() +
                    ", Title=" + reminder.getTitle());

            remindersRef.document(String.valueOf(reminder.getId()))
                    .set(cloudReminder)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Reminder synced: " + reminder.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Sync failed: " + e.getMessage(), e));
        }
    }

    // Load Reminders from Cloud
    public void loadRemindersFromCloud(final CloudLoadListener<List<Reminder>> listener) {
        String userId = getCurrentUserId();
        Log.d(TAG, "loadRemindersFromCloud: userId = " + userId);

        if (userId == null) {
            if (listener != null) listener.onError("User not logged in");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("reminders")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Reminder> reminders = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            CloudReminder cloudReminder = doc.toObject(CloudReminder.class);
                            if (cloudReminder != null) {
                                Reminder reminder = new Reminder();
                                reminder.setId(Integer.parseInt(cloudReminder.getId()));
                                reminder.setType(cloudReminder.getType());
                                reminder.setTitle(cloudReminder.getTitle());
                                reminder.setDescription(cloudReminder.getDescription());
                                reminder.setTimestamp(cloudReminder.getTimestamp());
                                reminder.setRecurring(cloudReminder.isRecurring());
                                reminder.setRecurrenceType(cloudReminder.getRecurrenceType());
                                reminder.setVibrationEnabled(cloudReminder.isVibrationEnabled());
                                reminder.setVoiceEnabled(cloudReminder.isVoiceEnabled());
                                reminder.setActive(cloudReminder.isActive());
                                reminders.add(reminder);
                            }
                        }
                        Log.d(TAG, "loadRemindersFromCloud: Loaded " + reminders.size() + " reminders");
                        if (listener != null) listener.onSuccess(reminders);
                    } else {
                        Log.e(TAG, "loadRemindersFromCloud: Failed: " + task.getException().getMessage());
                        if (listener != null) listener.onError(task.getException().getMessage());
                    }
                });
    }

    // Save data to local database from cloud (REPLACE not ADD)
    public void saveRemindersToLocal(List<Reminder> reminders) {
        Log.d(TAG, "saveRemindersToLocal: Saving " + reminders.size() + " reminders");
        new Thread(() -> {
            // FIRST: Delete ALL existing reminders
            localDb.deleteAllReminders();
            Log.d(TAG, "Deleted all existing reminders");

            // SECOND: Add all cloud reminders
            for (Reminder reminder : reminders) {
                localDb.addReminder(reminder);
                Log.d(TAG, "Added cloud reminder: " + reminder.getTitle());
            }
            Log.d(TAG, "saveRemindersToLocal: Completed - " + reminders.size() + " reminders saved");
        }).start();
    }

    // ==================== PEOPLE SYNC ====================

    // Sync People to Cloud
    public void syncPeopleToCloud() {
        String userId = getCurrentUserId();
        Log.d(TAG, "syncPeopleToCloud: userId = " + userId);

        if (userId == null) {
            Log.e(TAG, "syncPeopleToCloud: User not logged in!");
            return;
        }

        List<Person> people = localDb.getAllPeople();
        Log.d(TAG, "syncPeopleToCloud: Found " + people.size() + " people to sync");

        if (people.isEmpty()) {
            Log.d(TAG, "syncPeopleToCloud: No people to sync");
            return;
        }

        CollectionReference peopleRef = db.collection("users")
                .document(userId)
                .collection("people");

        for (Person person : people) {
            CloudPerson cloudPerson = new CloudPerson(person, userId);
            Log.d(TAG, "syncPeopleToCloud: Syncing person ID=" + person.getId() +
                    ", Name=" + person.getName());

            peopleRef.document(String.valueOf(person.getId()))
                    .set(cloudPerson)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Person synced: " + person.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Sync failed: " + e.getMessage(), e));
        }
    }

    // Load People from Cloud
    public void loadPeopleFromCloud(final CloudLoadListener<List<Person>> listener) {
        String userId = getCurrentUserId();
        Log.d(TAG, "loadPeopleFromCloud: userId = " + userId);

        if (userId == null) {
            if (listener != null) listener.onError("User not logged in");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("people")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Person> people = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            CloudPerson cloudPerson = doc.toObject(CloudPerson.class);
                            if (cloudPerson != null) {
                                Person person = new Person();
                                person.setId(Integer.parseInt(cloudPerson.getId()));
                                person.setName(cloudPerson.getName());
                                person.setPhotoPath(cloudPerson.getPhotoUrl());
                                person.setRelationship(cloudPerson.getRelationship());
                                person.setPhoneNumber(cloudPerson.getPhoneNumber());
                                person.setVoiceNotePath(cloudPerson.getVoiceNoteUrl());
                                people.add(person);
                            }
                        }
                        Log.d(TAG, "loadPeopleFromCloud: Loaded " + people.size() + " people");
                        if (listener != null) listener.onSuccess(people);
                    } else {
                        Log.e(TAG, "loadPeopleFromCloud: Failed: " + task.getException().getMessage());
                        if (listener != null) listener.onError(task.getException().getMessage());
                    }
                });
    }

    // Save data to local database from cloud (REPLACE not ADD)
    public void savePeopleToLocal(List<Person> people) {
        Log.d(TAG, "savePeopleToLocal: Saving " + people.size() + " people");
        new Thread(() -> {
            // FIRST: Delete ALL existing people
            localDb.deleteAllPeople();
            Log.d(TAG, "Deleted all existing people");

            // SECOND: Add all cloud people
            for (Person person : people) {
                localDb.addPerson(person);
                Log.d(TAG, "Added cloud person: " + person.getName());
            }
            Log.d(TAG, "savePeopleToLocal: Completed - " + people.size() + " people saved");
        }).start();
    }

    public interface CloudLoadListener<T> {
        void onSuccess(T data);
        void onError(String error);
    }
}