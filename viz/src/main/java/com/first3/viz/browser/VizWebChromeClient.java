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

package com.first3.viz.browser;

import java.net.URL;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.first3.viz.builders.ResourceBuilder;
import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentSources;
import com.first3.viz.models.Favorite;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Utils;

class VizWebChromeClient extends WebChromeClient {
    private Browser mBrowser;
    private String mFavIconUrl;
    private URL mFavoriteUrl;

    VizWebChromeClient(Browser browser) {
        mBrowser = browser;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        ProgressBar progressBar = mBrowser.getProgressBar();
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(progress);
            if (progress == 100) {
                // setting visibility to GONE will change the layout
                progressBar.setVisibility(View.INVISIBLE);
                progressBar.setProgress(0);
            }
        }
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        result.confirm();
        Log.d("JS Launch: " + message);
        super.onJsAlert(view, url, message, result);
        String details[] = message.split("%%__%%");
        if (details.length < 3) {
            Log.d("JS Launch less three 3 params");
            return true;
        }
        String contentUrl = details[0];
        String title = details[1];
        URL downloadUrl = Utils.urlFromString(details[2]);

        if (contentUrl == null || contentUrl.length() == 0) {
            Log.e("JS could not find content url");
            return true;
        }
        if (title == null || title.length() == 0) {
            Log.e("JS could not find title");
            return true;
        }
        if (downloadUrl == null) {
            Log.e("JS could not find url");
            return true;
        }

        Log.d("Content URL: " + contentUrl);
        Log.d("Title: " + title);
        Log.d("Download URL: " + downloadUrl);

        ContentSource source = ContentSources.newInstance(Utils.urlFromString(contentUrl));
        ResourceBuilder builder = source.getResourceBuilder();
        // Notice that the url set here is different than the one the builder
        // was retrieved with.
        builder.setURL(downloadUrl);
        if (!builder.canParse()) {
            return false;
        }

        builder.setTitle(title);

        mBrowser.confirmDownload(builder);
        return true;
    }

    public void storeFavIcon(String url) {
        mFavIconUrl = url;
        mFavoriteUrl = Utils.urlFromString(mFavIconUrl);
    }

    @Override
    public void onReceivedIcon(WebView webview, final Bitmap icon) {
        super.onReceivedIcon(webview, icon);

        if (icon == null || mFavoriteUrl == null || mFavIconUrl == null) {
            return;
        }
        URL wvUrl = Utils.urlFromString(webview.getUrl());
        if (wvUrl == null) {
            return;
        }

        if (mFavoriteUrl.getHost().equals(wvUrl.getHost())) {
            Log.d("Updating favorite with favicon");

            // making a copy on the stack so we don't have to set the value to
            // null in another thread
            final String favIconUrl = mFavIconUrl;

            mFavIconUrl = null;
            mFavoriteUrl = null;

            Thread t = new Thread() {
                @Override
                public void run() {
                    Uri favoriteUri = Favorite.getUriFromUrl(favIconUrl);
                    if (favoriteUri != null) {
                        Favorite.updateFavicon(favoriteUri, icon);
                    }
                }
            };
            Utils.threadStart(t, "Failed to update favorite url " + mFavIconUrl);
        }
    }
}
