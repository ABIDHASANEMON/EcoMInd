package com.memorymate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.memorymate.MainActivity;
import com.memorymate.R;
import com.memorymate.models.Person;
import com.memorymate.models.Reminder;
import com.memorymate.utils.CloudSyncManager;
import com.memorymate.utils.SharedPrefs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignup, tvForgotPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private SharedPrefs sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        sharedPrefs = SharedPrefs.getInstance(this);

        // Check if user is already logged in
        if (sharedPrefs.isLoggedIn()) {
            goToMainActivity();
            return;
        }

        // Initialize views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvSignup = findViewById(R.id.tv_signup);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        progressBar = findViewById(R.id.progress_bar);

        // Login button click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Signup link click
        tvSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });

        // Forgot password link click
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forgotPassword();
            }
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            sharedPrefs.setLoggedIn(true);
                            sharedPrefs.setUserEmail(email);
                            if (user != null) {
                                sharedPrefs.setUserId(user.getUid());

                                // ✅ FIXED: Use LoginActivity.this instead of this

                            }

                            Toast.makeText(LoginActivity.this,
                                    "Welcome back!", Toast.LENGTH_SHORT).show();

                            // ✅ CREATE FIRESTORE STRUCTURE (FIX FOR DELETED DATA)
                            createFirestoreStructure();

                            // Load user data from cloud
                            loadUserDataFromCloud();

                            goToMainActivity();
                        } else {
                            // Sign in fail
                            Toast.makeText(LoginActivity.this,
                                    "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // ✅ ADD THIS METHOD - Creates Firestore structure if missing
    private void createFirestoreStructure() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a test reminder to establish the collection structure
        Map<String, Object> testReminder = new HashMap<>();
        testReminder.put("title", "Setup Complete");
        testReminder.put("description", "Firestore structure created");
        testReminder.put("timestamp", System.currentTimeMillis());
        testReminder.put("type", "setup");
        testReminder.put("isActive", true);

        db.collection("users")
                .document(userId)
                .collection("reminders")
                .document("setup_doc")
                .set(testReminder)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIRESTORE", "✅ Reminders collection created");
                    // Delete the setup document after creation
                    db.collection("users")
                            .document(userId)
                            .collection("reminders")
                            .document("setup_doc")
                            .delete()
                            .addOnSuccessListener(aVoid2 -> Log.d("FIRESTORE", "Setup doc removed"));
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "❌ Failed to create reminders: " + e.getMessage());
                });

        // Also create people collection structure
        Map<String, Object> testPerson = new HashMap<>();
        testPerson.put("name", "Setup");
        testPerson.put("relationship", "System");

        db.collection("users")
                .document(userId)
                .collection("people")
                .document("setup_doc")
                .set(testPerson)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIRESTORE", "✅ People collection created");
                    db.collection("users")
                            .document(userId)
                            .collection("people")
                            .document("setup_doc")
                            .delete();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "❌ Failed to create people: " + e.getMessage());
                });
    }

    private void loadUserDataFromCloud() {
        CloudSyncManager syncManager = CloudSyncManager.getInstance(this);

        // ========== LOAD USER PROFILE ==========
        syncManager.loadUserProfileFromCloud(new CloudSyncManager.CloudLoadListener<Person>() {
            @Override
            public void onSuccess(Person profile) {
                if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
                    // Save profile data to SharedPrefs
                    sharedPrefs.setUserName(profile.getName());
                    sharedPrefs.setEmergencyContact(profile.getPhoneNumber());

                    // Address was stored in photoPath field
                    String address = profile.getPhotoPath();
                    if (address != null && !address.isEmpty()) {
                        sharedPrefs.setUserAddress(address);
                    }

                    Log.d("CloudSync", "Loaded user profile from cloud: " + profile.getName());
                } else {
                    Log.d("CloudSync", "No profile found in cloud");
                }
            }

            @Override
            public void onError(String error) {
                Log.d("CloudSync", "Failed to load profile: " + error);
            }
        });

        // ========== LOAD REMINDERS ==========
        syncManager.loadRemindersFromCloud(new CloudSyncManager.CloudLoadListener<List<Reminder>>() {
            @Override
            public void onSuccess(List<Reminder> reminders) {
                syncManager.saveRemindersToLocal(reminders);
                Log.d("CloudSync", "Loaded " + reminders.size() + " reminders from cloud");
            }

            @Override
            public void onError(String error) {
                Log.e("CloudSync", "Failed to load reminders: " + error);
            }
        });

        // ========== LOAD PEOPLE ==========
        syncManager.loadPeopleFromCloud(new CloudSyncManager.CloudLoadListener<List<Person>>() {
            @Override
            public void onSuccess(List<Person> people) {
                syncManager.savePeopleToLocal(people);
                Log.d("CloudSync", "Loaded " + people.size() + " people from cloud");
            }

            @Override
            public void onError(String error) {
                Log.e("CloudSync", "Failed to load people: " + error);
            }
        });
    }

    private void forgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email to reset password");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this,
                                    "Password reset email sent", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}