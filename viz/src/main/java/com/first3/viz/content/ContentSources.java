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

package com.first3.viz.content;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.first3.viz.utils.Log;

public class ContentSources {
    private ContentSources() { }

    private static final Map<String, ContentSource> sources =
        new HashMap<String, ContentSource>();
    static {
        registerContentSources();
    }

    public static final String GENERIC_SOURCE = "Generic";

    public static void registerContentSource(ContentSource s) {
        for (String hostname : s.getHostnames()) {
            sources.put(hostname, s);
        }
    }

    public static ContentSource newInstance(URL url) {
        String host = url.getHost().toLowerCase();

        Log.d("matching on: " + host);

        ContentSource s = sources.get(host);
        if (s == null || s.getResourceBuilder() == null) {
            s = ContentSource.GENERIC;
        }
        s.getResourceBuilder().setURL(url);
        return s;
    }

    public static Collection<ContentSource> getContentSources() {
        return sources.values();
    }

    private static void registerContentSources() {
        // Add pre-built parsers to handle videos found by
        // navigating to sites we know about.  ContentSource.GENERIC
        // will be used if the hostname does not match one of the
        // ContentSource.hostname() added here.

        // Any registered content source will be automatically added
        // to the favorites table and shown as a bookmark.
        ContentSources.registerContentSource(ContentSource.VIMEO);
        ContentSources.registerContentSource(ContentSource.DM);
        ContentSources.registerContentSource(ContentSource.BLINKX);
        ContentSources.registerContentSource(ContentSource.FOD);
        ContentSources.registerContentSource(ContentSource.MCAFE);
        ContentSources.registerContentSource(ContentSource.LIVELEAK);
        ContentSources.registerContentSource(ContentSource.VEVO);
        ContentSources.registerContentSource(ContentSource.REDTUBE);
        ContentSources.registerContentSource(ContentSource.PORNHUB);

        // GogoAnime and related sub-sites where videos are hosted
        ContentSources.registerContentSource(ContentSource.GOGOANIME);
        ContentSources.registerContentSource(ContentSource.VIDEO44);
        ContentSources.registerContentSource(ContentSource.VIDEOFUN);
        ContentSources.registerContentSource(ContentSource.VIDZUR);
        ContentSources.registerContentSource(ContentSource.YOURUPLOAD);
        ContentSources.registerContentSource(ContentSource.PLAY44);
    }
}
