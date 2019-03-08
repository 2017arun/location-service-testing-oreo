package android.form.avss.locationbackgroundservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import java.util.List;

import static android.form.avss.locationbackgroundservice.MainActivity.LOCATION;

/**
 * Receiver for handling location updates.
 *
 * For apps targeting API level O
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)} should be used when
 * requesting location updates. Due to limits on background services,
 * {@link android.app.PendingIntent#getService(Context, int, Intent, int)} should not be used.
 *
 *  Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 *  less frequently than the interval specified in the
 *  {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 *  foreground.
 */
public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "LUBroadcastReceiver";
    public static final String ACTION_UPDATES_SERVICES = "android.form.avss.locationbackgroundservice";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (ACTION_UPDATES_SERVICES.equals(intent.getAction())) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    LocationResult result = bundle.getParcelable(LOCATION);
                    if (result != null) {
                        List<Location> locations = result.getLocations();
                        LocationResultHelper locationResultHelper = new LocationResultHelper(context, locations);
                        // Save the location data to SharedPreferences.
                        locationResultHelper.saveResults();
                        // Show notification with the location data.
                        locationResultHelper.showNotification();
                        Log.i(TAG, LocationResultHelper.getSavedLocationResult(context));
                    }
                }
            }
        }
    }
}
