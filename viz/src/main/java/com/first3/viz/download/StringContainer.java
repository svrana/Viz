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

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.first3.viz.utils.FetchContainerTask;
import com.first3.viz.utils.IOUtilities;
import com.first3.viz.utils.Log;

public class StringContainer implements Container {
    StringBuilder mBuilder;
    private static Pattern mPattern;

    public StringContainer() { }

    public String toString() {
        return mBuilder.toString();
    }

    // premature optimization
    private static Pattern getEncodingPattern() {
        if (mPattern == null) {
            mPattern = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
        }
        return mPattern;
    }

    @Override
    public boolean downloadURL(FetchContainerTask task, String sURL) {
        boolean isSuccess = true;
        mBuilder = null;
        URL url = url_fromString(sURL);
        if (url == null) {
            Log.w("Could not parse url: " + sURL);
            return false;
        }

        Log.d("downloadURL: " + url.toExternalForm());

        HttpURLConnection con;
        try {
            con = (HttpURLConnection)url.openConnection();
        } catch (IOException e) {
            Log.d("Could not open connection error");
            return false;
        }

        if (con == null) {
            Log.d("Got null connection");
            return false;
        }

        String charset = "ISO-8859-1";
        if (con.getContentType() != null) {
            Pattern p = getEncodingPattern();
            Matcher m = p.matcher(con.getContentType());
            // If Content-Type doesn't match, choose default and hope for the best.
            charset = m.matches() ? m.group(1) : "ISO-8859-1";
        }

        Reader r;
        try {
            r = new InputStreamReader(con.getInputStream(), charset);
        } catch (Exception e) {
            Log.d("Error getting inputstream: " + e);
            con.disconnect();
            return false;
        }
        StringBuilder buf = new StringBuilder(512);
        try {
            while (true) {
                if (task.isCancelled()) {
                    Log.d("fetch container cancelled by user");
                    return false;
                }
                int ch;
                ch = r.read();
                if (ch < 0)
                    break;
                buf.append((char) ch);
            }
        } catch (Exception e) {
            Log.d("Error fetching container");
            isSuccess = false;
            buf = null;
        }
        finally {
            con.disconnect();
            IOUtilities.closeStream(r);
        }
        mBuilder = buf;
        Log.d("Downloaded: " + url.toExternalForm());
        return isSuccess;
    }

    private static URL url_fromString(String sURL) {
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
}
