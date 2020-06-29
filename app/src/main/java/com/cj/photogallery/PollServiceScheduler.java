package com.cj.photogallery;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public class PollServiceScheduler extends JobService {
    private static final String TAG = "PollServiceScheduler";

    public static final int JOB_ID = 1;
    public static final String CHANNEL_ID = "PollServiceAlarm";
    public static final String CHANNEL_NAME = "PhotoGallery PollServiceAlarm";

    private PollTask mCurrentTask;

    public static boolean hasBeenScheduled(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo jobInfo : scheduler.getAllPendingJobs())
            if (jobInfo.getId() == JOB_ID)
                return true;
        return false;
    }

    public static void setJobSchedule(Context context, boolean setJob) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (setJob) {
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, PollServiceScheduler.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(1000 * 15)
                    .setPersisted(true)
                    .build();
            scheduler.schedule(jobInfo);
        } else
            scheduler.cancel(JOB_ID);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        mCurrentTask = new PollTask();
        mCurrentTask.execute(params);
        Log.i(TAG, "onStartJob: mCurrentTask=" + mCurrentTask);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "onStopJob: mCurrentTask=" + mCurrentTask);
        if (mCurrentTask != null)
            mCurrentTask.cancel(true);
        return true;
    }

    private class PollTask extends AsyncTask<JobParameters, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(JobParameters... params) {
            JobParameters jobParams = params[0];

            // Poll Flickr for new images
            String query = QueryPreferences.getStoredQuery(PollServiceScheduler.this);
            List<GalleryItem> items = query == null ? new FlickrFetchr().fetchRecentPhotos() : new FlickrFetchr().searchPhotos(query);
            Log.i(TAG, "doInBackground: items.size()=" + items.size());

            jobFinished(jobParams, false);
            return items;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if (items.isEmpty())
                return;

            String lastResultId = QueryPreferences.getLastResultId(PollServiceScheduler.this);
            String resultId = items.get(0).getId();
            if (resultId.equals(lastResultId))
                Log.i(TAG, "PollTask: Got an old result: " + resultId);
            else {
                Log.i(TAG, "PollTask: Got a new result: " + resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(PollServiceScheduler.this);
                PendingIntent pi = PendingIntent.getActivity(PollServiceScheduler.this, 0, i, 0);
                Notification notification;

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(PollServiceScheduler.this);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
                    Log.i(TAG, "notification: DEFAULT_CHANNEL_ID=" + NotificationChannel.DEFAULT_CHANNEL_ID + ", mChannel:" + mChannel.toString());
                    notificationManager.createNotificationChannel(mChannel);
                }

                notification = new NotificationCompat.Builder(PollServiceScheduler.this, CHANNEL_ID)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();

                notificationManager.notify(0, notification);
            }

            QueryPreferences.setLastResultId(PollServiceScheduler.this, resultId);
        }
    }
}
