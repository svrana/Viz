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

package com.first3.viz.models;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.first3.viz.provider.VizContract.DownloadsColumns;
import com.first3.viz.provider.VizContract.ResourcesColumns;
import com.first3.viz.utils.VizUtils;
import com.first3.viz.utils.Utils;
import com.first3.viz.provider.VizContract;
import com.first3.viz.utils.Log;

public class Resource implements Parcelable {
    private static final String URL_KEY = "url_key";
    private static final String CONTAINER_URL_KEY = "container_url_key";
    private static final String TITLE_KEY = "title_key";
    private static final String FILENAME_KEY = "filename_key";
    private static final String DIRECTORY_KEY = "directory_key";
    private static final String FILESIZE_KEY = "filesize_key";
    private static final String CURRENT_FILESIZE_KEY = "current_filesize_key";
    private static final String DOWNLOAD_URI_KEY = "download_uri_key";
    private static final String IS_LOCKED_KEY = "is_locked_key";
    private String mFilename = "filename";
    private String mTitle = "Untitled";
    private File mDownloadDirectory = VizUtils.getVideosPrivateDir();
    private String mURL, mLastMod;
    private String mContainerURL;
    private Long mFilesize = Long.valueOf(0);
    private Long mCurrentFilesize = Long.valueOf(0);
    /** This is the URI referencing the Download in the Downloads table */
    private Uri mDownloadUri;
    private boolean mIsLocked = true;
    private static final Resource sNullResource = new Resource(); // stupid

    private Resource() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Resource)) {
            return false;
        }

        Resource lhs = (Resource) o;

        String thisPath = Utils.getCanonicalPath(mDownloadDirectory);
        String lhsPath = Utils.getCanonicalPath(lhs.mDownloadDirectory);

        if (thisPath.equals(lhsPath) && mFilename.equals(lhs.mFilename)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 15; // a non-zero constant recommended
        result = 31 * result + mFilename.hashCode();
        result = 31 * result + mDownloadDirectory.hashCode();
        return result;
    }

    public static Resource create() {
        return new Resource();
    }

    /**
     * This does not create a complete Resource but rather just enough to
     * fullfill the needs of equals and hashCode.  Note that this works for
     * both a Download and a Resource; however, the URLLastModified data is
     * only applicable to a Download.
     */
    public static Resource fromCursor(Cursor cursor) {
        if (cursor.getCount() == 0) {
            return sNullResource;
        }

        // These columns apply to both Downloads and Resources
        String title = cursor.getString(cursor.getColumnIndex(ResourcesColumns.TITLE));
        String directory = cursor.getString(cursor.getColumnIndex(ResourcesColumns.DIRECTORY));
        String filename = cursor.getString(cursor.getColumnIndex(ResourcesColumns.FILENAME));
        String url = cursor.getString(cursor.getColumnIndex(ResourcesColumns.URL));

        // These columns are only applicable for a Download cursor not Resource
        String urlLastMod = "";
        String currentFilesize = "";
        String filesize = "";
        boolean isDownloadCursor = (cursor.getColumnIndex(DownloadsColumns.URL_LASTMODIFIED) != -1);
        if (isDownloadCursor) {
            urlLastMod = cursor.getString(cursor.getColumnIndex(DownloadsColumns.URL_LASTMODIFIED));
            currentFilesize = cursor.getString(cursor.getColumnIndex(DownloadsColumns.CURRENT_FILESIZE));
            filesize = cursor.getString(cursor.getColumnIndex(DownloadsColumns.FILESIZE));
        }

        Resource r = new Resource();
        r.setTitle(title)
            .setDownloadDirectory(new File(directory))
            .setFilename(filename)
            .setURL(url)
            .setURLLastModified(urlLastMod)
            .setFilesize(filesize)
            .setCurrentFilesize(currentFilesize);
        return r;
    }

    private Resource(Parcel in) {
        Bundle b = in.readBundle();
        mURL = b.getString(URL_KEY);
        mContainerURL = b.getString(CONTAINER_URL_KEY, "invalid url");
        mTitle = b.getString(TITLE_KEY, "bad title");
        mFilename = b.getString(FILENAME_KEY, "bad filename");
        mFilesize = b.getLong(FILESIZE_KEY, 0);
        mCurrentFilesize = b.getLong(CURRENT_FILESIZE_KEY, 0);
        mDownloadUri = b.getParcelable(DOWNLOAD_URI_KEY);
        mIsLocked = b.getBoolean(IS_LOCKED_KEY, true);
        String directory = b.getString(DIRECTORY_KEY, mDownloadDirectory.toString());
        mDownloadDirectory = new File(directory);
    }

    public static final Parcelable.Creator<Resource> CREATOR = new Parcelable.Creator<Resource>() {
        public Resource createFromParcel(Parcel in) {
            return new Resource(in);
        }

        public Resource[] newArray(int size) {
            return new Resource[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle b = new Bundle();
        b.putString(URL_KEY, mURL);
        b.putString(CONTAINER_URL_KEY, mContainerURL);
        b.putString(TITLE_KEY, mTitle);
        b.putString(FILENAME_KEY, mFilename);
        b.putString(DIRECTORY_KEY, mDownloadDirectory.toString());
        b.putLong(FILESIZE_KEY, mFilesize);
        b.putLong(CURRENT_FILESIZE_KEY, mCurrentFilesize);
        b.putParcelable(DOWNLOAD_URI_KEY, mDownloadUri);
        b.putBoolean(IS_LOCKED_KEY, mIsLocked);
        dest.writeBundle(b);
    }

    public String toString() {
        //return "Resource[filename: " + mFilename + ", f: " + mURL + "]";
        return "Resource[" + mDownloadDirectory.toString() + "/" + mFilename + "]";
    }

    public String getContainerURL() {
        return mContainerURL;
    }

    public String getURL() {
        return mURL;
    }

    public String getURLLastModified() {
        return mLastMod;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getFilename() {
        return mFilename;
    }

    public Long getFilesizeOnDisk() {
        // Inspect the file directly, for accuracy's sake
        File file = new File(mDownloadDirectory + "/" + mFilename);
        long filesize = 0;

        if (file.exists()) {
            filesize = file.length();
        }

        return filesize;
    }

    public void deleteFile() {
        File file = new File(getPath(), getFilename());
        if (file.exists()) {
            file.delete();
        }
    }

    public OutputStream getOutputFileStream(boolean append) throws FileNotFoundException {
        return new FileOutputStream(new File(mDownloadDirectory, mFilename), append);
    }

    public File getPath() {
        return mDownloadDirectory;
    }

    public Resource setFilesize(long size) {
        mFilesize = Long.valueOf(size);
        return this;
    }

    public Long getFilesize() {
        return mFilesize;
    }

    public Resource setFilesize(String size) {
        if (size == null) {
            size = "0";
        }
        mFilesize = Long.valueOf(size);
        return this;
    }

    public Resource setCurrentFilesize(String size) {
        if (TextUtils.isEmpty(size) || size == null) {
            size = "0";
        }
        mCurrentFilesize = Long.valueOf(size);
        return this;
    }

    public Resource setCurrentFilesize(long size) {
        mCurrentFilesize = Long.valueOf(size);
        return this;
    }

    public long getCurrentFilesize() {
        return mCurrentFilesize;
    }

    public int getPercentComplete() {
        return VizUtils.percentComplete(mCurrentFilesize, mFilesize);
    }

    public Resource setTitle(String title) {
        mTitle = title;
        return this;
    }

    public Resource setContainerURL(String url) {
        mContainerURL = url;
        return this;
    }

    public Resource setURL(String url) {
        mURL = url;
        return this;
    }

    public Resource setURLLastModified(String lastModified) {
        mLastMod = lastModified;
        return this;
    }

    public boolean isLocked() {
        return mIsLocked;
    }

    public File getDownloadDirectory() {
        return mDownloadDirectory;
    }

    public Resource setDownloadDirectory(File directory) {
        mDownloadDirectory = directory;
        return this;
    }

    public Resource setFilename(String filename) {
        mFilename = filename;
        return this;
    }

    public Resource setUnlocked() {
        mIsLocked = false;
        return this;
    }

    /** The uri that represents this resource in the downloads table. */
    public void setDownloadUri(Uri uri) {
        mDownloadUri = uri;
    }

    public Uri getDownloadUri() {
        return mDownloadUri;
    }

    public ContentValues toContentValues() {
        ContentValues map = new ContentValues();
        map.put(ResourcesColumns.FILENAME, getFilename());
        map.put(ResourcesColumns.FILESIZE, String.valueOf(getFilesize()));
        map.put(ResourcesColumns.DIRECTORY, getPath().toString());
        map.put(ResourcesColumns.TITLE, getTitle());
        map.put(ResourcesColumns.CONTAINER_URL, getContainerURL());
        map.put(ResourcesColumns.URL, getURL());
        // only valid after having been stored as a download
        if (mDownloadUri != null) {
            // careful here, what if this is a download and not a resource?
            Log.d("Storing int id as uri:  " + VizContract.Downloads.getDownloadId(mDownloadUri));
            map.put(ResourcesColumns.DOWNLOAD_ID,
                    VizContract.Downloads.getDownloadId(mDownloadUri));
        }
        return map;
    }

    public static Resource createNullResource() {
        return sNullResource;
    }

    public static boolean isNull(Resource r) {
        if (r == null) {
            return true;
        }
        return r.equals(sNullResource);
    }
}
