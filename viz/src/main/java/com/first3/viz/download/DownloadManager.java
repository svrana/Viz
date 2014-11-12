/*
 * Copyright 2012-2014, First Three LLC
 *
 * This file is a part of Viz.
 *
 * Viz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Viz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Viz.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.first3.viz.download;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.first3.viz.Preferences;
import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.models.Resource;
import com.first3.viz.provider.VizContract;
import com.first3.viz.provider.VizContract.Resources;
import com.first3.viz.provider.VizContract.Downloads;
import com.first3.viz.ui.ActivityDelegate;
import com.first3.viz.utils.DownloadTask;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Maps;
import com.first3.viz.utils.Utils;

public class DownloadManager extends Service {
    private final Map<Resource, DownloadData> downloadMap = Maps.newHashMap();

    private final Queue<Resource> downloadQueue = new LinkedList<Resource>();

    private final ProgressListener downloadListener = new DownloadListener();

    private static volatile int mStartId = 0;

    private NotificationManager mNM;

    /** The client with which we're bound.  There can be only one. */
    private Messenger mClient;

    private Handler mIncomingHandler;

    /** Target we publish for clients to send messages to IncomingHandler.  */
    private Messenger mMessenger;

    /** Used to start the Async Tasks..which is perhaps a poor choice now. */
    private Handler mHandler = VizApp.getHandler();

    private static final int VIZ_NOTIFICATION = R.string.local_service_started;

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_CMD_CLIENT_REGISTER = 1;

    /**
     * Command to the service to unregister a client, or stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_CLIENT_REGISTER.
     */
    public static final int MSG_CMD_CLIENT_UNREGISTER = 2;

    /** Command to the service to start the download indicated in the Message. */
    public static final int MSG_CMD_DOWNLOAD_INITIATE = 3;

    /**
     * Command to the service to stop the download indicated by the Resource in the Message.
     */
    public static final int MSG_CMD_DOWNLOAD_PAUSE = 4;

    /**
     * Message from the service indicating the current progress of the download for UI updates.
     */
    public static final int MSG_STATUS_DOWNLOAD_PROGRESS = 10;

    /**
     * Message from the service indicating that the download was completed successfully.
     */
    public static final int MSG_STATUS_DOWNLOAD_SUCCESS = 11;

    /**
     * Message from the service indicating that the download could not finish
     * successfully.  An error message will be supplied along with this
     * message.
     */
    public static final int MSG_STATUS_DOWNLOAD_FAILED = 12;

    public static final String RESOURCE = "com.first3.viz.Resource";
    public static final String FAILURE_TEXT = "failure_text";

    /**
     * The number of downloads prior to new download requests waiting in the queue.
     *
     * This number is based on how I've seen Android behave on a Galaxy Nexus - the
     * 5th attempt to create a thread hangs until one of the ehread stops.  We
     * keep it one lower so we can create a thread to do work of our own.
     */
    private static int MAX_CONCURRENT_DOWNLOADS = 4;

    @Override
    public void onCreate() {
        Log.d();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Start up the thread running the service.  Note that we create
        // a separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it background
        // priority so CPU-intensive work will not disrupt our UI.
        //HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        //thread.start();

        mIncomingHandler = new IncomingHandler();
        mMessenger = new Messenger(mIncomingHandler);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        Log.d("shutting down service");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("client bound");
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        Log.d("starting service, id " + startId);
        // for now, only take commands from the bound.  We could process
        // download requests from external browsers here.
        return START_NOT_STICKY;
    }

    private void sendMsg(Message m) {
        if (mClient == null) {
            Log.d("Client is null, dropping message");
            return;
        }

        try {
            Log.d("Client bound, sending message");
            mClient.send(m);
        } catch (RemoteException e) {
            Log.d("Client is gone");
            // Client is dead, so we'll wait for it to rebind
            mClient = null;
        }
    }

    private synchronized int totalDownloads() {
        return downloadQueue.size() + numOngoingDownloads();
    }

    /**
     * Sends the client a message of each in-progress or queued download and
     * its progress.
     */
    private synchronized void sendDownloadsStatus() {
        Collection<DownloadData> c = downloadMap.values();
        for(DownloadData dd : c) {
            sendProgressUpdate(dd.getResource(), dd.getProgress());
        }

        for(Resource resource : downloadQueue) {
            sendProgressUpdate(resource, 0);
        }
    }

    private void sendDownloadFailedMsg(Resource resource) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(DownloadManager.RESOURCE, resource);
        String failureText = getFailureText(resource);
        bundle.putString(FAILURE_TEXT, failureText);
        Message m = Message.obtain(null, MSG_STATUS_DOWNLOAD_FAILED);
        m.setData(bundle);
        sendMsg(m);
    }

    private void sendDownloadSuccessMsg(Resource resource) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(DownloadManager.RESOURCE, resource);
        Message m = Message.obtain(null, MSG_STATUS_DOWNLOAD_SUCCESS);
        m.setData(bundle);
        sendMsg(m);
    }

    private void sendProgressUpdate(Resource resource, int progress) {
        Message m = Message.obtain(null, MSG_STATUS_DOWNLOAD_PROGRESS, progress, 0);
        Bundle bundle = new Bundle();
        bundle.putParcelable(DownloadManager.RESOURCE, resource);
        m.setData(bundle);
        sendMsg(m);
    }

    /**
     * Handler of incoming messages from Viz UI.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Resource r;

            switch (msg.what) {
                case MSG_CMD_CLIENT_REGISTER:
                    Log.i("Added client");
                    mClient = msg.replyTo;
                    if (totalDownloads() != 0) {
                        sendDownloadsStatus();
                    }
                    break;
                case MSG_CMD_CLIENT_UNREGISTER:
                    Log.i("Removed client");
                    mClient = null;
                    break;
                case MSG_CMD_DOWNLOAD_INITIATE:
                    r = getResourceFromMsg(msg);
                    Log.i("initiating download of " + r);
                    download(r);
                    break;
                case MSG_CMD_DOWNLOAD_PAUSE:
                    r = getResourceFromMsg(msg);
                    Log.i("pausing download of " + r);
                    pause(r);
                    break;
                default:
                    Log.e("Message not handled: " + msg.what);
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private Resource getResourceFromMsg(Message msg) {
        Bundle b = msg.getData();
        b.setClassLoader(getClassLoader());
        Resource r = b.getParcelable(RESOURCE);
        return r;
    }

    private synchronized int numOngoingDownloads() {
        return downloadMap.size();
    }

    private synchronized void queueDownload(Resource resource) {
        changeDownloadStatus(resource, Downloads.Status.QUEUED);
        downloadQueue.add(resource);
        showNotification(true);
    }

    /**
     * Download the file pointed to by the Resource.
     *
     * Careful: This may not be run on the UI thread in the case of onCancel
     */
    private synchronized void download(final Resource resource) {
        if (numOngoingDownloads() >= VizApp.getPrefs().getInt(Preferences.MAX_CONCURRENT_DOWNLOADS,
                    MAX_CONCURRENT_DOWNLOADS)) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    queueDownload(resource);
                }
            };
            Utils.threadStart(t, "ErrorOnQueueDownloadThread");
            return;
        }

        // AsyncTasks must be created and run on the UI thread. download()
        // may be called from done() which may not be called from on the UI thread.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                DownloadTask task = new DownloadTask(downloadListener);

                addDownload(resource, new DownloadData(resource, task));

                changeDownloadStatus(resource, Downloads.Status.INPROGRESS);

                showNotification(true);
                try {
                    // Yes, calling run here. run() is overridden in DownloadTask
                    task.run(resource);
                } catch(Throwable th) {
                    Log.e("Download failed: " + th);
                }
            }
        });
    }

    /**
     * Stops the download represented by resource.
     *
     * Changes the status of the Download to Paused.
     */
    private synchronized void pause(Resource resource) {
        Log.d("(uri=" + resource + ")");
        if (!downloadQueue.remove(resource)) {
            DownloadData data = downloadMap.get(resource);
            if (data != null) {
                Log.d("Interrupting download task");
                data.getTask().cancel(true);
            }
        } else {
            Log.d("Download was removed from queue");
        }
    }

    // How can resource be null here?
    private void changeDownloadStatus(Resource resource, Downloads.Status status) {
        ContentValues map = new ContentValues();

        if (status == Downloads.Status.COMPLETE) {
            map.put(VizContract.Downloads.PERCENT_COMPLETE, 100);
        } else {
            long currentFilesize = resource.getCurrentFilesize();
            int percentComplete = resource.getPercentComplete();

            map.put(VizContract.Downloads.CURRENT_FILESIZE,
                    String.valueOf(currentFilesize));
            map.put(VizContract.Downloads.PERCENT_COMPLETE, percentComplete);
        }

        map.put(VizContract.Downloads.STATUS, status.valueOf());
        getContentResolver().update(resource.getDownloadUri(), map, null, null);
    }

    private void addResource(Resource resource) {
        ContentValues map = resource.toContentValues();
        getContentResolver().insert(Resources.CONTENT_URI, map);
    }

    /** Pick a download request from the queue and start downloading. */
    private synchronized boolean checkQueue() {
        Resource resource = downloadQueue.poll();
        if (resource != null) {
            download(resource);
            return true;
        }
        return false;
    }

    private void shutdown(final int startId) {
        // mStartId is incremented on the UI thread, so make sure we only
        // shutdown the service on the UI thread so we don't shutdown the
        // service as its spinning up
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (totalDownloads() == 0 && startId == mStartId) {
                    stopForeground(true);
                    stopSelf(startId);
                }
            }
        });
    }

    private synchronized void addDownload(Resource resource, DownloadData dd) {
        downloadMap.put(resource, dd);
    }

    private synchronized void done(Resource resource) {
        downloadMap.remove(resource);
        if (!checkQueue()) {
            // Don't update the ticker each time a download finishes.  Maybe
            // update it with different text?
            showNotification(false);
        }

        if (totalDownloads() == 0) {
            shutdown(mStartId);
        }
    }

    private synchronized String getFailureText(Resource resource) {
        DownloadData dd = downloadMap.get(resource);
        if (dd != null) {
            DownloadTask task = dd.getTask();
            return task.getFailureText();
        }
        return null;
    }

    private synchronized void updateProgress(Resource resource, int progress) {
        // could be racing with a cancel()
        DownloadData data = downloadMap.get(resource);
        if (data != null) {
            data.setProgress(progress);
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification(boolean setTicker) {
        int numDownloads = numOngoingDownloads();
        int numQueued =  downloadQueue.size();

        String videos = getResources().getQuantityString(R.plurals.notification_title_numvideos,
                numDownloads, numDownloads);
        String queued = getResources().getQuantityString(R.plurals.notification_subtitle_numqueued,
                numQueued, numQueued);

        Intent startViz = new Intent(this, ActivityDelegate.class);
        Bundle bundle = new Bundle();
        bundle.putInt(ActivityDelegate.BUNDLE_LOAD_TAB, 3); // navigate to the Downloads tab
        startViz.putExtras(bundle);

        PendingIntent intent = PendingIntent.getActivity(this, 0, startViz, 0);

        Bitmap notificationIconLarge = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        String tickerText;
        if (numQueued > 0) {
            tickerText = videos + ", " + queued;
        } else {
            tickerText = videos;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setLargeIcon(notificationIconLarge)
                .setSmallIcon(R.drawable.ic_stat_notification_arrow)
                .setContentTitle(videos)
                .setContentIntent(intent);
        if (setTicker) {
            builder.setTicker(tickerText);
        }
        if (numQueued > 0) {
            builder.setContentText(queued);
        }

        Notification notification = builder.getNotification(); // need to update support lib
        startForeground(VIZ_NOTIFICATION, notification);
        mNM.notify(VIZ_NOTIFICATION, notification);
    }

    private class DownloadListener implements ProgressListener {

        public void onProgressUpdate(final Resource resource, int totalProgress,
                long currentFilesize) {
            // Ideally, all changes to the resource would be done on the UI
            // thread, but setting it here allows for it to marshalled over
            // to the UI when the resource is added to the bundle.
            resource.setCurrentFilesize(currentFilesize);

            updateProgress(resource, totalProgress);
            sendProgressUpdate(resource, totalProgress);
        }

        // not called on the ui thread
        public void onFilesizeUpdate(final Resource r, final long fileSize) {
            // set so the value is sent over on progress update calls
            r.setFilesize(fileSize);

            Thread t = new Thread("UpdateVideoFilesize") {
                @Override
                public void run() {
                    ContentValues map = new ContentValues();
                    map.put(VizContract.Resources.FILESIZE, String.valueOf(fileSize));
                    getContentResolver().update(r.getDownloadUri(), map, null, null);
                }
            };
            Utils.threadStart(t, "Error on filesize update thread");
        }

        public void onCancelled(final Resource r) {
            Thread t = new Thread("CancelStatusChange") {
                @Override
                public void run() {
                    changeDownloadStatus(r, Downloads.Status.PAUSED);
                    done(r);
                }
            };
            Utils.threadStart(t, "Error on cancel status update thread");
        }

        public void onFinish(final Resource resource, boolean success) {
            Log.d("onFinish[" + resource + "," + "success: " + success + "]");
            if (success) {
                Thread t = new Thread("onFinishSuccess") {
                    @Override
                    public void run() {
                        changeDownloadStatus(resource, Downloads.Status.COMPLETE);
                        sendDownloadSuccessMsg(resource);
                        addResource(resource);
                        done(resource);
                    }
                };
                Utils.threadStart(t, "onFinishSuccessThreadError");
            } else {
                Thread t = new Thread("onFinishFail") {
                    @Override
                    public void run() {
                        changeDownloadStatus(resource, Downloads.Status.FAILED);
                        sendDownloadFailedMsg(resource);
                        done(resource);
                    }
                };
                Utils.threadStart(t, "onFinishFailThreadError");
            }
        }
    }

    public interface ProgressListener {
        /**
         * Called on the UI thread so that the UI may update itself if
         * necessary as the download progresses.
         */
        public void onProgressUpdate(Resource resource, int cumulativeProgress,
                long filsize);

        /**
         * Called when the downloader figures out the size of the file that is
         * to be downloaded.  Not called on the UI thread.
         */
        public void onFilesizeUpdate(Resource resource, long fileSize);

        /**
         * Called when the download has completed.  Called on the UI thread.
         */
        public void onFinish(Resource resource, boolean success);

        /**
         * Called if the user cancelled or paused this download.  If this is called,
         * onFinish is not called.  Might not be called on the UI thread!
         */
        public void onCancelled(Resource resource);
    }

    private class DownloadData {
        DownloadTask task;
        int progress;
        final Resource resource;

        DownloadData(Resource resource, DownloadTask task) {
            this.resource = resource;
            this.task = task;
        }

        Resource getResource() {
            return resource;
        }

        DownloadTask getTask() {
            return task;
        }

        void setProgress(int progress) {
            this.progress = progress;
        }

        int getProgress() {
            return progress;
        }
    }
}
