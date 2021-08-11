package com.padia3d.advancedcompass;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager sensorManager;
    Sensor compassSensor;
    ImageView compassImage, compassDegrees;
    ProgressBar progressBar1;
    TextView debugText, latitudeText, longitudeText;
    Button locationButton;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar));

        compassImage = findViewById(R.id.compassIndicator);
        debugText = findViewById(R.id.textView);
        latitudeText = findViewById(R.id.latitudeText);
        longitudeText = findViewById(R.id.longitudeText);
        progressBar1 = findViewById(R.id.progressDegrees);
        locationButton = findViewById(R.id.locationButon);
        compassDegrees = findViewById(R.id.compassDegrees);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null) {
            compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getApplicationContext(), "No compass sensor available.", Toast.LENGTH_LONG).show();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}, 101);
        } else {
            if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF) {
                buildLocationRequestAndCallback();
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            } else {
                latitudeText.setText("Latitude \n...");
                longitudeText.setText("Longitude \n...");
            }
        }

        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                startActivity(new Intent(MainActivity.this, LocationInfoActivity.class));
            }
        });
    }

    @SuppressLint("RestrictedApi")
    void buildLocationRequestAndCallback() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        SharedPreferences preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(preferences.getInt("updatingTime", 25));
        locationRequest.setSmallestDisplacement(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location loc : locationResult.getLocations()) {
                    latitudeText.setText("Latitude \n" + loc.getLatitude());
                    longitudeText.setText("Longitude \n" + loc.getLongitude());
                    locationButton.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        debugText.setText(new DecimalFormat("0.00").format(event.values[0]) + "°");
        compassImage.setRotation(-event.values[0]);
        compassDegrees.setRotation(-event.values[0]);

        if (event.values[0] < 360) {
            progressBar1.setRotation(-90);
            progressBar1.setScaleX(1);
            progressBar1.setProgress((int) (((360 - event.values[0]) / 3.6) + 0.5f));
        }

        if (event.values[0] <= 180) {
            progressBar1.setRotation(90);
            progressBar1.setScaleX(-1);
            progressBar1.setProgress((int) (((event.values[0]) / 3.6) + 0.5f));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.degreesItem) {
            startActivity(new Intent(MainActivity.this, DegreesActivity.class));
        }

        if (item.getItemId() == R.id.settingsItem) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}