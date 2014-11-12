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

import java.io.File;

import com.first3.viz.provider.VizDatabase;

import android.content.Context;

public class ContentType {
    private final String type;
    private final DownloadLocationMediator dlm;

    private ContentType(String type) {
        this.type = type;
        this.dlm = new DownloadLocationMediator();
    }

    // static final class cannot access preferences itself, so delegating
    // to another class that can.
    public File getDefaultDownloadDirectory(Context context) {
        return dlm.getDefaultDownloadDirectory(context);
    }

    public String toSQLString() { return type.substring(1, type.length()); }

    public String toString() { return type; }

    /*
     * Do not forget to register your new ContentType.  See ContentTypes.
     */

    public static final ContentType MP4 = new ContentType(".mp4");
    public static final ContentType MOV = new ContentType(".mov");
    public static final ContentType GP3 = new ContentType(".3gp");
    public static final ContentType M4V = new ContentType(".m4v");
    public static final ContentType MPV = new ContentType(".mpv");

    /* A ContentType for content that cannot be handled */
    public static final ContentType NULL = new ContentType("<null>");


    private class DownloadLocationMediator {
        private DownloadLocationMediator() {}

        private File getDefaultDownloadDirectory(Context context) {
            VizDatabase db = new VizDatabase(context);
            String dir = db.getDirectory(toSQLString());
            db.close();
            if (dir == null) {
                return context.getExternalFilesDir(null);
            }
            return new File(dir);
        }
    }
};
