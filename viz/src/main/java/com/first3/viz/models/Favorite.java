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

import java.util.Collection;

import com.first3.viz.Preferences;
import com.first3.viz.VizApp;
import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentSources;
import com.first3.viz.provider.VizContract.Favorites;
import com.first3.viz.provider.VizContract.FavoritesColumns;
import com.first3.viz.provider.VizDatabase;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Utils;

import android.content.ContentValues;
import android.database.Cursor;

import android.graphics.Bitmap;
import android.net.Uri;

public class Favorite {
    int id;
    final String title;
    final String url;
    final Bitmap favicon;

    private Favorite(String title, String url, Bitmap favicon) {
        this.title = title;
        this.url = url;
        this.favicon = favicon;
    }

    private Favorite(int id, String title, String url, Bitmap favicon) {
        this(title, url, favicon);
        this.id = id;
    }

    public static Favorite newInstance(String title, String url, Bitmap favicon) {
        return new Favorite(title, url, favicon);
    }

    public static Favorite fromCursor(VizDatabase.FavoritesCursor cursor) {
        return new Favorite(cursor.getColId(), cursor.getColName(), cursor.getColURL(),
                            cursor.getColFavicon());
    }

    public String toString() {
        return "Favorite[id= " + id + ", title=" + title + ", url=" + url + "]";
    }

    public String getTitle() {
        return title;
    }

    // Probably should return an ImageView
    public Bitmap getFavicon() {
        return favicon;
    }

    public byte[] getFaviconByteArray() {
        return Utils.bitmapToByteArray(favicon);
    }

    public String getUrl() {
        return url;
    }

    public int getRank() {
        return 100;
    }

    public int getId() {
        return id;
    }

    public ContentValues toContentValues() {
        ContentValues map = new ContentValues();
        map.put(FavoritesColumns.RANK, getRank());
        map.put(FavoritesColumns.NAME, getTitle());
        map.put(FavoritesColumns.URL, getUrl());
        if (getFavicon() != null) {
            map.put(FavoritesColumns.FAVICON, getFaviconByteArray());
        }
        return map;
    }

    public static Uri getUriFromUrl(String url) {
        Uri uri = null;
        String selection = Favorites.URL + "=?";
        Cursor cursor = VizApp.getResolver().query(Favorites.CONTENT_URI,
                new String[] { Favorites._ID, Favorites.URL }, selection,
                new String[] { url }, Favorites.DEFAULT_SORT);
        if (cursor.moveToFirst()) {
            int favoriteId = cursor.getInt(cursor.getColumnIndex(Favorites._ID));
            uri = Favorites.buildFavoriteUri(favoriteId);
        }
        cursor.close();
        return uri;
    }

    public static void updateFavicon(Uri favoriteUri, Bitmap icon) {
        ContentValues map = new ContentValues();
        map.put(Favorites.FAVICON, Utils.bitmapToByteArray(icon));
        VizApp.getResolver().update(favoriteUri, map, null, null);
    }

    private static boolean existsAsFavorite(String url) {
        return getUriFromUrl(url) != null;
    }

    /**
     * Checks to see if the url of the ContentSource has been added as a
     * default favorite before or if the url is already present as a favorite.
     */
    public static boolean hasBeenAdded(ContentSource source) {
        String pref = Preferences.contentSourcePreferenceString(source);
        if (VizApp.getPrefs().getBoolean(pref, false)) {
            return true;
        }
        return existsAsFavorite(source.getURL());
    }

    public static void addContentSourceAsFavorite(ContentSource source) {
        ContentValues map = new ContentValues();
        map.put(FavoritesColumns.RANK, source.getRank());
        map.put(FavoritesColumns.NAME, source.getSite());
        map.put(FavoritesColumns.URL, source.getURL());

        String pref = Preferences.contentSourcePreferenceString(source);
        VizApp.getPrefs().edit().putBoolean(pref, true).commit();

        Log.i("Adding site to favorites: " + source.getSite());
        VizApp.getResolver().insert(Favorites.CONTENT_URI, map);
    }

    public static void addSupportedSites() {
        Collection<ContentSource> sources = ContentSources.getContentSources();
        for (ContentSource s : sources) {
            if (s.addToFavorites() && !hasBeenAdded(s)) {
                addContentSourceAsFavorite(s);
            }
        }
    }
}
