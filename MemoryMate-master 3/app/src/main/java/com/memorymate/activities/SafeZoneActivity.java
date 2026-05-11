package com.memorymate.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.memorymate.R;
import com.memorymate.models.SafeZone;
import com.memorymate.services.GeofencingService;
import com.memorymate.utils.DatabaseHelper;
import com.memorymate.utils.SharedPrefs;

public class SafeZoneActivity extends AppCompatActivity {

    private EditText etZoneName, etLatitude, etLongitude, etRadius;
    private Button btnSaveZone, btnUseCurrentLocation;
    private DatabaseHelper databaseHelper;
    private SharedPrefs sharedPrefs;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_zone);

        databaseHelper = new DatabaseHelper(this);
        sharedPrefs = SharedPrefs.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etZoneName = findViewById(R.id.et_zone_name);
        etLatitude = findViewById(R.id.et_latitude);
        etLongitude = findViewById(R.id.et_longitude);
        etRadius = findViewById(R.id.et_radius);
        btnSaveZone = findViewById(R.id.btn_save_zone);
        btnUseCurrentLocation = findViewById(R.id.btn_use_current_location);

        btnUseCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnSaveZone.setOnClickListener(v -> saveSafeZone());
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            etLatitude.setText(String.valueOf(location.getLatitude()));
                            etLongitude.setText(String.valueOf(location.getLongitude()));
                            Toast.makeText(SafeZoneActivity.this,
                                    "Location captured! You can adjust radius.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SafeZoneActivity.this,
                                    "Could not get location. Make sure GPS is on.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveSafeZone() {
        String name = etZoneName.getText().toString().trim();
        String latStr = etLatitude.getText().toString().trim();
        String lngStr = etLongitude.getText().toString().trim();
        String radiusStr = etRadius.getText().toString().trim();

        if (name.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
            Toast.makeText(this, "Please fill zone name and location", Toast.LENGTH_SHORT).show();
            return;
        }

        double latitude = Double.parseDouble(latStr);
        double longitude = Double.parseDouble(lngStr);
        float radius = radiusStr.isEmpty() ? 100 : Float.parseFloat(radiusStr);

        SafeZone zone = new SafeZone(name, latitude, longitude, radius, sharedPrefs.getUserId());
        long id = databaseHelper.addSafeZone(zone);

        if (id != -1) {
            Toast.makeText(this, "Safe zone saved!", Toast.LENGTH_SHORT).show();
            new GeofencingService(this).startGeofencing();
            finish();
        } else {
            Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            Toast.makeText(this, "Location permission required to use current location", Toast.LENGTH_SHORT).show();
        }
    }
}