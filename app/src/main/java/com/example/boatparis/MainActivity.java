package com.example.boatparis;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_APP = "APP";
    private static final String TAG_LOCATION = "LOCATION";
    private Button btnTOList;
    private Button btnTOInfo;
    private SupportMapFragment sMapFrag;
    private FusedLocationProviderClient loc;
    public static Map<String, Map<String, Object>> resultMap = new HashMap<>();


    public static LatLng latLng;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MAP ACTIVITY","APP CREATE");
        super.onCreate(savedInstanceState);
        new FetchDataTask().execute("https://data.iledefrance.fr/api/explore/v2.1/catalog/datasets/principaux-sites-touristiques-en-ile-de-france0/records?limit=100&refine=dep%3A75&refine=typo_niv3%3AHaltes%2C%20escales%20ou%20ports%20fluviaux%20de%20plaisance");

        setContentView(R.layout.activity_main);



        Log.d(TAG_APP, "CREATION APP");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        sMapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.MY_MAP);
        loc = (FusedLocationProviderClient) LocationServices.getFusedLocationProviderClient(this);
        btnTOList=findViewById(R.id.button);
        btnTOInfo=findViewById(R.id.btn2info);

        // Ajoutez un écouteur de clic au bouton
        btnTOList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Code pour passer à une autre activité
                Intent intent = new Intent(MainActivity.this, list_monument.class);
                startActivity(intent);
            }
        });

        btnTOInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Code pour passer à une autre activité
                Intent intent = new Intent(MainActivity.this, info_page.class);
                startActivity(intent);
            }
        });

    }

    private class FetchDataTask extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                connection.disconnect();

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode donneesOriginales = objectMapper.readTree(response.toString());

                JsonNode results = donneesOriginales.get("results");

                for (JsonNode resultat : results) {
                    String objectId = resultat.get("objectid").asText();
                    Map<String, Object> locationMap = new HashMap<>();
                    locationMap.put("latitude", resultat.get("geo_point_2d").get("lat").asDouble());
                    locationMap.put("longitude", resultat.get("geo_point_2d").get("lon").asDouble());
                    locationMap.put("adresse", resultat.get("adresse").asText());
                    locationMap.put("nom_carto", resultat.get("nom_carto").asText());
                    resultMap.put(objectId, locationMap);
                }

            } catch (Exception e) {
                Log.e("Error", "Error fetching data", e);
            }

            return null;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MAP ACTIVITY","APP START");
        Dexter.withContext(getApplicationContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        getCurrentLocation();
                        Log.d(TAG_APP, "PERMISSION GRANTED");
                        Toast.makeText(MainActivity.this, "LOCATION ACCESS", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Log.d(TAG_APP, "PERMISSION DENIED");
                        Toast.makeText(MainActivity.this, "Please enable Location App Permissions", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                        Log.d(TAG_APP, "PERMISSION");
                    }
                }).check();


    }

    public void getCurrentLocation() {
        Log.d(TAG_LOCATION, "SEARCH");

        @SuppressLint("MissingPermission") Task<Location> task = loc.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                sMapFrag.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(@NonNull GoogleMap googleMap) {
                        Log.d(TAG_LOCATION, "MAP LOCATION");
                        if (location != null) {
                            latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Current Location!");
                            googleMap.addMarker(markerOptions);
                            Toast.makeText(MainActivity.this, "LOCATION FOUND", Toast.LENGTH_SHORT).show();
                            for (Map.Entry<String, Map<String, Object>> entry : resultMap.entrySet()) {
                                Map<String, Object> locationData = entry.getValue();
                                double latitude = (double) locationData.get("latitude");
                                double longitude = (double) locationData.get("longitude");
                                LatLng latLng_monument = new LatLng(latitude, longitude);
                                MarkerOptions markerOptions_monument = new MarkerOptions().position(latLng_monument).title((String) locationData.get("nom_carto"));
                                googleMap.addMarker(markerOptions_monument);
                            }


                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            Log.d(TAG_LOCATION, "OK Location");
                            Log.d(TAG_LOCATION + " LONG", String.valueOf(latLng.longitude));
                            Log.d(TAG_LOCATION + " LATI", String.valueOf(latLng.latitude));

                        } else {
                            Toast.makeText(MainActivity.this, "Please enable Location App Permissions", Toast.LENGTH_SHORT).show();
                            Log.d(TAG_LOCATION, "UNSEARCH");
                        }
                    }

                });
            }
        });
    }
    public void onPause() {
        super.onPause();
        Log.d("MAP ACTIVITY","APP STOP");
    }

    public void onResume() {
        super.onResume();
        Log.d("MAP ACTIVITY","APP RESUME");
    }

}
