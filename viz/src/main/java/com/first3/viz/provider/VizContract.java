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

package com.first3.viz.provider;

import java.util.Map;

import android.net.Uri;
import android.provider.BaseColumns;

import com.first3.viz.Config;
import com.first3.viz.utils.Maps;

public class VizContract {

    public interface DirectoryColumns {
        String EXTENSION = "extension";
        String DIRECTORY = "directory";
    }

    public interface FavoritesColumns {
        String RANK = "rank";
        String NAME = "name";
        String URL = "url";
        String FAVICON = "favicon"; //
    }

    public interface ResourcesColumns {
        // added automatically on insert. uri to file mimetype mp4
        String CONTENT = "content";
        String TITLE = "title";
        // TODO: unused-remove this from the Resource table
        String TYPE = "type";
        String URL = "url";
        String CONTAINER_URL = "container";
        // added automatically on insert
        String TIMESTAMP = "timestamp";
        String FILENAME = "filename";   // repetitive
        String FILESIZE = "filesize";
        String DIRECTORY = "directory";
        // thumbnail created on insert. uri to file mimetype png
        String THUMBNAIL = "thumbnail";
        // pointer to its download row
        String DOWNLOAD_ID = "download_id";
        // the last position watched in the video
        String POSITION = "position";
        // the video length; time duration
        String DURATION = "length";
    }

    public interface DownloadsColumns {
        // Columns shared between Resources and Downloads
        String CONTENT = ResourcesColumns.CONTENT;
        String TITLE = ResourcesColumns.TITLE;
        String TYPE = ResourcesColumns.TYPE;
        String URL = ResourcesColumns.URL;
        String CONTAINER_URL = ResourcesColumns.CONTAINER_URL;
        // timestamp added automatically on insert
        String TIMESTAMP = ResourcesColumns.TIMESTAMP;
        String FILENAME = ResourcesColumns.FILENAME;
        // The filesize after the download is complete
        String FILESIZE = ResourcesColumns.FILESIZE;
        String DIRECTORY = ResourcesColumns.DIRECTORY;

        // Downloads specific columns (i.e., not shared with a Resource)
        // current number of ticks in the progress bar (for display)
        String PROGRESS = "progress";
        // maximum ticks of the progressbar
        String MAX_PROGRESS = "progress_max";
        String STATUS = "status"; // read as int and cast to one of Download.Status
        // A date/time file creation String received from the download server.
        // The data is saved to give back to the server when resuming a failed download
        // to verify integrity.
        String URL_LASTMODIFIED = "url_lastmodified";
        String CURRENT_FILESIZE = "current_filesize";
        String PERCENT_COMPLETE = "percent_complete";
    }

    // Based on the build, but usually com.first.viz
    public static final String CONTENT_AUTHORITY = Config.CONTENT_AUTHORITY;

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_RESOURCES = "resources";

    public static final String PATH_DOWNLOADS = "downloads";

    public static final String PATH_VIDEO_RESOURCES = "Videos";

    public static final String PATH_VIDEO_LOCKED = PATH_VIDEO_RESOURCES;

    /**
     * Coded directory name meaning that the provider should store the file
     * in a directory that can be found by the media scanner.
     */
    public static final String PATH_VIDEO_UNLOCKED = "unlocked";

    public static final String PATH_THUMBNAILS = "thumbnails";

    public static final String PATH_FAVORITES = "favorites";

    public static final String PATH_DIRECTORIES = "directories";


    public static class Favorites implements FavoritesColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITES)
                .build();

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.viz.favorite";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.favorite";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = FavoritesColumns.RANK;

        public static Uri buildFavoriteUri(String favoriteId) {
            return CONTENT_URI.buildUpon().appendPath(favoriteId).build();
        }

        public static Uri buildFavoriteUri(int resourceId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(resourceId)).build();
        }

        public static String getFavoriteId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    public static class Resources implements ResourcesColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_RESOURCES)
                .build();

        public static final Uri VIDEO_CONTENT_URI = CONTENT_URI.buildUpon()
                .appendPath(PATH_VIDEO_RESOURCES).build();

        public static final Uri UNLOCKED_VIDEO_CONTENT_URI = CONTENT_URI.buildUpon()
                .appendPath(PATH_VIDEO_UNLOCKED).build();

        public static final Uri THUMBNAIL_URI = CONTENT_URI.buildUpon()
                .appendPath(PATH_THUMBNAILS).build();

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.viz.resource";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.resource";

        /**
         * Default "ORDER BY" clause.
         *
         * Latest should show first
         */
        public static final String DEFAULT_SORT = ResourcesColumns.TIMESTAMP + " DESC";

        /*
        public static Uri buildUnlockedVideoResourceUri(String resourceId) {
            return UNLOCKED_VIDEO_CONTENT_URI.buildUpon().appendPath(resourceId).build();
        }

        public static Uri buildLockedVideoResourceUri(String resourceId) {
            return VIDEO_CONTENT_URI.buildUpon().appendPath(resourceId).build();
        }
        */

        public static Uri buildVideoResourceUri(String resourceId) {
            return VIDEO_CONTENT_URI.buildUpon().appendPath(resourceId).build();
        }

        public static Uri buildThumbnailUri(String filename) {
            return THUMBNAIL_URI.buildUpon().appendPath(filename).build();
        }

        public static Uri buildResourceUri(String resourceId) {
            return CONTENT_URI.buildUpon().appendPath(resourceId).build();
        }

        public static Uri buildResourceUri(int resourceId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(resourceId)).build();
        }

        public static Uri buildContentUri(String directory, String filename) {
            return CONTENT_URI.buildUpon().appendPath(directory).appendPath(filename).build();
        }

        public static String getResourceId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    public static class Downloads implements DownloadsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_DOWNLOADS)
                .build();

        // do not reorder
        public enum Status {
            INPROGRESS(0), COMPLETE(1), FAILED(2), CANCELLED(3), QUEUED(4), PAUSED(5);

            private final Integer value;

            Status(Integer value) {
                this.value = value;
            }

            public Integer valueOf() {
                return value;
            }

            private static final Map<Integer, Status> intToStatusMap = Maps.newHashMap();
            static {
                for (Status status: Status.values()) {
                    intToStatusMap.put(status.value, status);
                }
            }

            public static Status fromInt(int i) {
                return intToStatusMap.get(Integer.valueOf(i));
            }
        }

        /**
         * Number of increments in the progress bar.  Stored in each row so
         * it can be changed between versions.
         */
        public static final int PROGRESS_MAX_NUM = 25;

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.viz.download";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.download";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = DownloadsColumns.TIMESTAMP + " DESC";

        public static Uri buildDownloadUri(String downloadId) {
            return CONTENT_URI.buildUpon().appendPath(downloadId).build();
        }

        public static String getDownloadId(Uri uri) {
            return uri.getLastPathSegment();
        }

        public static Uri buildContentUri(String directory, String filename) {
            return CONTENT_URI.buildUpon().appendPath(directory).appendPath(filename).build();
        }
    }

    private VizContract() {
    }
}
