package com.memorymate.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefs {
    private static final String PREFS_NAME = "MemoryMatePrefs";
    private static SharedPrefs instance;
    private SharedPreferences prefs;

    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ADDRESS = "user_address";
    private static final String KEY_EMERGENCY_CONTACT = "emergency_contact";
    private static final String KEY_SOS_ACTIVE = "sos_active";

    private SharedPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefs getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefs(context);
        }
        return instance;
    }

    // User Info
    public void setUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "User");
    }

    public void setUserAddress(String address) {
        prefs.edit().putString(KEY_USER_ADDRESS, address).apply();
    }

    public String getUserAddress() {
        return prefs.getString(KEY_USER_ADDRESS, "Address not set");
    }

    // Emergency Contact
    public void setEmergencyContact(String contact) {
        prefs.edit().putString(KEY_EMERGENCY_CONTACT, contact).apply();
    }

    public String getEmergencyContact() {
        return prefs.getString(KEY_EMERGENCY_CONTACT, "");
    }

    // SOS Status
    public void setSOSActive(boolean active) {
        prefs.edit().putBoolean(KEY_SOS_ACTIVE, active).apply();
    }

    public boolean isSOSActive() {
        return prefs.getBoolean(KEY_SOS_ACTIVE, false);
    }

    // Clear all data
    public void clearAll() {
        prefs.edit().clear().apply();
    }


    // Add these keys
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";

    // Add these methods
    public void setLoggedIn(boolean isLoggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public void setUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void logout() {
        prefs.edit().clear().apply();
        setLoggedIn(false);
    }
}
