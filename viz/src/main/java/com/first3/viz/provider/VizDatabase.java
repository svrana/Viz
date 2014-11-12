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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.BaseColumns;

import com.first3.viz.models.Favorite;
import com.first3.viz.provider.VizContract.DirectoryColumns;
import com.first3.viz.provider.VizContract.DownloadsColumns;
import com.first3.viz.provider.VizContract.FavoritesColumns;
import com.first3.viz.provider.VizContract.ResourcesColumns;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Utils;
import com.first3.viz.utils.VizUtils;

public class VizDatabase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "viz.db";
    public static final int DATABASE_VERSION = 6;
    public final Context mContext;

    public interface Tables {
        // All downloaded content
        String RESOURCES = "resources";

        // (Current) Downloads
        String DOWNLOADS = "downloads";

        // bookmarks in the browser
        String FAVORITES  = "favorites";

        // learned download locations for all media types
        String DIRECTORIES  = "directories";
    }

    private static final String CREATE_RESOURCES_TABLE = "CREATE TABLE " +
        Tables.RESOURCES + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        ResourcesColumns.TITLE + " TEXT, " +
        ResourcesColumns.TYPE + " TEXT, " +
        ResourcesColumns.URL + " TEXT, " +
        ResourcesColumns.CONTAINER_URL + " TEXT, " +
        ResourcesColumns.TIMESTAMP + " TEXT, " +
        ResourcesColumns.DIRECTORY + " TEXT, " +
        ResourcesColumns.CONTENT + " TEXT, " +      // uri to file
        ResourcesColumns.THUMBNAIL + " TEXT, " +    // uri to file
        ResourcesColumns.FILENAME + " TEXT, " +
        ResourcesColumns.POSITION + " INTEGER, " +
        ResourcesColumns.DURATION + " INTEGER, " +
        ResourcesColumns.DOWNLOAD_ID + " TEXT, " +
        ResourcesColumns.FILESIZE + " TEXT)";

    private static final String CREATE_DOWNLOADS_TABLE = "CREATE TABLE " +
        Tables.DOWNLOADS + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        DownloadsColumns.TITLE + " TEXT, " +
        DownloadsColumns.TYPE + " TEXT, " +
        DownloadsColumns.URL + " TEXT, " +
        DownloadsColumns.CONTAINER_URL + " TEXT, " +
        DownloadsColumns.TIMESTAMP + " TEXT, " +
        DownloadsColumns.DIRECTORY + " TEXT, " +
        DownloadsColumns.CONTENT + " TEXT, " +      // uri to file
        DownloadsColumns.FILENAME + " TEXT, " +
        DownloadsColumns.PROGRESS + " INTEGER, " +
        DownloadsColumns.MAX_PROGRESS + " INTEGER, " +
        DownloadsColumns.STATUS + " INTEGER, " +
        DownloadsColumns.FILESIZE + " TEXT," +
        DownloadsColumns.URL_LASTMODIFIED + " TEXT, " +
        DownloadsColumns.CURRENT_FILESIZE + " TEXT, " +
        DownloadsColumns.PERCENT_COMPLETE + " INTEGER)";

    private static final String CREATE_DIRECTORIES_TABLE = "CREATE TABLE " +
        Tables.DIRECTORIES + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        DirectoryColumns.EXTENSION + " TEXT, " +
        DirectoryColumns.DIRECTORY + " TEXT)";

    private static final String CREATE_FAVORITES_TABLE = "CREATE TABLE " +
        Tables.FAVORITES + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        FavoritesColumns.RANK + " INTEGER, " +
        FavoritesColumns.NAME + " TEXT, " +
        FavoritesColumns.URL + " TEXT, " +
        FavoritesColumns.FAVICON + " BLOB)";


    public VizDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    public static class DownloadsCursor extends SQLiteCursor {
        public static final String QUERY =
            "SELECT " + Tables.DOWNLOADS + "." + BaseColumns._ID + ", " +
            DownloadsColumns.TITLE + ", " + DownloadsColumns.TYPE + ", " +
            DownloadsColumns.URL + ", " + DownloadsColumns.CONTAINER_URL + ", " +
            DownloadsColumns.TIMESTAMP + ", " + DownloadsColumns.DIRECTORY + ", " +
            DownloadsColumns.CONTENT + ", " + DownloadsColumns.FILENAME + ", " +
            DownloadsColumns.PROGRESS + ", " + DownloadsColumns.MAX_PROGRESS + ", " +
            DownloadsColumns.STATUS + ", " + DownloadsColumns.FILESIZE + ", " +
            DownloadsColumns.URL_LASTMODIFIED + ", " +
            DownloadsColumns.CURRENT_FILESIZE + ", " +
            DownloadsColumns.PERCENT_COMPLETE +
            " FROM " + Tables.DOWNLOADS + " ORDER BY " + BaseColumns._ID;

        private DownloadsCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
                String editTable, SQLiteQuery query) {
            super(db, driver, editTable, query);
        }

        private static class Factory implements SQLiteDatabase.CursorFactory {
            @Override
            public Cursor newCursor(SQLiteDatabase db,
                    SQLiteCursorDriver driver, String editTable,
                    SQLiteQuery query) {
                return new DownloadsCursor(db, driver, editTable, query);
            }
        }

        public int getColId() {
            return getInt(getColumnIndexOrThrow(BaseColumns._ID));
        }

        public String getColTitle() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.TITLE));
        }

        public String getColType() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.TYPE));
        }

        public String getColUrl() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.URL));
        }

        public String getColContainerUrl() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.CONTAINER_URL));
        }

        public String getColTimestamp() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.TIMESTAMP));
        }

        public String getDirectory() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.DIRECTORY));
        }

        public String getColContent() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.CONTENT));
        }

        public String getColFilename() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.FILENAME));
        }

        public int getColProgress() {
            return getInt(getColumnIndexOrThrow(DownloadsColumns.PROGRESS));
        }

        public int getColMaxProgress() {
            return getInt(getColumnIndexOrThrow(DownloadsColumns.MAX_PROGRESS));
        }

        public String getColStatus() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.STATUS));
        }

        public String getColFilesize() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.FILESIZE));
        }

        public String getColURL() {
            return getString(getColumnIndexOrThrow(DownloadsColumns.URL));
        }
    }

    public DownloadsCursor getDownloadsCursor() {
        SQLiteDatabase d = getReadableDatabase();
        DownloadsCursor c = (DownloadsCursor) d.rawQueryWithFactory(
            new DownloadsCursor.Factory(), DownloadsCursor.QUERY, null, null);
        c.moveToFirst();
        return c;
    }

    public static class FavoritesCursor extends SQLiteCursor {
        public static final String QUERY =
            "SELECT " + Tables.FAVORITES + "." + BaseColumns._ID + ", " +
            FavoritesColumns.NAME + ", " + FavoritesColumns.URL + ", " +
            FavoritesColumns.FAVICON + " FROM " + Tables.FAVORITES +
            " ORDER BY " + FavoritesColumns.RANK;

        private FavoritesCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
                String editTable, SQLiteQuery query) {
            super(db, driver, editTable, query);
        }

        /*
        private FavoritesCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
            super(driver, editTable, query);
        }
        */

        private static class Factory implements SQLiteDatabase.CursorFactory {
            @Override
            public Cursor newCursor(SQLiteDatabase db,
                    SQLiteCursorDriver driver, String editTable,
                    SQLiteQuery query) {
                return new FavoritesCursor(db, driver, editTable, query);
            }
        }

        public int getColId() {
            return getInt(getColumnIndexOrThrow(BaseColumns._ID));
        }

        public String getColName() {
            return getString(getColumnIndexOrThrow(FavoritesColumns.NAME));
        }

        public String getColURL() {
            return getString(getColumnIndexOrThrow(FavoritesColumns.URL));
        }

        public Bitmap getColFavicon() {
            byte[] data = getBlob(getColumnIndexOrThrow(FavoritesColumns.FAVICON));
            if (data != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                return bitmap;
            }
            return null;
        }
    }

    public FavoritesCursor getFavorites() {
        SQLiteDatabase d = getReadableDatabase();
        FavoritesCursor c = (FavoritesCursor) d.rawQueryWithFactory(
            new FavoritesCursor.Factory(), FavoritesCursor.QUERY,
            null, null);
        c.moveToFirst();
        return c;
    }

    public static class DirectoriesCursor extends SQLiteCursor {
        public static final String QUERY =
            "SELECT " + Tables.DIRECTORIES + "." + BaseColumns._ID + ", " +
            DirectoryColumns.DIRECTORY + ", " + DirectoryColumns.EXTENSION +
            " FROM " + Tables.DIRECTORIES;


        private DirectoriesCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
                String editTable, SQLiteQuery query) {
            super(db, driver, editTable, query);
        }

        private DirectoriesCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
            super(driver, editTable, query);
        }

        private static class Factory implements SQLiteDatabase.CursorFactory {
            @Override
            public Cursor newCursor(SQLiteDatabase db,
                    SQLiteCursorDriver driver, String editTable,
                    SQLiteQuery query) {
                return new DirectoriesCursor(db, driver, editTable, query);
            }
        }

        public String getColDirectory() {
            return getString(getColumnIndexOrThrow(DirectoryColumns.DIRECTORY));
        }

        public String getColExtension() {
            return getString(getColumnIndexOrThrow(DirectoryColumns.EXTENSION));
        }
    }

    public String getDirectory(String extension) {
        String sql = DirectoriesCursor.QUERY + " WHERE " + DirectoryColumns.EXTENSION +
            "=" + DatabaseUtils.sqlEscapeString(extension);

        SQLiteDatabase d = getReadableDatabase();
        DirectoriesCursor c = (DirectoriesCursor) d.rawQueryWithFactory(
            new DirectoriesCursor.Factory(), sql, null, null);
        if (c.getCount() == 0) {
            d.close();
            c.close();
            return null;
        }
        c.moveToFirst();
        String directory = c.getColDirectory();
        c.close();
        d.close();
        return directory;
    }

    public void addFavorite(Favorite favorite) {
        Log.d("addFavorites(): " + favorite);
        ContentValues map = new ContentValues();
        map.put(FavoritesColumns.RANK, favorite.getRank());
        map.put(FavoritesColumns.NAME, favorite.getTitle());
        map.put(FavoritesColumns.URL, favorite.getUrl());
        if (favorite.getFavicon() != null) {
            map.put(FavoritesColumns.FAVICON, favorite.getFaviconByteArray());
        }
        try {
            getWritableDatabase().insert(Tables.FAVORITES, null, map);
        } catch (SQLException e) {
            Log.e("Error adding favorite: ", favorite.toString());
        }
    }

    /**
     * Must have a valid id set.
     */
    public void removeFavorite(Favorite favorite) {
        Log.d("removeFavorite(): " + favorite);
        try {
            getWritableDatabase().delete(Tables.FAVORITES,
                    BaseColumns._ID + "=" + favorite.getId(), null);
        } catch (SQLException e) {
            Log.e("Error removing favorite: ", favorite.toString());
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d();
        db.execSQL(CREATE_RESOURCES_TABLE);
        db.execSQL(CREATE_DOWNLOADS_TABLE);
        db.execSQL(CREATE_DIRECTORIES_TABLE); // just use sharedpreferences?
        db.execSQL(CREATE_FAVORITES_TABLE);

        // called here so this only happens once per app installation.  i.e.,
        // let users delete/rename the default directories
        createDefaultDirectories(mContext);

        Thread t = new Thread() {
            public void run() {
                Favorite.addSupportedSites();
            }
        };
        Utils.threadStart(t, "Unable to add default favorites");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d();
        // run necessary upgrades
        int version = oldVersion;
        switch (version) {
            case 1:
                upgradeToTwo(db);
                version = 2;
            case 2:
                upgradeToThree(db);
                version = 3;
            case 3:
                upgradeToFour(db);
                version = 4;
            case 4:
                upgradeToFive(db);
                version = 5;
            case 5:
                upgradeToSix(db);
                version = 6;
        }

        // drop all tables if version is not right
        Log.d("after upgrade logic, at version " + version);
        if (version != DATABASE_VERSION) {
            Log.w("Database has incompatible version, starting from scratch");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.RESOURCES);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.DOWNLOADS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.DIRECTORIES);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.FAVORITES);

            onCreate(db);
        }
    }

    private void upgradeToTwo(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.DOWNLOADS + " ADD COLUMN " + DownloadsColumns.CONTAINER_URL +
                 " TEXT DEFAULT '';");
        db.execSQL("ALTER TABLE " + Tables.RESOURCES + " ADD COLUMN " + ResourcesColumns.CONTAINER_URL +
                 " TEXT DEFAULT '';");
    }

    private void upgradeToThree(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.RESOURCES + " ADD COLUMN " + ResourcesColumns.DURATION +
                 " INTEGER DEFAULT 0;");
        db.execSQL("ALTER TABLE " + Tables.RESOURCES + " ADD COLUMN " + ResourcesColumns.POSITION +
                 " INTEGER DEFAULT 0;");
    }

    private void upgradeToFour(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.DOWNLOADS + " ADD COLUMN " + DownloadsColumns.URL_LASTMODIFIED +
                 " TEXT DEFAULT '';");
    }

    // Replace directory column that used to contain a string that acted as a
    // pointer to the directory, with the full path of the directory where the
    // download is stored.
    private void upgradeToFive(SQLiteDatabase db) {
        // Create content values that contains the name of the column you want to update and
        // the value you want to assign to it
        ContentValues cv = new ContentValues();
        cv.put(ResourcesColumns.DIRECTORY, VizUtils.getVideosPublicPath());

        // The where clause to identify which columns to update.
        String where = ResourcesColumns.DIRECTORY + "=?";
        String[] value = { VizContract.PATH_VIDEO_UNLOCKED }; // The value for the where clause.

        // Update the database (all columns in TABLE_NAME where my_column has
        // a value of unlocked will the value in contentvalues
        db.update(Tables.RESOURCES, cv, where, value);
        db.update(Tables.DOWNLOADS, cv, where, value);

        cv = new ContentValues();
        cv.put(ResourcesColumns.DIRECTORY, VizUtils.getVideosPrivatePath());
        value[0] = VizContract.PATH_VIDEO_LOCKED;

        db.update(Tables.RESOURCES, cv, where, value);
        db.update(Tables.DOWNLOADS, cv, where, value);
    }

    private void upgradeToSix(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.DOWNLOADS + " ADD COLUMN " +
                DownloadsColumns.CURRENT_FILESIZE + " TEXT DEFAULT '';");
        db.execSQL("ALTER TABLE " + Tables.DOWNLOADS + " ADD COLUMN " +
                DownloadsColumns.PERCENT_COMPLETE + " INTEGER DEFAULT 0;");
    }

    private void createDefaultDirectories(Context context) {
        VizUtils.getVideosPrivateDir();
        VizUtils.getVideosThumbnailDir();
    }
}
