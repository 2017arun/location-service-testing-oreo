package android.form.avss.locationbackgroundservice;

import android.content.Context;
import android.preference.PreferenceManager;

class LocationRequestHelper {
    static final String KEY_LOCATION_UPDATES_REQUESTED = "location-update-requested";

    static void setRequesting(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_LOCATION_UPDATES_REQUESTED, value).apply();
    }

    static boolean getRequesting(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_LOCATION_UPDATES_REQUESTED, false);
    }

}
