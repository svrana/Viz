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

import java.util.LinkedList;

import com.first3.viz.Preferences;
import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentType;
import com.first3.viz.download.Container;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.StringBuffer;

public class DailyMotionResourceBuilder extends ContainerResourceBuilder {
    @Override
    public ContentSource getContentSource() {
        return ContentSource.DM;
    }

    /*
     * Given touch.dailymotion.com/video/BLARGH return www.dailymotion.com/embed/video/BLARGH
     */
    @Override
    public String getContainerURL() {
        Log.d(mURL.getFile());
        // return "http://www.dailymotion.com/embed" + mURL.getFile();
        return "http://www.dailymotion.com/embed"
                        + StringBuffer.fromString(mURL.toExternalForm()).stringStartsWith("/video/");
    }

    /*
     * "stream_h264_ld_url":"http:\/\/www.dailymotion.com\/cdn\/H264-320x240\/video\/xoq3mo.mp4?auth=132968749","mode"
     * Title from text like: "title":"Kate Upton Slammed By Victoria's Secret Casting Director","url", "paywall":false
     * Using now the hd if available stream_h264_hd1080_url > stream_h264_hd_url > stream_h264_hq_url >
     * stream_h264_ld_url > stream_h264_ld_url
     */
    @Override
    public String getDownloadURL(Container container) {
        StringBuffer sb = StringBuffer.fromString(container.toString());
        String url = getBestQualityURLAvailable(sb);
        if (url == null) {
            return null;
        }
        mTitle = sb.stringBetween("\"title\":\"", "\",\"url\"");
        if (mTitle != null) {
            mTitle = mTitle.replaceAll("\\\\", "");
        }
        return url.replaceAll("\\\\", "");
    }

    private String getBestQualityURLAvailable(StringBuffer sb) {
        LinkedList<String> qualityLinks = new LinkedList<String>();
        String video;

        video = sb.stringBetween("\"stream_h264_hd1080_url\":\"", "\",\"");
        if (video != null) {
            qualityLinks.add(video);
        }

        video = sb.stringBetween("\"stream_h264_hd1080_url\":\"", "\",\"");
        if (video != null) {
            qualityLinks.add(video);
        }

        video = sb.stringBetween("\"stream_h264_hd_url\":\"", "\",\"");
        if (video != null) {
            qualityLinks.add(video);
        }

        video = sb.stringBetween("\"stream_h264_hq_url\":\"", "\",\"");
        if (video != null) {
            qualityLinks.add(video);
        }

        video = sb.stringBetween("\"stream_h264_ld_url\":\"", "\",\"");
        if (video != null) {
            qualityLinks.add(video);
        }

        video = sb.stringBetween("\"stream_h264_url\":\"", "\",\"");
        if (video != null) {
            qualityLinks.add(video);
        }

        if (qualityLinks.size() == 0) {
            Log.w("Could not find download link");
            return null;
        }

        if (Preferences.isHighQualityDownloadDesired()) {
            return qualityLinks.getFirst();
        } else {
            return qualityLinks.getLast();
        }
    }

    @Override
    public ContentType getContentType() {
        return ContentType.MP4;
    }

    /**
     * URL is something like this: touch.dailymotion.com/video/BLARGH
     */
    @Override
    public boolean canParse() {
        String file = mURL.toExternalForm();
        if (file.matches(".*/video/.*")) {
            Log.d("can parse " + file);
            return true;
        }
        Log.d("cannot parse " + file);
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
