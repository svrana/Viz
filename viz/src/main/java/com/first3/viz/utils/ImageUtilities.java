/*
 * Copyright (C) 2008 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Addendum:
 *  Modified code from Romain's project Shelves @ code.google.com/p/shelves
 *  Removed everything but his cache.
 *  Added image sampling, i.e., calculateInSampleSize
 */
package com.first3.viz.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.first3.viz.ui.FastBitmapDrawable;
import com.first3.viz.Constants;

public class ImageUtilities {
    // Use a concurrent HashMap to support multiple threads?
    private static final HashMap<Uri, SoftReference<FastBitmapDrawable>> sArtCache = Maps.newHashMap();

    private ImageUtilities() {
    }

    /**
     * Deletes the specified drawable from the cache.
     *
     * @param id The uri of the drawable to delete from the cache
     */
    public static void deleteCachedCover(Uri uri) {
        sArtCache.remove(uri);
    }

    /**
     * Retrieves a drawable from the image cache, identified by the uri.
     * If the drawable does not exist in the cache, it is loaded and added to the cache.
     * If the drawable cannot be added to the cache, a null drawable is
     * returned.
     *
     * @param uri The id of the drawable to retrieve
     *
     * @return The drawable identified by the uri or a null drawable
     */
    public static FastBitmapDrawable getCachedBitmap(Context context, Uri uri,
            int width, int height) {
        FastBitmapDrawable drawable = null;

        SoftReference<FastBitmapDrawable> reference = sArtCache.get(uri);
        if (reference != null) {
            drawable = reference.get();
        }

        if (drawable == null) {
            final Bitmap bitmap = loadBitmap(context, uri, width, height);
            if (bitmap != null) {
                drawable = new FastBitmapDrawable(bitmap);
                sArtCache.put(uri, new SoftReference<FastBitmapDrawable>(drawable));
            }
        }

        return drawable;
    }

    /**
     * Removes all the callbacks from the drawables stored in the memory cache. This
     * method must be called from the onDestroy() method of any activity using the
     * cached drawables. Failure to do so will result in the entire activity being
     * leaked.
     */
    public static void cleanupCache() {
        for (SoftReference<FastBitmapDrawable> reference : sArtCache.values()) {
            final FastBitmapDrawable drawable = reference.get();
            if (drawable != null) {
                drawable.setCallback(null);
            }
        }
    }
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        //Log.d("thumbnail width: " + width + " height: " + height);

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                //Log.v("Calculating insample size: width greater than
                //height");
                inSampleSize = Math.round((float)height / (float)reqHeight);
            } else {
                //Log.v("Calculating insample size: width less than height");
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }
        }
        return inSampleSize;
    }

    private static Uri getDefaultThumbnailUri() {
        File defaultThumbnail = new File(Constants.DEFAULT_THUMBNAIL_FILENAME);
        return Uri.fromFile(defaultThumbnail);
    }

    private static InputStream getInputStream(Context context, Uri uri) {
        InputStream in;

        if (getDefaultThumbnailUri().equals(uri)) {
            //Log.i("Found default thumbnail");
            try {
                in = context.getAssets().open("thumbnail_default.jpg");
            } catch (IOException e) {
                Log.w("Could not open default thumbnail");
                return null;
            }
        } else {
            try {
                in = context.getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                Log.w("Could not find thumbnail for " + uri);
                return null;
            };
        }

        return in;
    }

    private static Bitmap loadBitmap(Context context, Uri uri, int width, int height) {
        InputStream in = getInputStream(context, uri);
        if (in == null) {
            Log.w("getInputStream failed to open: " + uri);
            return null;
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, options);

        options.inSampleSize = calculateInSampleSize(options, width, height);
        // need to close and reopen input stream as we're now at the end of it
        IOUtilities.closeStream(in);

        //Log.v("Dimensions: height: " +  height + ", width: " + width);
        //Log.v("Calculated inSampleSize of " + options.inSampleSize);

        // repeated on purpose
        in = getInputStream(context, uri);
        if (in == null) {
            Log.w("Could not find thumbnail for " + uri);
            return null;
        }

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
        IOUtilities.closeStream(in);
        return bitmap;
    }
}
