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

import com.first3.viz.BuildConfig;

/**
 * Basic logging that adds filename, method and line number to logs before
 * sending them to Android.
 */
public final class Log {
    private static final String TAG = "Viz";
    private final static int depth = 4;

    public interface Level {
        static final int VERBOSE = 2;
        static final int DEBUG = 3;
        static final int INFO = 4;
        static final int WARN = 5;
        static final int ERROR = 6;
    }

    private static int mLogLevel = BuildConfig.DEBUG ? Level.VERBOSE : Level.WARN;

    private Log() {
    }


    /*
     * Show the file, method and line number without a message with the
     * default tag.
     */

    public static final void d() {
        if (mLogLevel <= Level.DEBUG) {
            logger(Level.DEBUG, TAG, null, true);
        }
    }

    public static final void v() {
        if (mLogLevel <= Level.VERBOSE) {
            logger(Level.VERBOSE, TAG, null, true);
        }
    }


    /*
     * Show the file and line number with the default tag.
     */

    public static final void d(String msg) {
        if (mLogLevel <= Level.DEBUG) {
            logger(Level.DEBUG, TAG, msg, false);
        }
    }

    public static final void e(String msg) {
        if (mLogLevel <= Level.ERROR) {
            logger(Level.ERROR, TAG, msg, false);
        }
    }

    public static final void w(String msg) {
        if (mLogLevel <= Level.WARN) {
            logger(Level.WARN, TAG, msg, false);
        }
    }

    public static final void i(String msg) {
        if (mLogLevel <= Level.INFO) {
            logger(Level.INFO, TAG, msg, false);
        }
    }

    public static final void v(String msg) {
        if (mLogLevel <= Level.VERBOSE) {
            logger(Level.VERBOSE, TAG, msg, false);
        }
    }

    /*
     * Show the file and line number with a custom tag.
     */

    public static final void d(String tag, String msg) {
        if (mLogLevel <= Level.DEBUG) {
            logger(Level.DEBUG, tag, msg, false);
        }
    }

    public static final void w(String tag, String msg) {
        if (mLogLevel <= Level.WARN) {
            logger(Level.WARN, tag, msg, false);
        }
    }

    public static final void i(String tag, String msg) {
        if (mLogLevel <= Level.INFO) {
            logger(Level.INFO, tag, msg, false);
        }
    }

    public static final void v(String tag, String msg) {
        if (mLogLevel <= Level.VERBOSE) {
            logger(Level.VERBOSE, tag, msg, false);
        }
    }

    public static final void e(String tag, String msg) {
        if (mLogLevel <= Level.ERROR) {
            logger(Level.ERROR, tag, msg, false);
        }
    }

    /*
     * Show the file, method name and line number with the default tag.
     */

    public static final void md(String msg) {
        if (mLogLevel <= Level.DEBUG) {
            logger(Level.DEBUG, TAG, msg, true);
        }
    }

    public static final void me(String msg) {
        if (mLogLevel <= Level.ERROR) {
            logger(Level.ERROR, TAG, msg, true);
        }
    }

    public static final void mw(String msg) {
        if (mLogLevel <= Level.WARN) {
            logger(Level.WARN, TAG, msg, true);
        }
    }

    public static final void mi(String msg) {
        if (mLogLevel <= Level.INFO) {
            logger(Level.INFO, TAG, msg, true);
        }
    }

    public static final void mv(String msg) {
        if (mLogLevel <= Level.VERBOSE) {
            logger(Level.VERBOSE, TAG, msg, true);
        }
    }

    private static void logger(int level, String tag, String msg, boolean showMethodName) {
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();

        if (msg == null) {
            msg = "";
        }

        if (level == Level.WARN) {
            android.util.Log.w(tag, getTrace(ste, showMethodName) + msg);
        } else if (level == Level.ERROR) {
            android.util.Log.e(tag, getTrace(ste, showMethodName) + msg);
        } else if (level == Level.DEBUG) {
            android.util.Log.d(tag, getTrace(ste, showMethodName) + msg);
        } else if (level == Level.INFO) {
            android.util.Log.i(tag, getTrace(ste, showMethodName) + msg);
        } else if (level == Level.VERBOSE) {
            android.util.Log.v(tag, getTrace(ste, showMethodName) + msg);
        }
    }

    private static String getTrace(StackTraceElement[] ste, boolean includeMethod) {
        if (includeMethod) {
            return "[" + getClassName(ste) + "." + getMethodName(ste) + ":" + getLineNumber(ste) + "] ";
        } else {
            return "[" + getClassName(ste) + ":" + getLineNumber(ste) + "] ";
        }
    }

    private static String getClassName(StackTraceElement[] ste) {
        String[] temp = ste[depth].getClassName().split("\\.");
        return temp[temp.length - 1];
    }

    private static String getMethodName(StackTraceElement[] ste) {
        return ste[depth].getMethodName();
    }

    private static int getLineNumber(StackTraceElement[] ste) {
        return ste[depth].getLineNumber();
    }
}
