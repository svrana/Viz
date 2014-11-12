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

package com.first3.viz.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.CursorIndexOutOfBoundsException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.download.DownloadManager;
import com.first3.viz.models.Resource;
import com.first3.viz.provider.VizContract;
import com.first3.viz.provider.VizContract.DownloadsColumns;
import com.first3.viz.provider.VizContract.Resources;
import com.first3.viz.utils.FragmentParent;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Maps;
import com.first3.viz.utils.Utils;

public class Downloads extends FragmentParent implements ServiceConnection,
       LoaderManager.LoaderCallbacks<Cursor> {
    private final Map<Resource, DownloadData> mDownloadMap = Maps.newHashMap();
    private final List<Message> mMessages = new ArrayList<Message>();
    private Messenger mService;
    private ListView mDownloadList;
    private DownloadsCursorAdapter mAdapter;
    private Menu mMenu;
    private TextView mEmptyList;
    private volatile boolean mIsBound = false;
    private static final int LOADER_ID = 3;
    private static final String DOWNLOAD_TITLE = "title";
    private static final String DOWNLOAD_FAILURE = "failure";
    private static final String DOWNLOAD_RESOURCE = "resource";
    private static final String FIRST_DOWNLOAD = "FirstDownload";
    private static final String FILESIZE_SEP = "  /";

    /**
     * Target we publish to retrieve messages from the service.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Handler of incoming messages from DownloadManager service.  Run on the
     * UI thread.
     */
    private class IncomingHandler extends Handler {
        Resource resource;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DownloadManager.MSG_STATUS_DOWNLOAD_PROGRESS:
                    resource = getResourceFromMessage(msg);
                    int cumulativeProgress = msg.arg1;
                    updateProgress(resource, cumulativeProgress);
                    break;
                case DownloadManager.MSG_STATUS_DOWNLOAD_SUCCESS:
                    resource = getResourceFromMessage(msg);
                    removeDownloadData(resource);
                    break;
                case DownloadManager.MSG_STATUS_DOWNLOAD_FAILED:
                    resource = getResourceFromMessage(msg);
                    String failureText = getFailureTextFromMessage(msg);
                    downloadFailed(resource, failureText);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private Resource getResourceFromMessage(Message msg) {
        Bundle b = msg.getData();
        b.setClassLoader(VizApp.getClsLoader());
        return b.getParcelable(DownloadManager.RESOURCE);
    }

    private String getFailureTextFromMessage(Message msg) {
        Bundle b = msg.getData();
        b.setClassLoader(VizApp.getClsLoader());
        return b.getString(DownloadManager.FAILURE_TEXT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d();

        setHasOptionsMenu(true);

        // Make sure the adapter has been created before binding as progress
        // reports depend on it being non-null
        fillData();

        // we want to go ahead and bind whenever the UI is showing so we can
        // keep the UI up-to-date on current download progress
        bindDownloadService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VizApp.getContext().unbindService(this);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.downloads, null);

        mDownloadList = (ListView) v.findViewById(R.id.downloadsListView);
        mDownloadList.setAdapter(mAdapter);

        // now let's get a loader or reconnect to existing one
        getLoaderManager().initLoader(LOADER_ID, null, this);

        mEmptyList = (TextView) v.findViewById(android.R.id.empty);
        mEmptyList.setVisibility(View.INVISIBLE);

        mDownloadList.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, final int position, long arg) {
                final Uri downloadUri = getDownloadUriFromPosition(position);
                VizContract.Downloads.Status status = getDownloadStatus(position);
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                final Resource resource = Resource.fromCursor(cursor);
                final AlertDialog dialog;
                String downloadTitle = cursor.getString(cursor.getColumnIndex(DownloadsColumns.TITLE));

                switch (status) {
                    case QUEUED:
                    case INPROGRESS:
                        if (isDownloading(position)) {
                            dialog = new AlertDialog.Builder(getActivity())
                                    .setIcon(R.drawable.ic_launcher)
                                    .setTitle(getString(R.string.downloads_pauseDownload))
                                    .setMessage(downloadTitle)
                                    .setPositiveButton(R.string.download_pause,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    pauseDownload(resource);
                                                }
                                            })
                                    .setNegativeButton(R.string.download_continue,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    Log.d("Download continuing");
                                                }
                                            }).create();
                            dialog.show();

                            break;
                        } else {
                            changeDownloadStatus(downloadUri, VizContract.Downloads.Status.FAILED);
                            // there was a problem, so carry on to the 'FAILED' block
                        }
                    case PAUSED: // Paused by the user
                    case FAILED: // Due to crash or connectivity loss
                        String[] choices = new String[] { VizApp.getResString(R.string.downloads_resume_ok),
                                                          VizApp.getResString(R.string.downloads_remove) };
                        dialog = new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_launcher)
                                    .setTitle(downloadTitle)
                                    .setItems(choices, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (which == 0) {
                                                resource.setDownloadUri(downloadUri);
                                                queue(resource);
                                            } else if (which == 1) {
                                                removeDownloadData(resource);
                                                deleteDownloadThread(downloadUri);
                                            }
                                        }})
                                    .create();
                        dialog.show();
                        break;
                    case COMPLETE:
                        choices = new String[] { VizApp.getResString(R.string.download_play),
                                                 VizApp.getResString(R.string.downloads_remove) };
                        dialog = new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_launcher)
                                    .setTitle(downloadTitle)
                                    .setItems(choices, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (which == 0) {
                                                Uri uri = getUriFromDownloadPosition(position);
                                                getActivityDelegate().play(uri);
                                            } else if (which == 1) {
                                                deleteDownloadThread(downloadUri);
                                            }
                                        }})
                                    .create();
                        dialog.show();
                        break;
                }
            }
        });

        mDownloadList.setOnItemLongClickListener(
                new android.widget.AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapter,
                            View view, int position, long id) {
                        Uri uri = getUriFromDownloadPosition(position);
                        getActivityDelegate().play(uri);
                        return true;
                    }
                });

        return v;
    }

    /**
     * If there's a Resource for the corresponding download at the position
     * specified, return it, otherwise return the downloadUri for that
     * position.
     *
     * It's better to play a resource than a download uri, as resources can
     * be resumed.
     */
    private Uri getUriFromDownloadPosition(int position) {
        Uri uri = getDownloadUriFromPosition(position);
        String downloadId = VizContract.Downloads.getDownloadId(uri);

        String selection = Resources.DOWNLOAD_ID + "=?";
        Cursor cursor = VizApp.getResolver().query(Resources.CONTENT_URI,
                new String[] { Resources._ID, Resources.DOWNLOAD_ID }, selection,
                new String[] { downloadId }, Resources.DEFAULT_SORT);
        if (cursor.moveToFirst()) {
            int resourceId = cursor.getInt(cursor.getColumnIndex(Resources._ID));
            uri = Resources.buildResourceUri(resourceId);
        }
        cursor.close();
        return uri;
    }

    private Uri getDownloadUriFromPosition(int position) {
        String downloadId = String.valueOf(mDownloadList.getItemIdAtPosition(position));
        return VizContract.Downloads.buildDownloadUri(downloadId);
    }

    private DownloadData createDownloadData(Resource r) {
        DownloadData data = mDownloadMap.get(r);
        if (data == null) {
            data = new DownloadData();
            mDownloadMap.put(r, data);
        }
        data.setProgress(0);
        return data;
    }

    private void removeDownloadData(Resource r) {
        mDownloadMap.remove(r);
    }

    private void updateProgress(Resource r, int cumulativeProgress) {
        DownloadData data = mDownloadMap.get(r);
        if (data == null) {
            data = createDownloadData(r);
        }

        data.setProgress(cumulativeProgress)
            .setCurrentFilesize(r.getCurrentFilesize())
            .setPercentComplete(r.getPercentComplete());

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private int getProgress(Resource r) {
        DownloadData data = mDownloadMap.get(r);
        if (data == null) {
            return 0;
        }
        return data.getProgress();
    }

    private int getPercentComplete(Resource r) {
        DownloadData data = mDownloadMap.get(r);
        if (data == null) {
            return 0;
        }
        return data.getPercentComplete();
    }

    private long getCurrentFilesize(Resource r) {
        DownloadData data = mDownloadMap.get(r);
        if (data == null) {
            return 0;
        }
        return data.getCurrentFilesize();
    }

    private void downloadFailed(Resource resource, String failureText) {
        HashMap<String, Object> map = Maps.newHashMap();
        map.put(DOWNLOAD_TITLE, resource.getTitle());
        map.put(DOWNLOAD_FAILURE, failureText);
        map.put(DOWNLOAD_RESOURCE, resource);
        // seems like we shouldn't have to do this anymore b/c we know
        // we're on the ui thread, but keeping it
        sendMessage(ActivityDelegate.MSG_DOWNLOADS,
                ActivityDelegate.MSG_DOWNLOADS_FAILURE_SHOW, map);
    }

    private void fillData() {
        String[] from = new String[] {
            DownloadsColumns.FILENAME,
            DownloadsColumns.TITLE,
            DownloadsColumns.FILESIZE,
            DownloadsColumns.CURRENT_FILESIZE,
            DownloadsColumns.PERCENT_COMPLETE
        };
        int[] to = new int[] {
            R.id.downloadFilename,
            R.id.downloadTitle,
            R.id.downloadFilesize,
            R.id.currentFilesize,
            R.id.percentComplete
        };

        mAdapter = new DownloadsCursorAdapter(getActivity(), R.layout.download_list,
                null, from, to, CursorAdapter.NO_SELECTION);
        mAdapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == DownloadsQuery.FILESIZE) {
                    final TextView fileSizeView = (TextView) view;
                    final String sFileSize = cursor.getString(DownloadsQuery.FILESIZE);
                    if (TextUtils.isEmpty(sFileSize) || Long.valueOf(sFileSize) == 0) {
                        view.setVisibility(View.INVISIBLE);
                    } else {
                        Long fileSize = Long.valueOf(sFileSize);
                        fileSizeView.setText(Utils.filesize_toReadableForm(fileSize, false));
                        view.setVisibility(View.VISIBLE);
                    }
                    return true;
                }
                if (columnIndex == DownloadsQuery.FILENAME) {
                    final TextView filenameView = (TextView) view;
                    final String title = cursor.getString(DownloadsQuery.FILENAME);
                    filenameView.setText(title);
                    return true;
                }
                if (columnIndex == DownloadsQuery.TITLE) {
                    final TextView titleView = (TextView) view;
                    final String title = cursor.getString(DownloadsQuery.TITLE);
                    titleView.setText(title);
                    return true;
                }
                if (columnIndex == DownloadsQuery.CURRENT_FILESIZE) {
                    final TextView filesizeView = (TextView) view;
                    final String sFileSize = cursor.getString(DownloadsQuery.CURRENT_FILESIZE);
                    int percentComplete = cursor.getInt(DownloadsQuery.PERCENT_COMPLETE);
                    if (TextUtils.isEmpty(sFileSize) || Long.valueOf(sFileSize) == 0 || percentComplete == 100) {
                        view.setVisibility(View.GONE);
                    } else {
                        Long fileSize = Long.valueOf(sFileSize);
                        filesizeView.setText(Utils.filesize_toReadableForm(fileSize, false) + FILESIZE_SEP);
                        view.setVisibility(View.VISIBLE);
                    }
                    return true;
                }
                if (columnIndex == DownloadsQuery.PERCENT_COMPLETE) {
                    int percentComplete = cursor.getInt(DownloadsQuery.PERCENT_COMPLETE);
                    final TextView percentView = (TextView) view;
                    if (percentComplete != 0) {
                        percentView.setText(percentComplete + "%");
                        percentView.setVisibility(View.VISIBLE);
                    } else {
                        percentView.setVisibility(View.INVISIBLE);
                    }
                    return true;
                }

                Log.d("Did not handle column index: " + columnIndex);
                return false;
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d();
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.downloads_menu, menu);
        mMenu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d();
        if (item.getItemId() == R.id.menu_clear) {
                Log.d("menu clear selected");
                final StringBuilder selection = new StringBuilder();
                selection.append(VizContract.Downloads.STATUS).append("=")
                         .append(VizContract.Downloads.Status.COMPLETE.valueOf());
                selection.append(" OR ")
                         .append(VizContract.Downloads.STATUS).append("=")
                         .append(VizContract.Downloads.Status.FAILED.valueOf());
                new Thread("RemoveDownloads") {
                    @Override
                    public void run() {
                        VizApp.getResolver().delete(VizContract.Downloads.CONTENT_URI, selection.toString(), null);
                    }
                }.start();
                return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.d();
        if (mAdapter == null) {
            return;
        }
        clearButtonUpdateState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("bla", "Value1");
        Log.d();
        super.onSaveInstanceState(outState);
    }

    private void clearButtonUpdateState() {
        boolean enabled = (mAdapter != null) ? (mAdapter.getCount() > 0) : false;
        Log.d("(enabled=" + enabled + ")");

        // TODO: this should only be enabled there are non active downloads present
        //       with either a COMPLETE or FAILED status.  Other failed downloads
        //       have to be cancelled seperately (their status is IN PROGRESS
        //       b/c of an app crash)
        if (mMenu != null) {
            MenuItem item = mMenu.findItem(R.id.menu_clear);
            if (item != null) {
                item.setEnabled(enabled);
            }
        }

        if (mEmptyList != null) {
            if (enabled) {
                mEmptyList.setVisibility(View.INVISIBLE);
            } else {
                mEmptyList.setVisibility(View.VISIBLE);
            }
        }
    }

    /** This should be done at app start so updates the UI happen immediately */
    public void bindDownloadService() {
        if (!mIsBound) {
            Log.d("binding to download service");
            VizApp.getContext().bindService(new Intent(VizApp.getContext(), DownloadManager.class),
                    Downloads.this, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
        } else {
            Log.d("already bound..");
        }
    }

    /**
     * Create the service if it's not running service hasn't been created yet,
     * create it, otherwise just use the existing binding to communicate the
     * new download.
     */
    private void sendMsg(Message msg) {
        if (mIsBound) {
            Log.d("Bound: sending message immediately");
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.w("Failed to communicate with service: " + e);
                // shouldn't we rebind at this point??
            }
        } else {
            Log.d("Not bound: queueing message for sending later");
            // Add the msg to the queue before bind, so we pass it to the
            // DownloadService on bind
            synchronized (Downloads.class) {
                mMessages.add(msg);
            }
        }
    }

    private void sendDownloadMessage(Resource resource) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(DownloadManager.RESOURCE, resource);
        Message msg = Message.obtain(null, DownloadManager.MSG_CMD_DOWNLOAD_INITIATE);
        msg.setData(bundle);

        sendMsg(msg);
    }

    public void queue(Resource r) {
        Log.d("(resource=" + r + ", uri=" + r.getDownloadUri() + ")");

        // the service may have shut itself down, killing the binding, so make
        // sure we get UI updates by binding again
        bindDownloadService();

        // starting the service makes sure the service lasts even after the
        // binding has been closed which will occur when the UI is destroyed
        VizApp.getContext().startService(new Intent(VizApp.getContext(), DownloadManager.class));

        createDownloadData(r);

        sendDownloadMessage(r);

        boolean firstDownload = VizApp.getPrefs().getBoolean(FIRST_DOWNLOAD, true);
        if (firstDownload) {
            VizApp.getPrefs().edit().putBoolean(FIRST_DOWNLOAD, false).commit();
            ActivityDelegate ad = getActivityDelegate();
            if (ad != null) {
                ad.switchToTab(3);
            }
        }
    }

    public VizContract.Downloads.Status getDownloadStatus(int position) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        // this is cruft left-over from a previous bug which I do not think
        // exists anymore b/c we no longer remove a Download behind the users
        // back (we used to do this when the download failed)
        if (cursor.getCount() == 0) {
            return VizContract.Downloads.Status.FAILED;
        }
        int statusInt = VizContract.Downloads.Status.FAILED.valueOf();
        try {
            statusInt = cursor.getInt(cursor.getColumnIndex(DownloadsColumns.STATUS));
        } catch(CursorIndexOutOfBoundsException e) {
            Log.w("threw a cursorIndexOfBoundsException");
        }
        return VizContract.Downloads.Status.fromInt(statusInt);
    }

    private boolean isDownloading(int position) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        Resource r = Resource.fromCursor(cursor);
        return (!Resource.isNull(r)) && mDownloadMap.containsKey(r);
    }

    private void changeDownloadStatus(Uri downloadUri, VizContract.Downloads.Status status) {
        ContentValues map = new ContentValues();
        map.put(VizContract.Downloads.STATUS, status.valueOf());
        int rows = VizApp.getResolver().update(downloadUri, map, null, null);
        if (rows != 1) {
            Log.e("Failed to update status of " + downloadUri + " to " + status);
        }
    }

    private void deleteDownloadThread(final Uri downloadUri) {
        new Thread("DownloadDeleteThread") {
            @Override
            public void run() {
                deleteDownload(downloadUri);
            }
        }.start();
    }

    /**
     * Tells the provider to delete the download row.
     *
     * Careful: if the status of the download is not FAILED, the downloaded
     * contents will not be removed.
     */
    private void deleteDownload(Uri downloadUri) {
        VizApp.getResolver().delete(downloadUri, null, null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), VizContract.Downloads.CONTENT_URI,
                DownloadsQuery.PROJECTION, null, null, VizContract.Downloads.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
        clearButtonUpdateState();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    private interface DownloadsQuery {
         String[] PROJECTION = { BaseColumns._ID, DownloadsColumns.FILENAME,
             DownloadsColumns.TITLE, DownloadsColumns.FILESIZE, DownloadsColumns.PROGRESS,
             DownloadsColumns.MAX_PROGRESS, DownloadsColumns.STATUS, DownloadsColumns.CONTENT,
             DownloadsColumns.DIRECTORY, DownloadsColumns.URL, DownloadsColumns.URL_LASTMODIFIED,
             DownloadsColumns.CURRENT_FILESIZE, DownloadsColumns.PERCENT_COMPLETE
         };

         //int _ID = 0;
         int FILENAME = 1;
         int TITLE = 2;
         int FILESIZE = 3;
         int CURRENT_FILESIZE = 11;
         int PERCENT_COMPLETE = 12;
    }

    @SuppressWarnings("unchecked")
    public void showDownloadFailedDialog(Context context, Object obj) {
        HashMap<String, Object> map = (HashMap<String, Object>) obj;
        String title = (String) map.get(DOWNLOAD_TITLE);
        String failure = (String) map.get(DOWNLOAD_FAILURE);
        final Resource resource = (Resource) map.get(DOWNLOAD_RESOURCE);
        new AlertDialog.Builder(context)
            .setIcon(R.drawable.ic_launcher)
            .setTitle(title)
            .setMessage(VizApp.getResString(R.string.download_failed) + ": " + failure)
            .setNeutralButton(R.string.download_failed_accept,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            removeDownloadData(resource);
                        }
                    })
        .create()
        .show();
    }

    private void pauseDownload(Resource resource) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(DownloadManager.RESOURCE, resource);
        Message msg = Message.obtain(null, DownloadManager.MSG_CMD_DOWNLOAD_PAUSE);
        msg.setData(bundle);
        VizApp.getContext().startService(new Intent(VizApp.getContext(), DownloadManager.class));
        sendMsg(msg);

        removeDownloadData(resource);
    }

    private class DownloadsCursorAdapter extends SimpleCursorAdapter {
        public DownloadsCursorAdapter(Context context, int layout, Cursor c,
                String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            updateDownloadProgressBar(view, cursor);
        }

        private void updateDownloadProgressBar(View view, Cursor cursor) {
            int statusInt = cursor.getInt(cursor.getColumnIndex(DownloadsColumns.STATUS));
            Resource r = Resource.fromCursor(cursor);
            int progress = getProgress(r);
            VizContract.Downloads.Status status = VizContract.Downloads.Status.fromInt(statusInt);

            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.downloadProgessBar);
            if (progress != 0 && status == VizContract.Downloads.Status.INPROGRESS) {
                int max_progress = cursor.getInt(cursor.getColumnIndex(DownloadsColumns.MAX_PROGRESS));
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setMax(max_progress);
                progressBar.setProgress(progress);
            } else {
                progressBar.setVisibility(View.GONE);
            }

            // Current filesize is provided by the database when the status is
            // failed or paused, and by DownloadData when the download is in
            // progress (so we were not continually writing to the db). How
            // about when the download is complete?
            if (status == VizContract.Downloads.Status.INPROGRESS) {
                long currentFilesize = getCurrentFilesize(r);
                if (currentFilesize == 0) {
                    // let the value come from the DB
                } else {
                    TextView currentSizeView  = (TextView) view.findViewById(R.id.currentFilesize);
                    Long currentFileSize = Long.valueOf(currentFilesize);
                    currentSizeView.setVisibility(View.VISIBLE);
                    currentSizeView.setText(Utils.filesize_toReadableForm(currentFileSize, false) + FILESIZE_SEP);
                }

                int percentComplete = getPercentComplete(r);
                if (percentComplete != 0) {
                    TextView percentCompleteView = (TextView) view.findViewById(R.id.percentComplete);
                    percentCompleteView.setText(percentComplete + "%");
                    percentCompleteView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void sendQueuedMessages() {
        Log.d();
        synchronized(Downloads.class) {
            for(Message m : mMessages) {
                sendMsg(m);
            }
            mMessages.clear();
        }
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.d("connected to download service");
        // This is called when the connection with the service has been
        // established, giving us the object we can use to
        // interact with the service.  We are communicating with the
        // service using a Messenger, so here we get a client-side
        // representation of that from the raw IBinder object.
        mService = new Messenger(service);
        mIsBound = true;

        Message msg = Message.obtain(null, DownloadManager.MSG_CMD_CLIENT_REGISTER);
        msg.replyTo = mMessenger;
        sendMsg(msg);

        sendQueuedMessages();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        Log.d("disconnected from download service");
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        mService = null;
        mIsBound = false;
    }

    private class DownloadData {
        int progress = 0;
        long currentFilesize = 0;
        int percentComplete = 0;

        int getProgress() {
            return progress;
        }

        long getCurrentFilesize() {
            return currentFilesize;
        }

        int getPercentComplete() {
            return percentComplete;
        }

        DownloadData setProgress(int progress) {
            this.progress = progress;
            return this;
        }

        DownloadData setCurrentFilesize(long size) {
            currentFilesize = size;
            return this;
        }

        DownloadData setPercentComplete(int percentComplete) {
            this.percentComplete = percentComplete;
            return this;
        }
    }
}
