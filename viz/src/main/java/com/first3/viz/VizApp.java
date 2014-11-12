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

package com.first3.viz;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

public class VizApp extends Application {
    private static Application sContext;

    @Override
    public void onCreate() {
        sContext = this;
    }

    public static Context getContext() {
        return sContext;
    }

    public static String getResString(int resId) {
        return getContext().getString(resId);
    }

    public static String getResString(int resId, Object... formatArgs) {
        return getContext().getString(resId, formatArgs);
    }

    public static LayoutInflater getInflator() {
        return (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    public static ContentResolver getResolver() {
        return getContext().getContentResolver();
    }

    public static Looper getLooper() {
        Looper looper = Looper.getMainLooper();
        if (looper == null) {
            looper = Looper.myLooper();
        }
        if (looper == null) {
            throw new RuntimeException("Error creating looper");
        }
        return looper;
    }

    public static Handler getHandler() {
        return new Handler(getLooper());
    }

    public static ClassLoader getClsLoader() {
        return sContext.getClassLoader();
    }

    public static String getVizPackageName() {
        return sContext.getPackageName();
    }
}
