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

package com.first3.viz.builders;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.webkit.WebView;

import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentType;
import com.first3.viz.download.Container;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.StringBuffer;
import com.first3.viz.utils.VizUtils;

public class VevoResourceBuilder extends ContainerResourceBuilder {
    String vevoContainerRegex = ".*vevo.com.*watch/";
    Pattern mIdPattern = Pattern.compile(vevoContainerRegex);

    @Override
    public boolean canParse() {
        String sURL = mURL.toExternalForm();

        if (sURL.matches(".*vevo.com.*watch.*") &&
                !sURL.contains("watch/playlist/")) {
            Log.d("can Parse " + sURL);
            return true;
        }
        Log.d("cannot Parse " + sURL);
        return false;
    }

    @Override
    public String getDownloadURL(Container container) {
        StringBuffer sb = StringBuffer.fromString(container.toString());
        String url = sb.stringBetween("/videos\",\"mobile\":\"", "\"},\"");
        String id = getVideoIdFromURL();
        if (id == null) {
            Log.e("vevo failed to parse id from url");
            return null;
        }
        String s[] = id.split("/");
        if (s == null || s.length == 0) {
            return null;
        }
        //Log.d("Vevo: " + id);

        // The title will be overwritten when the browser calls getTitle, but
        // needs to be set or the parent builder will not continue with the
        // download.
        mTitle = "Default Vevo Title";

        String videoId = s[s.length-1];
        //Log.d("getDownloadURL: got vevo id: " + videoId);
        return url.replace("%0", videoId).replace("%1", videoId.toLowerCase());
    }

    @Override
    public String getTitle(WebView wv) {
        String title = wv.getTitle();
        if (title != null) {
            mTitle = VizUtils.normalizeTitle(title);
            setDefaultFilename(mTitle);
        }
        return mTitle;
    }

    private String getVideoIdFromURL() {
        String url = mURL.toString();
        Matcher matcher = mIdPattern.matcher(url);
        while (matcher.find()) {
            int index = matcher.end();
            return url.substring(index);
        }
        return null;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.MP4;
    }

    @Override
    public ContentSource getContentSource() {
        return ContentSource.VEVO;
    }
}
