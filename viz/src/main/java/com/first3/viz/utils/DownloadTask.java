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

package com.first3.viz.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;

import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.download.DownloadManager.ProgressListener;
import com.first3.viz.models.Resource;
import com.first3.viz.provider.VizContract;

public class DownloadTask extends AsyncTask<Resource, Integer, Boolean> {
    private Resource mResource;
    private Uri mUri;
    private ProgressListener mListener;
    private int mProgress = 0;
    private String mFailure;
    private static int MAX_BUF_SIZE = 1024 * 4;
    private volatile long mCurrentFilesize = 0;

    public DownloadTask(ProgressListener listener) {
        super();
        mListener = listener;
    }

    public AsyncTask<Resource, Integer, Boolean> run(Resource...param ) {
        if (Utils.isHoneycombOrHigher()) {
            //@TargetApi(11)
            return executeOnExecutor(THREAD_POOL_EXECUTOR, param);
        } else {
            return execute(param);
        }
    }

    @Override
    protected Boolean doInBackground(Resource... params) {
        mResource = params[0];
        mUri = mResource.getDownloadUri();
        try {
            return download();
        } catch (Exception e) {
            Log.w("Download failed: " + e.getLocalizedMessage());
            mFailure = e.getLocalizedMessage();
            return false;
        }
    }

    public String getFailureText() {
        return mFailure;
    }

    /**
     * Returns true if the download was successful.  Throws an IOException
     * if there was an unforseen error.  Returns false if the download was
     * interrupted by the user.
     */
    private boolean download() throws IOException {
        URL url = null;

        long localFileSize = mResource.getFilesizeOnDisk();
        mCurrentFilesize = localFileSize;

        try {
            url = new URL(mResource.getURL());
        } catch (MalformedURLException e) {
            Log.d("malformed url: " + mResource.getURL());
            throw e;
        }

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            Log.d("Could not open connection error");
            throw e;
        }

        urlConnection.setConnectTimeout(5000);
        // handle connection establishment error, timeout, etc

        if (localFileSize > 0) {
            urlConnection.setRequestProperty("Range", "bytes=" + localFileSize + "-");

            String lastModified = mResource.getURLLastModified();
            urlConnection.setRequestProperty("If-Range", lastModified);
            Log.d("Saved Last-Modified URL header: "+lastModified);

            // Check to see that the server honored the range request
            String rangeConfirmation = urlConnection.getHeaderField("Content-Range");
            if (rangeConfirmation==null || rangeConfirmation.startsWith("0-")) {
                Log.d("Resume is not supported by the server");
                mResource.deleteFile();
                localFileSize = mCurrentFilesize = 0;
            } else {
                Log.d("Range confirmation: " + rangeConfirmation);
            }
        } else {
            String lastModified = urlConnection.getHeaderField("Last-Modified");
            // Is there a method for this?
            ContentValues map = new ContentValues();
            map.put(VizContract.Downloads.URL_LASTMODIFIED, lastModified);
            VizApp.getResolver().update(mUri, map, null, null);
        }

        urlConnection.connect();

        String sLength = urlConnection.getHeaderField("Content-Length");
        if (sLength == null) {
            throw new IOException(VizApp.getResString(R.string.download_error_content_length) + ": " +
                    mResource.getURL());
        }

        long fileSizeRemaining = Long.parseLong(sLength);
        float bytesAvailable = Utils.bytesAvailable(mResource.getDownloadDirectory());
        if (fileSizeRemaining == 0) {
            Log.w("Got 0 file size from Content-Length header");
            fileSizeRemaining = 1024*1024*20;
        } else {
            Log.d("remaining to download: " + fileSizeRemaining);
            Log.d("already downloaded: " + localFileSize);
            Log.d("available space: " + bytesAvailable);
            if (fileSizeRemaining > bytesAvailable) {
                String requested = String.format("%.2f", (fileSizeRemaining / (1024.f * 1024.f)));
                String available = String.format("%.2f", (bytesAvailable / (1024.f * 1024.f)));
                throw new IOException(VizApp.getResString(R.string.download_error_nofreespace, requested, available));
            }
        }

        mListener.onFilesizeUpdate(mResource, fileSizeRemaining+localFileSize);

        int bufferSize = 1024*100*2;
        InputStream in = new BufferedInputStream(urlConnection.getInputStream(), bufferSize);

        // TODO: figure out how to go through content resolver
        OutputStream ostream = mResource.getOutputFileStream(true);

        BufferedOutputStream bout = null;
        try {
            bout = new BufferedOutputStream(ostream, bufferSize);
            int len = 0;
            int chunkSize = Long.valueOf(localFileSize+fileSizeRemaining).intValue() / VizContract.Downloads.PROGRESS_MAX_NUM;
            byte[] buffer = new byte[MAX_BUF_SIZE];
            Integer progress = (int) (localFileSize/chunkSize);
            int chunker = 0;

            while ((len = in.read(buffer, 0, MAX_BUF_SIZE)) !=-1) {
                chunker += len;
                bout.write(buffer, 0, len);
                mCurrentFilesize += len;

                if (isCancelled()) {
                    Log.d("isCancelled(uri=" + mUri + ")");

                    // 4.0+ works and 2.3.6 fails. Romain said it was fixed in froyo+.
                    if (Utils.isLowerThanHoneyComb()) {
                        // cancel broken in older builds.  Romain says fixed in Froyo+
                        Log.d("calling onCancelled manually");
                        onCancelled(true);
                    }
                    return false;
                }

                if (chunker >= chunkSize) {
                    progress += 1;
                    chunker = 0;
                    publishProgress(progress);
                }
                if (len == -1) {
                    progress = VizContract.Downloads.PROGRESS_MAX_NUM;
                    publishProgress(progress);
                }
            }
        } finally {
            IOUtilities.closeStream(bout);
            IOUtilities.closeStream(in);
            IOUtilities.closeStream(ostream);
            urlConnection.disconnect();
        }
        return true;
    }

    // Called on the UI thread, triggered by a call to publishProgress
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        mProgress =+ values[0];
        mListener.onProgressUpdate(mResource, mProgress, mCurrentFilesize);
    }

    @Override
    protected void onCancelled(Boolean b) {
        Log.d("(b=" + b + ")");
        mListener.onCancelled(mResource);
    }

    // Runs on the UI thread before doInBackground(Params...)
    @Override
    protected void onPreExecute() {
    }

    // On UI thread after task has completed
    @Override
    protected void onPostExecute(Boolean success) {
        Log.d("onPostExecute(uri=" + mUri + ", success=" + success + ")");
        mListener.onFinish(mResource, success);
    }
}
