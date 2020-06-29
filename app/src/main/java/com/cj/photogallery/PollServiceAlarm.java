package com.cj.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollServiceAlarm extends IntentService {
    private static final String TAG = "PollServiceAlarm";

    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    public static final String CHANNEL_ID = "PollServiceAlarm";
    public static final String CHANNEL_NAME = "PhotoGallery PollServiceAlarm";

    public static Intent newIntent(Context context) {
        return new Intent(context, PollServiceAlarm.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollServiceAlarm.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (isOn)
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi);
        else {
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollServiceAlarm.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    public PollServiceAlarm() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!isNetworkAvailableAndConnected())
            return;

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);
        List<GalleryItem> items = query == null ? new FlickrFetchr().fetchRecentPhotos() : new FlickrFetchr().searchPhotos(query);

        if (items.isEmpty())
            return;

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId))
            Log.i(TAG, "onHandleIntent: Got an old result: " + resultId);
        else {
            Log.i(TAG, "onHandleIntent: Got a new result: " + resultId);

            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
            Notification notification;

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
                Log.i(TAG, "notification: DEFAULT_CHANNEL_ID=" + NotificationChannel.DEFAULT_CHANNEL_ID + ", mChannel:" + mChannel.toString());
                notificationManager.createNotificationChannel(mChannel);
            }

            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            notificationManager.notify(0, notification);
        }

        QueryPreferences.setLastResultId(this, resultId);
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}