package com.padia3d.advancedcompass;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

import static android.location.LocationManager.*;

public class LocationInfoActivity extends AppCompatActivity {

    TextView latitudeText, longitudeText, altitudeText, accuracyText, speedText, speedAccuracyText;
    Button openMapsButon;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Location information");
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        latitudeText = findViewById(R.id.latitudeText);
        longitudeText = findViewById(R.id.longitudeText);
        altitudeText = findViewById(R.id.altitudeText);
        accuracyText = findViewById(R.id.accuracyText);
        speedText = findViewById(R.id.speedText);
        openMapsButon = findViewById(R.id.openMapsButon);
        speedAccuracyText = findViewById(R.id.providerText);

        if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF) {
            buildLocationRequestAndCallback();
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            //Toast.makeText(getApplicationContext(), "Locating...", Toast.LENGTH_LONG).show();
        } else {
            latitudeText.setText("Latitude \n...");
            longitudeText.setText("Longitude \n...");
        }

        openMapsButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("geo:"));
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_access_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.home) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            finish();
        }

        /*if (item.getItemId() == R.id.mapButton) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            startActivity(new Intent(LocationInfoActivity.this, MapActivity.class));
        }*/
        return super.onOptionsItemSelected(item);
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
                    altitudeText.setText("Altitude \n" + loc.getAltitude() + "m");
                    accuracyText.setText("Accuracy \n" + loc.getAccuracy() + "m");
                    speedText.setText("Speed \n" + loc.getSpeed() + "m/s");
                    speedAccuracyText.setText("Provider \n" + loc.getProvider());
                }
            }
        };
    }
}