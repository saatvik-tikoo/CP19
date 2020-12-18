package com.example.cp19;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    String[] requestPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};
    private LocationManager locationManager;
    private String provider;
    private Criteria criteria;
    private MapView mMapView;
    private LocationDisplay mLocationDisplay;
    private int requestPermissionsCode = 2;
    private double lat, lng;
    private GraphicsOverlay mGraphicsOverlay;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        mMapView = findViewById(R.id.mapView);

        ArcGISMap mMap = new ArcGISMap(Basemap.createStreetsVector());
        ArcGISRuntimeEnvironment.setLicense(getResources().getString(R.string.arcgis_license_key));
        mMapView.setMap(mMap);
        setupLocationDisplay();
        createGraphicsOverlay();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    requestPermissions, requestPermissionsCode);
        }
        Location location = locationManager.getLastKnownLocation(provider);
        onLocationChanged(location);
    }

    public void onLocationChanged(Location location) {
        Geocoder geocoder;
        List<Address> user;
        if (location == null) {
            Toast.makeText(this, "Location Not found", Toast.LENGTH_LONG).show();
        } else {
            geocoder = new Geocoder(this);
            try {
                user = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                lat = user.get(0).getLatitude();
                lng = user.get(0).getLongitude();
                getLocationData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getLocationData() {
        String API = "http://10.0.2.2:8000/api/markers?lat=" + lat + "&long=" + lng;
        JsonObjectRequest requestLocation;
        RequestQueue requestQueue;
        requestLocation = new JsonObjectRequest(Request.Method.GET, API, null, res -> {
            try {
                String message = res.getString("message");
                if (message.equals("success")) {
                    JSONArray data = res.getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject jsonObject = data.getJSONObject(i);
                        int color;
                        String givenColor = jsonObject.getString("Color");

                        if (givenColor.equals("Red")) color = Color.RED;
                        else if (givenColor.equals("Yellow")) color = Color.YELLOW;
                        else color = Color.GREEN;
                        createPointGraphics(jsonObject.getDouble("Lat"),
                                            jsonObject.getDouble("Long"),
                                            color);
                    }
                }
            } catch (JSONException e) {
                System.out.println(e);
            }
        }, error -> System.out.println(error));
        requestLocation.setRetryPolicy(new DefaultRetryPolicy(
                50000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(requestLocation);
    }

    private void createGraphicsOverlay() {
        mGraphicsOverlay = new GraphicsOverlay();
        mGraphicsOverlay.setOpacity(0.5f);
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
    }

    private void createPointGraphics(double l1, double l2, Integer color) {
        Point point = new Point(l2, l1, SpatialReferences.getWgs84());
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,
                color, 100.0f);
        pointSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID,
                color, 2.0f));
        Graphic pointGraphic = new Graphic(point, pointSymbol);
        mGraphicsOverlay.getGraphics().add(pointGraphic);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            mLocationDisplay.startAsync();

            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, false);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        requestPermissions, requestPermissionsCode);
            }
            Location location = locationManager.getLastKnownLocation(provider);
            onLocationChanged(location);

        } else {
            Toast.makeText(MainActivity.this, getResources()
                    .getString(R.string.location_permission_denied), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void setupLocationDisplay() {
        mLocationDisplay = mMapView.getLocationDisplay();
        mLocationDisplay.addDataSourceStatusChangedListener(dataSourceStatusChangedEvent -> {
            if (dataSourceStatusChangedEvent.isStarted() ||
                    dataSourceStatusChangedEvent.getError() == null) {
                return;
            }
            if (!(ContextCompat.checkSelfPermission(MainActivity.this,
                    requestPermissions[0]) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(MainActivity.this,
                    requestPermissions[1]) == PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        requestPermissions, requestPermissionsCode);
            } else {
                String message = String.format("Error in DataSourceStatusChangedListener: %s",
                        dataSourceStatusChangedEvent.getSource().getLocationDataSource()
                                .getError().getMessage());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
        mLocationDisplay.startAsync();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar, menu);

        MenuItem register = menu.findItem(R.id.register);
        MenuItem covidPos = menu.findItem(R.id.covid_pos);

        register.setOnMenuItemClickListener(item -> {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialogue_register);
            dialog.show();

            Button btn = dialog.findViewById(R.id.button_register);
            btn.setOnClickListener(v -> {
                EditText email = dialog.findViewById(R.id.textDialog);
                SharedPreferences pref = this.getSharedPreferences("cp19", 0);
                SharedPreferences.Editor editor = pref.edit();
                Random x = new Random();
                String uuid = Integer.toString(x.nextInt(106) + 101);
                if (!pref.contains("uuid")) {
                    editor.putString("uuid", uuid);
                    editor.commit();
                } else {
                    uuid = pref.getString("uuid", "");
                }

                String API = "http://10.0.2.2:8000/api/user/register?id=" + uuid
                        + "&email=" + email.getText().toString();
                JsonObjectRequest registerEmail;
                RequestQueue requestQueue;
                registerEmail = new JsonObjectRequest(Request.Method.PATCH, API, null, res -> {
                }, error -> System.out.println(error));
                registerEmail.setRetryPolicy(new DefaultRetryPolicy(
                        50000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                requestQueue = Volley.newRequestQueue(this);
                requestQueue.add(registerEmail);
                dialog.cancel();
            });
            return false;
        });
        covidPos.setOnMenuItemClickListener(item -> {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialogue_covid);
            dialog.show();

            Button btn = dialog.findViewById(R.id.button_covid);
            btn.setOnClickListener(v -> {
                RadioGroup covid = dialog.findViewById(R.id.covid);
                int radioBtnId = covid.getCheckedRadioButtonId();
                RadioButton radioBtn = dialog.findViewById(radioBtnId);
                String ans;
                if (radioBtn.getText().toString().toLowerCase().equals("yes")) ans = "True";
                else ans = "False";

                SharedPreferences pref = this.getSharedPreferences("cp19", 0);
                SharedPreferences.Editor editor = pref.edit();
                Random x = new Random();
                String uuid = Integer.toString(x.nextInt(106) + 101);
                if (!pref.contains("uuid")) {
                    editor.putString("uuid", uuid);
                    editor.commit();
                } else {
                    uuid = pref.getString("uuid", "");
                }

                String API = "http://10.0.2.2:8000/api/user/covidmarker?id=" + uuid
                        + "&covid=" + ans;
                JsonObjectRequest markCovid;
                RequestQueue requestQueue;
                markCovid = new JsonObjectRequest(Request.Method.PATCH, API, null, res -> {
                }, error -> System.out.println(error));
                markCovid.setRetryPolicy(new DefaultRetryPolicy(
                        50000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                requestQueue = Volley.newRequestQueue(this);
                requestQueue.add(markCovid);
                dialog.cancel();
            });
            return false;
        });

        return true;
    }

    @Override
    protected void onPause() {
        if (mMapView != null) {
            mMapView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null) {
            mMapView.dispose();
        }
        super.onDestroy();
    }
}
