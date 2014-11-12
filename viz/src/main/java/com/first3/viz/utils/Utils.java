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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.view.Window;

public class Utils {
    public static boolean isExtStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static String getVersion(Context context) {
        String version;
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA).versionName;
        } catch (NameNotFoundException e) {
            version = "UnknownVersion";
        }
        return version;
    }

    public static boolean isHoneycombOrHigher() {
        // Can use static final constants like HONEYCOMB, declared in later
        // versions of the OS since they are inlined at compile time. This is guaranteed
        // behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isGingerBreadMROrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1;
    }

    public static boolean isGingerbreadOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean isFroyoOrLower() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO;
    }

    public static boolean isLowerThanHoneyComb() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isLowerThanKitkat() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
    }

    public static boolean isKitkatOrHigher() {
        return !isLowerThanKitkat();
    }

    public static boolean isVersionTwo() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isJellyBeanMR1OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static String filesize_toReadableForm(Long fileSize, boolean pad) {
        float gbyte_val = fileSize/(1024F*1024*1024);
        float mbyte_val = fileSize/(1024F*1024);
        float kbyte_val = fileSize/1024F;
        // longest possible string size is from a filesize:
        // 124.21 MB - a minium of a 6 characters for the number

        if (gbyte_val > 1) {
            if (pad) {
                return String.format("%6.2f GB", gbyte_val);
            } else {
                return String.format("%.2f GB", gbyte_val);
            }
        } else if (mbyte_val > 1) {
            if (pad) {
                return String.format("%6.2f MB", mbyte_val);
            } else {
                return String.format("%.2f MB", mbyte_val);
            }

        } else if (kbyte_val > 1) {
            // precision looks weird on kilobytes
            if (pad) {
                return String.format("%6.0f KB", kbyte_val);
            } else {
                return String.format("%.0f KB", kbyte_val);
            }
        } else {
            if (pad) {
                // ..and stupid on bytes
                return String.format("%6.0f B ", fileSize);
            } else {
                return fileSize.toString() + "B";
            }
        }
    }

    public static String msecs_toReadableForm(int msecs) {
        int seconds = msecs/1000;
        int hours = seconds/3600;
        seconds = seconds%3600;
        int minutes = seconds/60;
        seconds = seconds%60;
        String duration;
        if (hours > 0) {
            duration = String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            duration = String.format("%d:%02d", minutes, seconds);
        } else {
            duration = String.format(":%d", seconds);
        }
        return duration;
    }

    public static float bytesAvailable(File path) {
        if (!path.exists()) {
            return 0;
        }
        StatFs stat = new StatFs(path.getPath());
        return (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
    }

    /**
     * Return the number of megabytes available at the specified path.
     */
    public static float mbAvailable(File path) {
        return bytesAvailable(path) / (1024.f * 1024.f);
    }

    /**
     * Get the memory class of this device (approx. per-app memory limit)
     *
     * @param context
     * @return
     */
    public static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService(
                        Context.ACTIVITY_SERVICE)).getMemoryClass();
    }

    public static String getCanonicalPath(File f) {
        if (f == null) {
            Log.mw("null file specified");
            return "";
        }
        try {
            return f.getCanonicalPath();
        } catch(IOException e) {
            Log.w("Invalid path: " + f);
            return f.toString();
        }
    }

    public static void maximizeDialog(Activity activity, View view) {
        Rect displayRectangle = new Rect();
        Window window = activity.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        view.setMinimumHeight(displayRectangle.height());
        view.setMinimumWidth(displayRectangle.width());
    }

    public static void threadStart(Thread t, String errorMessage) {
        try {
            t.start();
        } catch(Throwable th) {
            Log.e(errorMessage);
        }
    }

    public static boolean directoryCreate(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.exists()) {
            dir.mkdir();
            if (!dir.isDirectory()) {
                return false;
            }
        }

        return dir.canWrite();
    }

    public static URL urlFromString(String sURL) {
        URL url = null;

        if (sURL == null || sURL.length() == 0) {
            return null;
        }

        try {
            url = new URL(sURL);
        } catch (MalformedURLException e) {
            Log.d("malformedurlexception " + sURL);
        }
        return url;
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, 100, blob);
        return blob.toByteArray();
    }
}
