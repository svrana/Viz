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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import org.jsoup.Jsoup;

import com.first3.viz.Config;
import com.first3.viz.Constants;
import com.first3.viz.Preferences;
import com.first3.viz.R;
import com.first3.viz.provider.VizContract.Resources;
import com.first3.viz.VizApp;

public class VizUtils {
    public static boolean isAmazonVersion() {
        return Config.isAmazonVersion();
    }

    /**
     * The directory of video storage used when the user wants the videos to
     * be automatically picked up by the media scanner (and visible in the
     * Gallery).
     */
    public static File getVideosPublicDir() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    /**
     * @return The canonical string representation of {@link #getVideosPublicDir()}
     */
    public static String getVideosPublicPath() {
        return Utils.getCanonicalPath(getVideosPublicDir());
    }

    /**
     * The directory used to store video thumbnails.  This directory is owned
     * by the application, so when the user uninstalls the thumbnails are
     * deleted.
     */
    public static File getVideosThumbnailDir() {
        return VizApp.getContext().getExternalFilesDir("thumbnails");
    }

    /**
     * @return The canonical string representation of {@link #getVideosThumbnailDir()}
     */
    public static String getVideosThumbnailPath() {
        return Utils.getCanonicalPath(getVideosThumbnailDir());
    }

    /**
     * The default storage location of videos for the Free version of Viz.
     *
     * This is the Viz app directory, so the videos are effectively hidden from the
     * Media Scanner (i.e., Gallery).  This directory was chosen b/c I thought
     * it best to remove all the downloaded files if the user uninstalls Viz.
     */
    public static File getVideosPrivateDir() {
        return VizApp.getContext().getExternalFilesDir("Videos");
    }

    /**
     * @return The canonical string representation of {@link #getVideosPrivateDir()}
     */
    public static String getVideosPrivatePath() {
        return Utils.getCanonicalPath(getVideosPrivateDir());
    }

    /**
     * Return the directory where downloads should be stored according to user
     * preferences, whether this is Viz premium or not, etc.
     */
    public static File getDownloadDir() {
        return new File(VizApp.getPrefs().getString(Preferences.DOWNLOAD_DIRECTORY, getVideosPrivatePath()));
    }

    /**
     * @return The canonical string representation of {@link #getDownloadDir()}
     */
    public static String getDownloadPath() {
        return Utils.getCanonicalPath(getDownloadDir());
    }

    /**
     * Updates the location where user downloads should be stored.
     */
    public static void setDownloadDir(File dir) {
        Log.i("setting download directory to [" + dir + "]");
        SharedPreferences.Editor editor = VizApp.getPrefs().edit();
        editor.putString(Preferences.DOWNLOAD_DIRECTORY, dir.toString());
        editor.commit();
    }

    public static File getVideoFile(String dir, String filename) {
        return new File(dir, filename);
    }

    public static File getVideoFile(String filename) {
        return new File(getDownloadDir(), filename);
    }

    public static File getPrivateVideoFile(String filename) {
        return new File(getVideosPrivateDir(), filename);
    }

    public static File getPublicVideoFile(String filename) {
        return new File(getVideosPublicDir(), filename);
    }

    public static String getPublicVideoFilename(String filename) {
        return getPublicVideoFile(filename).toString();
    }

    public static boolean saveVideosPublicly() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(VizApp.getContext());
        return prefs.getBoolean(Preferences.SHARE_VIDEOS, false);
    }

    public static void informMediaScanner(final String filename) {
        MediaScannerConnection.scanFile(VizApp.getContext(), new String[] { filename }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        if (uri != null) {
                            Log.d("MediaScanner.onScanCompleted(filename=" + filename + ", uri=" + uri + ")");
                        }
                    }
                });
    }

    public static void unlockVideo(final Uri resourceUri, final File videoFile) {
        Log.mi("unlocking uri: " + resourceUri + " video: " + videoFile);

        Thread t = new Thread("unlockVideos") {
            @Override
            public void run() {
                BufferedInputStream is;
                BufferedOutputStream os;

                File publicVideo = getPublicVideoFile(videoFile.getName());

                try {
                    is = new BufferedInputStream(new FileInputStream(videoFile));
                } catch (FileNotFoundException e) {
                    Log.w("Invalid input file: " + videoFile.toString());
                    return;
                }

                try {
                    os = new BufferedOutputStream(new FileOutputStream(publicVideo));
                } catch (FileNotFoundException e) {
                    Log.w("Invalid output file: " + publicVideo.toString());
                    IOUtilities.closeStream(is);
                    return;
                }

                try {
                    IOUtilities.copy(is, os);
                } catch (IOException e) {
                    IOUtilities.closeStream(is);
                    IOUtilities.closeStream(os);
                    Log.w("Error copying file");
                    return;
                }

                IOUtilities.closeStream(is);
                IOUtilities.closeStream(os);

                videoFile.delete();

                // doing this seperate just in case there's a bug in mediascanner
                ContentValues m = new ContentValues();
                m.put(Resources.DIRECTORY, getVideosPublicPath());
                VizApp.getResolver().update(resourceUri, m, null, null);

                informMediaScanner(publicVideo.toString());
	    }
	};
	t.start();
    }

    public static boolean isPublicDir(String dir) {
        if (TextUtils.isEmpty(dir)) {
            return false;
        }
        if (getVideosPublicPath().equals(dir)) {
            return true;
        }
        return false;
    }

    public static boolean isExtStorageAvailable() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    public static void showVizThumbnailInTray(Activity a) {
        Log.d();
        a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    public static void hideVizThumbnailInTray(Activity a) {
        Log.d();
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    public static String normalizeFilename(String filename, String extension) {
        filename = filename.trim();
        filename = filename.replaceAll("[^\\w]", "").toLowerCase();

        // use the first and last half of the string to create the filename.
        // This makes downloading episodic content easier, as the variable
        // part of the filename is often at the end of the string.
        if (filename.length() > (Constants.MAX_FILENAME_LEN - 3)) {
            int halfLength = (Constants.MAX_FILENAME_LEN - 3) / 2;
            String firstHalf = filename.substring(0, halfLength);
            String lastHalf = filename.substring(filename.length() - halfLength);
            filename = firstHalf + lastHalf;
        }
        return filename.toString() + extension;
    }

    /*
     * Remove html quoting like amp;, etc.
     */
    public static String normalizeTitle(String title) {
        return Jsoup.parse(title).text();
    }

    public static int percentComplete(long downloaded, long total) {
        if (downloaded == 0 || total == 0) {
            return 0;
        }
        return (int)((downloaded * 100.0f) / total);
    }

    public static String getVersionName() {
        try {
            String packageName = VizApp.getContext().getPackageName();
            return VizApp.getContext().getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return "";
    }
}
