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

import com.first3.viz.content.ContentSource;

public final class Preferences {
    public static String CURRENT_TAB = "current_tab";
    public static String CURRENT_MODE = "current_mode";
    public static String PLAYING_URI = "playing_uri";
    public static String LASTPAGE_LOADED = "page_loaded";
    public static String AUTO_RESUME = "auto_resume";
    public static String SHARE_VIDEOS = "share_videos";
    public static String DOWNLOAD_DIRECTORY = "download_directory";
    public static String PIN = "pin_code";
    public static String PIN_LOCKED = "pin_locked";
    /**
     * Whether an external video player should be used to play videos selected
     * within Viz.
     */
    public static String USE_EXTERNAL_PLAYER = "use_external_video_player";

    /**
     * Number of files that can be downloaded at the same time before the
     * download requests are queued.
     */
    public static String MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads";
    public static String DOWNLOAD_QUALITY = "download_quality";
    // These much match the strings in constants.xml of the
    // download_quality_values array
    public static String DOWNLOAD_QUALITY_HIGH = "download_quality_high";
    public static String DOWNLOAD_QUALITY_LOW = "download_quality_low";

    public static boolean isHighQualityDownloadDesired() {
        String requestedQuality = VizApp.getPrefs().getString(DOWNLOAD_QUALITY,
                DOWNLOAD_QUALITY_LOW);
        return requestedQuality.equals(DOWNLOAD_QUALITY_HIGH);
    }

    public static String contentSourcePreferenceString(ContentSource source) {
        return source.getSite() + "#added_as_favorite";
    }
}
