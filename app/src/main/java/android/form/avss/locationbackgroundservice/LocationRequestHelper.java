package android.form.avss.locationbackgroundservice;

import android.content.Context;
import android.preference.PreferenceManager;

public class LocationRequestHelper {
    public static final String KEY_LOCATION_UPDATES_REQUESTED = "location-update-requested";

    public static void setRequesting(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_LOCATION_UPDATES_REQUESTED, value).apply();
    }

    public static boolean getRequesting(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_LOCATION_UPDATES_REQUESTED, false);
    }

}
