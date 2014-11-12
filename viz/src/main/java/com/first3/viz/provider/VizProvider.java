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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.first3.viz.Constants;
import com.first3.viz.provider.VizContract.Resources;
import com.first3.viz.provider.VizContract.Downloads;
import com.first3.viz.provider.VizContract.Favorites;
import com.first3.viz.provider.VizDatabase.Tables;
import com.first3.viz.utils.IOUtilities;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.SelectionBuilder;
import com.first3.viz.utils.Utils;
import com.first3.viz.utils.VizUtils;

public class VizProvider extends ContentProvider {
    private static final String THUMBNAIL_EXT = ".jpg";
    private static final int FAVORITES = 100;
    private static final int FAVORITES_ID = 101;

    private static final int RESOURCES = 200;
    private static final int RESOURCES_ID = 201;
    private static final int RESOURCES_FILENAME = 202;
    private static final int RESOURCES_THUMBNAILS = 203;

    private static final int DOWNLOADS = 300;
    private static final int DOWNLOADS_ID = 301;
    private static final int DOWNLOADS_FILENAME = 302;

    private static UriMatcher sUriMatcher;

    private VizDatabase mOpenHelper;

    private static final Object mutex = new Object();

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = VizContract.CONTENT_AUTHORITY;

        // Favorites
        matcher.addURI(authority, VizContract.PATH_FAVORITES, FAVORITES);
        matcher.addURI(authority, VizContract.PATH_FAVORITES + "/*", FAVORITES_ID);

        // Resources
        matcher.addURI(authority, VizContract.PATH_RESOURCES, RESOURCES);
        matcher.addURI(authority, VizContract.PATH_RESOURCES + "/#", RESOURCES_ID);
        // should be able to distinguish between the two using mimetypes
        matcher.addURI(authority, VizContract.PATH_RESOURCES + "/thumbnails/*", RESOURCES_THUMBNAILS);
        matcher.addURI(authority, VizContract.PATH_RESOURCES + "/*/*", RESOURCES_FILENAME);

        // Downloads
        matcher.addURI(authority, VizContract.PATH_DOWNLOADS, DOWNLOADS);
        matcher.addURI(authority, VizContract.PATH_DOWNLOADS + "/#", DOWNLOADS_ID);
        matcher.addURI(authority, VizContract.PATH_DOWNLOADS + "/*/*", DOWNLOADS_FILENAME);

        return matcher;
    }

    private synchronized UriMatcher getUriMatcher() {
        if (sUriMatcher == null) {
            sUriMatcher = buildUriMatcher();
        }
        return sUriMatcher;
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mOpenHelper = new VizDatabase(context);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Log.v("query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");

        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        final int match = getUriMatcher().match(uri);

        final SelectionBuilder builder = buildExpandedSelection(uri, match);
        Cursor query;
        synchronized (mutex) {
            query = builder.where(selection, selectionArgs).query(db, projection, sortOrder);
        }
        query.setNotificationUri(getContext().getContentResolver(), uri);
        return query;
    }

    @Override
    public String getType(Uri uri) {
        final int match = getUriMatcher().match(uri);
        switch (match) {
            case FAVORITES:
                return Favorites.CONTENT_TYPE;
            case FAVORITES_ID:
                return Favorites.CONTENT_ITEM_TYPE;
            case RESOURCES:
                return Resources.CONTENT_TYPE;
            case RESOURCES_ID:
                return Resources.CONTENT_ITEM_TYPE;
            case DOWNLOADS:
                return Resources.CONTENT_TYPE;
            case DOWNLOADS_ID:
                return Resources.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        switch (getUriMatcher().match(uri)) {
            case FAVORITES:
            case RESOURCES:
            case DOWNLOADS:
                return null;
            case RESOURCES_ID:
                return new String[] { "video/mp4" };
                // If the URI pattern doesn't match any permitted patterns, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    private File fileFromResourceMap(ContentValues map) throws IOException {
        String videoDir = (String) map.get(Resources.DIRECTORY);
        if (TextUtils.isEmpty(videoDir)) {
            throw new IOException("Must specify DIRECTORY when inserting Resource");
        }
        String filename = (String) map.get(Resources.FILENAME);
        if (TextUtils.isEmpty(filename)) {
            throw new IOException("Must specify FILENAME when inserting Resource");
        }

        // Deprecated
        if (VizContract.PATH_VIDEO_UNLOCKED.equals(videoDir)) {
            return VizUtils.getPublicVideoFile(filename);
        } else if (VizContract.PATH_VIDEO_LOCKED.equals(videoDir)){
            return VizUtils.getPrivateVideoFile(filename);
        } else {
            // All new files will have a literal path
            return new File(videoDir, filename);
        }
    }

    //@TargetApi(11)
    private void calculateDuration(ContentValues map) throws IOException {
        if (Utils.isGingerBreadMROrLater()) {
            File videoFile = fileFromResourceMap(map);
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(videoFile.toString());
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (!TextUtils.isEmpty(duration)) {
                map.put(Resources.DURATION, Integer.valueOf(duration));
            }
            mmr.release();
        }
    }

    private void informMediaScanner(ContentValues map) throws IOException {
        final File videoFile = fileFromResourceMap(map);

        String dir = (String) map.get(Resources.DIRECTORY);
        if (VizUtils.isPublicDir(dir)) {
            Thread t = new Thread("AddVideoToGallery") {
                @Override
                public void run() {
                    VizUtils.informMediaScanner(videoFile.toString());
                }
            };
            Utils.threadStart(t, "Failed to add video to gallery");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.v("insert(uri=" + uri + ", values=[" + values.toString() + "])");

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = getUriMatcher().match(uri);
        long id = 0;

        switch (match) {
            case FAVORITES: {
                synchronized (mutex) {
                    id = db.insertOrThrow(Tables.FAVORITES, null, values);
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return Favorites.buildFavoriteUri(String.valueOf(id));
            }
            case RESOURCES:
            case RESOURCES_ID: {
                try {
                    addDefaultThumbnail(values);
                    calculateDuration(values);
                } catch (Exception e) {
                    Log.w("Exception thrown creating thumbnail");
                    e.printStackTrace();
                }
                try { informMediaScanner(values); } catch(IOException e) {}
                values.put(Resources.TIMESTAMP, String.valueOf(System.currentTimeMillis()));
                synchronized (mutex) {
                    id = db.insertOrThrow(Tables.RESOURCES, null, values);
                }
                Uri resourceUri = Resources.buildResourceUri(String.valueOf(id));

                try {
                    createThumbnail(values);
                    getContext().getContentResolver().update(resourceUri, values, null, null);
                } catch (IOException e) {
                    Log.w("Error creating thumbnail");
                }

                getContext().getContentResolver().notifyChange(uri, null);
                return resourceUri;
            }
            case DOWNLOADS: {
                values.put(Downloads.STATUS, Downloads.Status.INPROGRESS.valueOf());
                values.put(Downloads.TIMESTAMP, String.valueOf(System.currentTimeMillis()));
                values.put(Downloads.MAX_PROGRESS, Downloads.PROGRESS_MAX_NUM);
                synchronized (mutex) {
                    id = db.insertOrThrow(Tables.DOWNLOADS, null, values);
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return Downloads.buildDownloadUri(String.valueOf(id));
            }

            default: {
                throw new UnsupportedOperationException("Unknown uri(" + match + "): " + uri);
            }
        }
    }

    private void deleteVideo(File dir, String filename) {
        Log.v("deleteThumbnail(dir=" + dir.toString() + ", filename=" + filename + ")");
        File file = new File(dir, filename);
        file.delete();
    }

    private void deleteThumbnail(String dir, String filename) {
        Log.v("deleteThumbnail(dir=" + dir + ", filename=" + filename + ")");
        File file = new File(getContext().getExternalFilesDir(dir), filename);
        file.delete();
    }

    /**
     * If the download failed to complete then delete the content associated
     * with it, as it cannot otherwise be deleted (there's no resource record
     * for it).
     */
    private void deleteDownloadSideEffect(Uri downloadUri) {
        Log.v("deleteDownloadSideEffect(uri=" + downloadUri + ")");
        Cursor cursor = query(downloadUri, new String[] { Downloads._ID,
            Downloads.DIRECTORY, Downloads.FILENAME, Downloads.STATUS }, null, null, null);
        if (!cursor.moveToFirst()) {
            // This is also called when deleting a Resource and there the
            // download could have been manually removed by the user, so this is
            // to be expected common.
            cursor.close();
            return;
        }
        int statusInt = cursor.getInt(cursor.getColumnIndex(Downloads.STATUS));
        Downloads.Status status = Downloads.Status.fromInt(statusInt);
        if (status == Downloads.Status.FAILED || status == Downloads.Status.CANCELLED
                || status == Downloads.Status.PAUSED) {
            String filename = cursor.getString(cursor.getColumnIndex(Downloads.FILENAME));
            String dir = cursor.getString(cursor.getColumnIndex(Downloads.DIRECTORY));
            deleteVideo(directorySwitch(dir), filename);
        }
        cursor.close();
    }

    private void removeFromMediaStore(String filename) {
        Log.d("removeFromMediaStore(filename=" + filename + ")");

        final String[] fields = { MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.TITLE };
        String select = MediaStore.MediaColumns.DATA + "=?";
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContext().getContentResolver().query(uri, fields, select, new String[] { filename }, null);
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri mediaUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
            getContext().getContentResolver().delete(mediaUri, null, null);
            getContext().getContentResolver().notifyChange(mediaUri, null);
            Log.d("Removing media uri: " + mediaUri);
        } else {
            Log.w("Could not find media uri");
        }
        cursor.close();
    }

    private void deleteResourceSideEffect(Uri resourceUri) {
        Log.v("deleteResourceSideEffect(uri=" + resourceUri + ")");

        // delete download row correspondong to this content
        Cursor cursor = query(resourceUri, new String[] { Resources._ID, Resources.DOWNLOAD_ID,
            Resources.DIRECTORY, Resources.FILENAME, Resources.CONTENT }, null, null, null);
        if (!cursor.moveToFirst()) {
            Log.e("deleteResourceSideEffect: cursor empty error");
            return;
        }
        String downloadId = cursor.getString(cursor.getColumnIndex(Resources.DOWNLOAD_ID));
        Uri downloadUri = Downloads.buildDownloadUri(downloadId);
        delete(downloadUri, null, null);
        getContext().getContentResolver().notifyChange(downloadUri, null);

        // delete video file associated with resource
        String filename = cursor.getString(cursor.getColumnIndex(Resources.FILENAME));
        String dir = cursor.getString(cursor.getColumnIndex(Resources.DIRECTORY));
        deleteVideo(directorySwitch(dir), filename);

        if (VizUtils.isPublicDir(dir)) {
            removeFromMediaStore(VizUtils.getPublicVideoFilename(filename));
        }

        deleteThumbnail(VizContract.PATH_THUMBNAILS, filename + THUMBNAIL_EXT);

        cursor.close();
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.v("delete(uri=" + uri + ")");

        final int match = getUriMatcher().match(uri);
        switch (match) {
            case RESOURCES_ID:
                deleteResourceSideEffect(uri);
                break;
            case DOWNLOADS_ID:
                deleteDownloadSideEffect(uri);
                break;
            default:
                break;
        }

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal;
        synchronized (mutex) {
            retVal = builder.where(selection, selectionArgs).delete(db);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        Log.v("update(uri=" + uri + ")");

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int rowsChanged;
        synchronized (mutex) {
            rowsChanged = builder.where(selection, selectionArgs).update(db, values);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsChanged;
    }

    private File directorySwitch(String dir) {
        return new File(dir);
    }

    private File getFileFromUri(Uri uri) throws FileNotFoundException {
        Cursor cursor = null;
        String filename = null;
        File videoDir = null;

        int match = getUriMatcher().match(uri);
        Log.d("getFileFromUri(uri=" + uri + ", match=" + match + ")");
        switch(match) {
            case RESOURCES_ID:
                cursor = query(uri, new String[] { Resources._ID, Resources.DIRECTORY,
                    Resources.FILENAME }, null, null, null);
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    throw new FileNotFoundException("Could not find Resource for uri");
                }
                filename = cursor.getString(cursor.getColumnIndex(Resources.FILENAME));
                videoDir = directorySwitch(cursor.getString(cursor.getColumnIndex(Resources.DIRECTORY)));
                Log.d("Got filename: " + filename + " directory: " + videoDir);
                break;
            case DOWNLOADS_ID:
                cursor = query(uri, new String[] { Downloads._ID, Downloads.DIRECTORY,
                    Downloads.FILENAME }, null, null, null);
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    throw new FileNotFoundException("Could not find Download for uri");
                }
                filename = cursor.getString(cursor.getColumnIndex(Downloads.FILENAME));
                videoDir = directorySwitch(cursor.getString(cursor.getColumnIndex(Downloads.DIRECTORY)));
                Log.d("Got filename: " + filename + " directory: " + videoDir);
                break;
            case RESOURCES_THUMBNAILS:
                Log.d("Got thumbnail: " + uri);
                filename = uri.getLastPathSegment();
                File thumbnailDirectory = VizUtils.getVideosThumbnailDir();
                Log.d("Full thumbnail directory path: " + VizUtils.getVideosThumbnailPath());

                if (thumbnailDirectory == null || !thumbnailDirectory.exists()) {
                    Log.e("Could not create directory error: " + VizUtils.getVideosThumbnailPath());
                    throw new FileNotFoundException("Media not mounted error: thumbnail could not be found");
                }

                Log.d("Got thumbnail filename: " + filename);

                return new File(thumbnailDirectory, filename);
            default:
                throw new FileNotFoundException("No case for " + match);
        }

        cursor.close();
        if (videoDir == null) {
            throw new FileNotFoundException("no video directory specified for " + match);
        }
        if (filename == null) {
            throw new FileNotFoundException("no filename specified for " + match);
        }
        return new File(videoDir, filename);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        List<String> pseg = uri.getPathSegments();
        if (pseg.size() < 2) {
            throw new FileNotFoundException("invalid uri error " + uri);
        }

        File path = getFileFromUri(uri);

        Log.v("openFile(uri=" + uri + ", file=" + path + ")");
        int imode = 0;
        if (mode.contains("w")) {
            imode |= ParcelFileDescriptor.MODE_WRITE_ONLY;
            if (!path.exists()) {
                try {
                    path.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new FileNotFoundException("Error creating " + uri);
                }
            } else {
                throw new FileNotFoundException("File with name " + path + " already exists");
            }
        } else if (mode.contains("r")) {
            if (!path.exists()) {
                throw new FileNotFoundException("File not found " + uri);
            }
        }

        if (mode.contains("r")) imode |= ParcelFileDescriptor.MODE_READ_ONLY;
        if (mode.contains("+")) imode |= ParcelFileDescriptor.MODE_APPEND;

        return ParcelFileDescriptor.open(path, imode);
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = getUriMatcher().match(uri);
        switch (match) {
            case FAVORITES_ID: {
                final String favoriteId = Favorites.getFavoriteId(uri);
                return builder.table(Tables.FAVORITES).where(Favorites._ID + "=?", favoriteId);
            }
            case RESOURCES_ID: {
                final String resourceId = Resources.getResourceId(uri);
                return builder.table(Tables.RESOURCES).where(Resources._ID + "=?", resourceId);
            }
            case DOWNLOADS_ID: {
                final String downloadId = Downloads.getDownloadId(uri);
                return builder.table(Tables.DOWNLOADS).where(Downloads._ID + "=?", downloadId);
            }
            case DOWNLOADS: {
                return builder.table(Tables.DOWNLOADS);
            }
            case RESOURCES: {
                return builder.table(Tables.RESOURCES);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri( " + match + "): " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case FAVORITES: {
                return builder.table(Tables.FAVORITES);
            }
            case FAVORITES_ID: {
                final String favoritesId = Favorites.getFavoriteId(uri);
                return builder.table(Tables.FAVORITES).where(Favorites._ID + "=?", favoritesId);
            }
            case RESOURCES: {
                return builder.table(Tables.RESOURCES);
            }
            case RESOURCES_ID: {
                final String resourcesId = Resources.getResourceId(uri);
                return builder.table(Tables.RESOURCES).where(Resources._ID + "=?", resourcesId);
            }
            case DOWNLOADS: {
                return builder.table(Tables.DOWNLOADS);
            }
            case DOWNLOADS_ID: {
                final String downloadsId = Downloads.getDownloadId(uri);
                return builder.table(Tables.DOWNLOADS).where(Downloads._ID + "=?", downloadsId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

     /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    private void addDefaultThumbnail(ContentValues map) {
        File defaultThumbnail = new File(Constants.DEFAULT_THUMBNAIL_FILENAME);
        Uri defaultThumbnailUri = Uri.fromFile(defaultThumbnail);
        map.put(Resources.THUMBNAIL, defaultThumbnailUri.toString());
    }

    private void createThumbnail(ContentValues map) throws IOException {
        File videoFile = fileFromResourceMap(map);

        Log.d("Creating thumbnail from video file " + videoFile.toString());

        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoFile.toString(),
                MediaStore.Video.Thumbnails.MINI_KIND);
        if (bitmap == null) {
            Log.w("Error creating thumbnail");
            return;
        }

        String filename = (String) map.get(Resources.FILENAME);
        if (TextUtils.isEmpty(filename)) {
            throw new IOException("Must specify FILENAME when inserting Resource");
        }
        Uri thumbnailUri = Resources.buildThumbnailUri(filename + THUMBNAIL_EXT);
        OutputStream ostream;
        try {
            ostream = getContext().getContentResolver().openOutputStream(thumbnailUri);
        } catch (FileNotFoundException e) {
            Log.d("Could not open output stream for thumbnail storage: " + e.getLocalizedMessage());
            return;
        }

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
        ostream.flush();
        IOUtilities.closeStream(ostream);

        map.put(Resources.THUMBNAIL, thumbnailUri.toString());
    }
}
