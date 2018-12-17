package android.form.avss.locationbackgroundservice;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;

import java.util.List;

import static android.form.avss.locationbackgroundservice.LocationUpdatesBroadcastReceiver.ACTION_UPDATES_SERVICES;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 301;
    private String[] permissionsOfList = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL = 10 * 1000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value, but they may be less frequent.
     */
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;

    /**
     * The max time before batched results are delivered by location services. Results may be
     * delivered sooner than this interval.
     */
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 3;


    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;

    TextView tvLocationUpdate;
    Button btnLocationRequest, btnLocationRemove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocationUpdate = findViewById(R.id.tvLocationUpdates);
        btnLocationRequest = findViewById(R.id.btnLocationRequest);
        btnLocationRemove = findViewById(R.id.btnLocationRemove);

        btnLocationRequest.setOnClickListener(this);
        btnLocationRemove.setOnClickListener(this);

        // Check if the user revoked runtime permissions.
        if (!checkPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions();
            }
        }
        buildGoogleApiClient();
    }

    private void requestPermissions() {
        boolean isFineLocation = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        boolean isCoarseLocation = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.

        if (isFineLocation && isCoarseLocation) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            AlertDialog builder = new AlertDialog.Builder(this).create();
            builder.setTitle(getString(R.string.app_name));
            builder.setMessage("These Permission are requires");
            builder.setCancelable(false);
            builder.setButton(1, "OK", (dialog, which) -> {
                // Request permission
                ActivityCompat.requestPermissions(MainActivity.this, permissionsOfList, REQUEST_CODE);
            });
            builder.show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this, permissionsOfList, REQUEST_CODE);
        }
    }

    private boolean checkPermissions() {
        int permissionFine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionCoarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionFine == PackageManager.PERMISSION_GRANTED && permissionCoarse == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLocationRequest:
                requestLocationUpdates();
                break;

            case R.id.btnLocationRemove:
                removeLocationUpdates();
                break;
        }
    }


    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        createLocationRequest();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME);

        // if location is turn off in setting then show message to turn on the location
        LocationSettingsRequest.Builder settingBuilder = new LocationSettingsRequest.Builder();
        settingBuilder.addLocationRequest(mLocationRequest);
        settingBuilder.setAlwaysShow(true);

        Task<LocationSettingsResponse> taskResult = LocationServices.getSettingsClient(this)
                .checkLocationSettings(settingBuilder.build());
        taskResult.addOnSuccessListener(this, locationSettingsResponse -> {

        });

        taskResult.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException apiException = (ResolvableApiException) e;
                    apiException.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException ex) {
                    // Ignore the error
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
//                removeLocationUpdates();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                requestLocationUpdates();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        final String text = "Connection suspended";
        Log.w(TAG, text + ": Error code: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        final String text = "Exception while connecting to Google Play services";
        Log.w(TAG, text + ": " + connectionResult.getErrorMessage());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(LocationResultHelper.KEY_LOCATION_UPDATES_RESULT)) {
            tvLocationUpdate.setText(LocationResultHelper.getSavedLocationResult(this));
        } else if (s.equals(LocationRequestHelper.KEY_LOCATION_UPDATES_REQUESTED)) {
            updateButtonState(LocationRequestHelper.getRequesting(this));
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Intent intent = new Intent(MainActivity.this, LocationUpdatesBroadcastReceiver.class);
            intent.setAction(ACTION_UPDATES_SERVICES);
            intent.putExtra("LOCATION", locationResult);
            LocationUpdatesIntentService.enqueueWork(MainActivity.this, intent);
        }
    };

    /**
     * Handles the Request Updates button and requests start of location updates.
     */
    public void requestLocationUpdates() {
        try {
            Log.i(TAG, "Starting location updates");
            LocationRequestHelper.setRequesting(this, true);
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    toast(location.getLatitude() + "  :  " + location.getLongitude());
                }
            });

            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } catch (Exception e) { }
    }

    /**
     * Handles the Remove Updates button, and requests removal of location updates.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        LocationRequestHelper.setRequesting(this, false);
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        tvLocationUpdate.setText("");
        toast("Removing location updates");
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
        .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonState(LocationResultHelper.getRequesting(this));
        tvLocationUpdate.setText(LocationResultHelper.getSavedLocationResult(this));
    }

    /**
     * Ensures that only one button is enabled at any time. The Start Updates button is enabled
     * if the user is not requesting location updates. The Stop Updates button is enabled if the
     * user is requesting location updates.
     */
    private void updateButtonState(boolean isState) {
        if (isState) {
            btnLocationRemove.setEnabled(true);
            btnLocationRequest.setEnabled(false);
        } else {
            btnLocationRemove.setEnabled(false);
            btnLocationRequest.setEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean isGranted = false;
                for (int perm : grantResults) {
                    if (perm == PackageManager.PERMISSION_GRANTED) {
                        isGranted = true;
                    } else {
                        isGranted = false;
                        break;
                    }
                }

                if (isGranted) {
                    buildGoogleApiClient();
                } else {
                    toast("Permission is not granted");
                }
            }
        }
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
