package com.padia3d.advancedcompass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
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
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    GoogleMap map;
    ImageButton centerCameraButon;

    SensorManager sensorManager;
    Sensor compassSensor;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    TextView latitudeText, longitudeText, altitudeText, accuracyText, speedText;
    Switch infoSwitch;

    boolean autoCenterCamera = false, compassMode = false, infoDisplay = false;
    double bearing;
    int request = 0;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        latitudeText = findViewById(R.id.latitudeText);
        longitudeText = findViewById(R.id.longitudeText);
        altitudeText = findViewById(R.id.altitudeText);
        accuracyText = findViewById(R.id.accuracyText);
        speedText = findViewById(R.id.speedText);

        infoSwitch = findViewById(R.id.infoSwitch);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null) {
            compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getApplicationContext(), "No compass sensor available.", Toast.LENGTH_LONG).show();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Map view");
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        a();

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(10);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (compassMode) {
                                    map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(map.getMyLocation().getLatitude(), map.getMyLocation().getLongitude()), 18, 30, (float) bearing)));
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        thread.start();

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(10);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (infoDisplay) {
                                    latitudeText.setText("Latitude \n" + map.getMyLocation().getLatitude());
                                    longitudeText.setText("Longitude \n" + map.getMyLocation().getLongitude());
                                    altitudeText.setText("Altitude \n" + map.getMyLocation().getAltitude() + "m");
                                    accuracyText.setText("Accuracy \n" + map.getMyLocation().getAccuracy() + "m");
                                    speedText.setText("Speed \n" + map.getMyLocation().getSpeed() + "m/s");
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.start();

        infoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (infoDisplay) {
                    latitudeText.setVisibility(View.INVISIBLE);
                    longitudeText.setVisibility(View.INVISIBLE);
                    altitudeText.setVisibility(View.INVISIBLE);
                    accuracyText.setVisibility(View.INVISIBLE);
                    speedText.setVisibility(View.INVISIBLE);
                    infoDisplay = false;
                } else {
                    latitudeText.setVisibility(View.VISIBLE);
                    longitudeText.setVisibility(View.VISIBLE);
                    altitudeText.setVisibility(View.VISIBLE);
                    accuracyText.setVisibility(View.VISIBLE);
                    speedText.setVisibility(View.VISIBLE);
                    infoDisplay = true;
                }
            }
        });

        if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF) {
            buildLocationRequestAndCallback();
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
    }

    @SuppressLint("RestrictedApi")
    void buildLocationRequestAndCallback() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setSmallestDisplacement(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (request == 0) {
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()), 18, 30, 0)));
                    request++;
                }

                if (autoCenterCamera) {
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(map.getMyLocation().getLatitude(), map.getMyLocation().getLongitude()), 18, 30, 0)));
                }
            }
        };
    }

    void a() {
        centerCameraButon = findViewById(R.id.centerCameraButton);

        centerCameraButon.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                if (!autoCenterCamera) {
                    autoCenterCamera = true;
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(map.getMyLocation().getLatitude(), map.getMyLocation().getLongitude()), 18, 30, 0)));
                    centerCameraButon.setImageResource(R.drawable.center_camera_icon);
                    compassMode = false;
                    centerCameraButon.setColorFilter(Color.parseColor("#4A89F3"));
                } else {
                    autoCenterCamera = false;
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(map.getMyLocation().getLatitude(), map.getMyLocation().getLongitude()), 18, 30, 0)));
                    centerCameraButon.setImageResource(R.drawable.compass_icon);
                    compassMode = true;
                    centerCameraButon.setColorFilter(Color.parseColor("#4A89F3"));
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setBuildingsEnabled(true);
        map.setIndoorEnabled(true);
        map.setMyLocationEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setAllGesturesEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        map.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
            @Override
            public void onMyLocationClick(@NonNull Location location) {
                Toast.makeText(getApplicationContext(), "Debug: " + map.getMyLocation().getLatitude() + " / " + map.getMyLocation().getLongitude(), Toast.LENGTH_LONG).show();
            }
        });

        setListener();
    }

    void setListener() {
        map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if (i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    autoCenterCamera = false;
                    centerCameraButon.setImageResource(R.drawable.center_camera_icon);
                    compassMode = false;
                    centerCameraButon.setColorFilter(Color.parseColor("#1d1d1b"));
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        if (item.getItemId() == R.id.normalMap) {
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        if (item.getItemId() == R.id.satelliteMap) {
            map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }

        if (item.getItemId() == R.id.hybridMap) {
            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }

        if (item.getItemId() == R.id.terrainMap) {
            map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        bearing = event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}