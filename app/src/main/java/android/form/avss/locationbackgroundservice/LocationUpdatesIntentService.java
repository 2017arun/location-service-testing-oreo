package android.form.avss.locationbackgroundservice;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import java.util.List;

import static android.form.avss.locationbackgroundservice.LocationUpdatesBroadcastReceiver.ACTION_UPDATES_SERVICES;


public class LocationUpdatesIntentService extends JobIntentService {
    private static final String TAG = LocationUpdatesIntentService.class.getSimpleName();

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 1000;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, LocationUpdatesIntentService.class, JOB_ID, work);
    }

    public LocationUpdatesIntentService() {
        super();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_UPDATES_SERVICES.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                LocationResult result = bundle.getParcelable(MainActivity.LOCATION);
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    LocationResultHelper locationResultHelper = new LocationResultHelper(this, locations);
                    // Save the location data to SharedPreferences.
                    locationResultHelper.saveResults();
                    // Show notification with the location data.
                    locationResultHelper.showNotification();
                    Log.i(TAG, LocationResultHelper.getSavedLocationResult(this));
                }
            }
        }
    }
}