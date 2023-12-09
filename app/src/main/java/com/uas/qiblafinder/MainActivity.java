package com.uas.qiblafinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private Compass compass;
    private ImageView compassOuter, compassDegree, compassArrow;
    private TextView textCity, textDegree;
    private ImageButton btnDeveloper;
    private float currentAzimuth;
    private String city;
    SharedPreferences prefs;
    GPSTracker gps;
    Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("", MODE_PRIVATE);
        gps = new GPSTracker(this);

        compassOuter = (ImageView) findViewById(R.id.compassOuter);
        compassDegree = (ImageView) findViewById(R.id.compassDegree);
        compassArrow = (ImageView) findViewById(R.id.compassArrow);

        textCity = (TextView) findViewById(R.id.textCity);
        textDegree = (TextView) findViewById(R.id.textDegree);

        setupCompass();
        fetchGPS();

        String qiblaDeg = String.valueOf(Math.round(GetFloat("qibla_degree"))) + "°";

        textCity.setText(city);
        textDegree.setText(qiblaDeg);

        btnDeveloper = (ImageButton) findViewById(R.id.btnDeveloper);
        btnDeveloper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DeveloperActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (compass != null) {
            compass.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (compass != null) {
            compass.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (compass != null) {
            compass.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (compass != null) {
            compass.start();
        }
    }

    public void SaveBoolean(String key, Boolean value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public Boolean GetBoolean(String key) {
        Boolean result = prefs.getBoolean(key, false);
        return result;
    }

    public void SaveFloat(String key, Float value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    public Float GetFloat(String key) {
        Float result = prefs.getFloat(key, 0);
        return result;
    }

    private void setupCompass() {
        Boolean permissionGranted = GetBoolean("permission_granted");

        if (permissionGranted) {
            getBearing();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }

        compass = new Compass(this);
        Compass.CompassListener compassListener = new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(float azimuth) {
                adjustCompassDegree(azimuth);
                adjustCompassArrow(azimuth);
            }
        };

        compass.setListener(compassListener);
    }

    public void adjustCompassDegree(float azimuth) {
        Animation animation = new RotateAnimation(-currentAzimuth, -azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        currentAzimuth = (azimuth);
        animation.setDuration(500);
        animation.setRepeatCount(0);
        animation.setFillAfter(true);
        compassDegree.setAnimation(animation);
    }

    public void adjustCompassArrow(float azimuth) {
        float qiblaDegree = GetFloat("qibla_degree");
        Animation animation = new RotateAnimation(-(currentAzimuth) + qiblaDegree, -azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        currentAzimuth = (azimuth);

        int minAzimuth = (int) (Math.floor(GetFloat("qibla_degree")) - 3);
        int maxAzimuth = (int) (Math.ceil(GetFloat("qibla_degree")) + 3);

        if (currentAzimuth >= minAzimuth && currentAzimuth <= maxAzimuth ) {
            compassOuter.setImageResource(R.drawable.compass_outer_green);
        } else {
            compassOuter.setImageResource(R.drawable.compass_outer_gray);
        }

        animation.setDuration(500);
        animation.setRepeatCount(0);
        animation.setFillAfter(true);
        compassArrow.startAnimation(animation);
    }

    @SuppressLint("MissingPermission")
    public void getBearing() {
        float qiblaDegree = GetFloat("qibla_degree");
        if (qiblaDegree <= 0.0001) {
            fetchGPS();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SaveBoolean("permission_granted", true);
                    setupCompass();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission tidak diizinkan!", Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void fetchGPS() {
        double result = 0;
        gps = new GPSTracker(this);
        geocoder = new Geocoder(this, Locale.getDefault());
        StringBuilder builder = new StringBuilder();

        if (gps.canGetLocation()) {
            double myLat = gps.getLatitude();
            double myLon = gps.getLongitude();

            try {
                List<Address> addresses = geocoder.getFromLocation(myLat, myLon, 1);
                city = addresses.get(0).getSubAdminArea();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (myLat >= 0.001 && myLon >= 0.001) {
                // posisi ka'bah
                double kabahLat = Math.toRadians(21.422487);
                double kabahLon = 39.826206;
                double myRadiansLat = Math.toRadians(myLat);
                double lonDiff = Math.toRadians(kabahLon - myLon);
                double y = Math.sin(lonDiff) * Math.cos(kabahLat);
                double x = Math.cos(myRadiansLat) * Math.sin(kabahLat) - Math.sin(myRadiansLat) * Math.cos(kabahLat) * Math.cos(lonDiff);
                result = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
                SaveFloat("qibla_degree", (float) result);
            }
        } else {
            gps.showSettingAlert();
        }
    }
}