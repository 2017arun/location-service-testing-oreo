package android.form.avss.locationbackgroundservice;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import static android.form.avss.locationbackgroundservice.LocationRequestHelper.KEY_LOCATION_UPDATES_REQUESTED;

/**
 * Class to process location results.
 */
class LocationResultHelper {
    static final String KEY_LOCATION_UPDATES_RESULT = "location-update-result";
    private static final String PRIMARY_CHANNEL = "default";
    private Context mContext;
    private List<Location> mLocations;
    private NotificationManager mNotificationManager;

    LocationResultHelper(Context context, List<Location> locations) {
        mContext = context;
        mLocations = locations;
    }

    /**
     * Returns the title for reporting about a list of {@link Location} objects.
     */
    private String getLocationResultTittle() {
        String numLocationReported = mContext.getResources().
                getQuantityString(R.plurals.num_locations_reported, mLocations.size(), mLocations.size());
        return numLocationReported + " : " + DateFormat.getDateTimeInstance().format(new Date());
    }

    private String getLocationResultText() {
        if (mLocations.isEmpty()) {
            return mContext.getString(R.string.unknown_location);
        }

        StringBuilder sb = new StringBuilder();
        for (Location location : mLocations) {
            sb.append("(");
            sb.append(location.getLatitude());
            sb.append(", ");
            sb.append(location.getLongitude());
            sb.append(")");
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Saves location result as a string to {@link android.content.SharedPreferences}.
     */
    void saveResults() {
        PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .putString(KEY_LOCATION_UPDATES_RESULT, getLocationResultTittle() + "\n" + getLocationResultText())
                .apply();
    }

    /**
     * Fetches location results from {@link android.content.SharedPreferences}.
     */
    static String getSavedLocationResult(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_LOCATION_UPDATES_RESULT, "");
    }

    static boolean getRequesting(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_LOCATION_UPDATES_REQUESTED, false);
    }

    /**
     * Get the notification mNotificationManager.
     * <p>
     * Utility method as this helper works with it a lot.
     *
     * @return The system service NotificationManager
     */
    private NotificationManager getNotificationManager() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mNotificationManager;
    }

    /**
     * Displays a notification with the location results.
     */
    void showNotification() {
        Intent notificationIntent = new Intent(mContext, MainActivity.class);

        //construct a task stack
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(mContext);

        // Add the main Activity to the task stack as the parent.
        taskStackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        taskStackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, PRIMARY_CHANNEL)
                .setContentTitle(getLocationResultTittle())
                .setContentText(getLocationResultText())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setVibrate(new long[500])
                .setContentIntent(notificationPendingIntent);
        getNotificationManager().notify(0, notificationBuilder.build());
    }
}
