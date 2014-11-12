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
import com.first3.viz.utils.StringBuffer;

public class ContentTypes {
    private ContentTypes() { }

    private static final Map<String, ContentType> contentTypes =
        new HashMap<String, ContentType>();

    static {
        registerContentTypes();
    }

    public static void registerContentType(ContentType type) {
        contentTypes.put(type.toString(), type);
    }

    public static ContentType fromURL(URL url) {
        String ext;
        StringBuffer sb = StringBuffer.fromString(url.toExternalForm());

        StringBuffer filenameAndQuery = sb.lastAfter("/");
        if (filenameAndQuery == null) {
            return ContentType.NULL;
        }
        StringBuffer filename = filenameAndQuery.before("?");
        if (filename != null) {
            ext = filename.lastStringEndsWith(".");
        } else {
            ext = filenameAndQuery.lastStringEndsWith(".");
        }

        Log.d("matching on extension: " + ext);
        ContentType type = contentTypes.get(ext);
        if (type == null) {
            return ContentType.NULL;
        }
        return type;
    }

    public static Collection<ContentType> getContentTypes() {
        return contentTypes.values();
    }

    private static void registerContentTypes() {
        registerContentType(ContentType.MP4);
        registerContentType(ContentType.MOV);
        registerContentType(ContentType.GP3);
        registerContentType(ContentType.M4V);
        registerContentType(ContentType.MPV);
    }
}
