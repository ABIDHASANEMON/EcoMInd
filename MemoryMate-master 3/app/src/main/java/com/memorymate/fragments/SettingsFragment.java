package com.memorymate.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.memorymate.R;
import com.memorymate.activities.LoginActivity;
import com.memorymate.activities.SafeZoneActivity;
import com.memorymate.models.Person;
import com.memorymate.models.SafeZone;
import com.memorymate.services.GeofencingService;
import com.memorymate.utils.CloudSyncManager;
import com.memorymate.utils.DatabaseHelper;
import com.memorymate.utils.SharedPrefs;

import java.util.List;

public class SettingsFragment extends Fragment {

    private EditText etName, etAddress, etEmergencyContact;
    private Button btnSave, btnLogout, btnSetSafeZone;
    private TextView tvSafeZoneStatus;
    private SharedPrefs sharedPrefs;
    private CloudSyncManager cloudSyncManager;
    private DatabaseHelper databaseHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize views
        etName = view.findViewById(R.id.et_name);
        etAddress = view.findViewById(R.id.et_address);
        etEmergencyContact = view.findViewById(R.id.et_emergency_contact);
        btnSave = view.findViewById(R.id.btn_save);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnSetSafeZone = view.findViewById(R.id.btn_set_safe_zone);
        tvSafeZoneStatus = view.findViewById(R.id.tv_safe_zone_status);

        // Initialize helpers
        sharedPrefs = SharedPrefs.getInstance(getContext());
        cloudSyncManager = CloudSyncManager.getInstance(getContext());
        databaseHelper = new DatabaseHelper(getContext());

        // Load existing data
        loadUserData();
        loadSafeZoneStatus();

        // Setup save button
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });

        // Setup logout button
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        // Setup Safe Zone button
        btnSetSafeZone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SafeZoneActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    private void loadUserData() {
        // First load from SharedPrefs (local)
        etName.setText(sharedPrefs.getUserName());
        etAddress.setText(sharedPrefs.getUserAddress());
        etEmergencyContact.setText(sharedPrefs.getEmergencyContact());

        // Then load from cloud and update
        cloudSyncManager.loadUserProfileFromCloud(new CloudSyncManager.CloudLoadListener<Person>() {
            @Override
            public void onSuccess(Person profile) {
                if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
                    // Update UI with cloud data
                    etName.setText(profile.getName());
                    etEmergencyContact.setText(profile.getPhoneNumber());
                    sharedPrefs.setUserName(profile.getName());
                    sharedPrefs.setEmergencyContact(profile.getPhoneNumber());
                }
            }

            @Override
            public void onError(String error) {
                // No cloud data yet, that's fine
            }
        });
    }

    private void loadSafeZoneStatus() {
        List<SafeZone> zones = databaseHelper.getAllSafeZones();
        if (!zones.isEmpty()) {
            SafeZone zone = zones.get(0);
            tvSafeZoneStatus.setText("✓ Safe Zone: " + zone.getName() + "\n   Radius: " + (int)zone.getRadius() + " meters");
        } else {
            tvSafeZoneStatus.setText("⚠ No safe zone set.\nTap 'Set Safe Zone' to add your safe area.");
        }
    }

    private void saveUserData() {
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String emergencyContact = etEmergencyContact.getText().toString().trim();

        if (emergencyContact.isEmpty()) {
            etEmergencyContact.setError("Emergency contact is required for SOS");
            return;
        }

        // Save locally
        sharedPrefs.setUserName(name);
        sharedPrefs.setUserAddress(address);
        sharedPrefs.setEmergencyContact(emergencyContact);

        // Save to cloud
        cloudSyncManager.syncUserProfileToCloud(name, address, emergencyContact);

        Toast.makeText(getContext(), "Settings saved to cloud!", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Clear local data
                        SharedPrefs.getInstance(getContext()).logout();

                        // Sign out from Firebase
                        FirebaseAuth.getInstance().signOut();

                        // Go to login screen
                        Intent intent = new Intent(getContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSafeZoneStatus();
    }
}