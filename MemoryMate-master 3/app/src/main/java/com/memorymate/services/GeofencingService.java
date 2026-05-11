package com.memorymate.services;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.memorymate.models.SafeZone;
import com.memorymate.receivers.GeofenceBroadcastReceiver;
import com.memorymate.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class GeofencingService {
    private static final String TAG = "Geofencing";
    private Context context;
    private GeofencingClient geofencingClient;
    private DatabaseHelper databaseHelper;

    public GeofencingService(Context context) {
        this.context = context;
        geofencingClient = LocationServices.getGeofencingClient(context);
        databaseHelper = new DatabaseHelper(context);
    }

    public void startGeofencing() {
        List<SafeZone> zones = databaseHelper.getAllSafeZones();
        if (zones.isEmpty()) return;

        List<Geofence> geofences = new ArrayList<>();
        for (SafeZone zone : zones) {
            geofences.add(new Geofence.Builder()
                    .setRequestId(String.valueOf(zone.getId()))
                    .setCircularRegion(zone.getLatitude(), zone.getLongitude(), zone.getRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build());
        }

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofences(geofences)
                .build();

        try {
            geofencingClient.addGeofences(request, getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofencing started"))
                    .addOnFailureListener(e -> Log.e(TAG, "Geofencing failed: " + e.getMessage()));
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted");
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public void stopGeofencing() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofencing stopped"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to stop geofencing"));
    }
}