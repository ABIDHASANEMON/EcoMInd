package com.memorymate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import android.os.Build;
import android.content.pm.PackageManager;
import android.Manifest;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.memorymate.fragments.PeopleFragment;
import com.memorymate.fragments.RemindersFragment;
import com.memorymate.fragments.SOSFragment;
import com.memorymate.fragments.SettingsFragment;
import com.memorymate.fragments.VoiceAssistantFragment;  // ✅ ADD THIS
import com.memorymate.services.GeofencingService;
import com.memorymate.utils.NotificationHelper;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationHelper.createChannel(this);

        GeofencingService geofencingService = new GeofencingService(this);
        geofencingService.startGeofencing();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);

        loadFragment(new RemindersFragment());

        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_reminders) {
                fragment = new RemindersFragment();
            } else if (itemId == R.id.nav_people) {
                fragment = new PeopleFragment();
            } else if (itemId == R.id.nav_sos) {
                fragment = new SOSFragment();
            } else if (itemId == R.id.nav_settings) {
                fragment = new SettingsFragment();
            } else if (itemId == R.id.nav_voice) {      // ✅ ADD THIS CASE
                fragment = new VoiceAssistantFragment();
            }

            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    // Helper method to switch tabs (used by VoiceAssistantFragment to trigger SOS)
    public void selectNavigationItem(int itemId) {
        bottomNavigation.setSelectedItemId(itemId);
    }
}