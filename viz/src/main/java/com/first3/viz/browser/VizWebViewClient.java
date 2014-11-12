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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.first3.viz.builders.BlinkxResourceBuilder;
import com.first3.viz.builders.ResourceBuilder;
import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentSources;
import com.first3.viz.ui.ActivityDelegate;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Utils;

public class VizWebViewClient extends WebViewClient {
    EditText mURLBar;
    Browser mBrowser;
    boolean mIgnoreNextPageLoad = false;
    BlinkxResourceBuilder mBlinkxBuilder = new BlinkxResourceBuilder();

    public VizWebViewClient(Browser browser) {
        mBrowser = browser;
    }

    private void continueURLLoading(WebView view, String url) {
        Log.d("(url=" + url + ")");
        mBrowser.loadUrl(url);
    }

    public void goingBack() {
        mIgnoreNextPageLoad = true;
    }

    /**
     * The browser reloads the last page you're on, which causes downloads
     * to commence on startup (or anytime after tab is destroyed). This
     * works around that annoying bug.
     */
    public boolean isBrowserActive() {
        if (mBrowser == null)
            return false;

        ActivityDelegate ad = mBrowser.getActivityDelegate();
        if (ad == null)
            return false;

        return ad.isCurrentTabBrowser();
    }

    private void handleURL(WebView view, URL url, boolean isPageLoaded, boolean isIntercepted) {
        Log.d("(url=" + url + ", isPageLoaded=" + isPageLoaded + ")");

        if (url == null) {
            Log.w("invalid URL");
            return;
        }

        ContentSource source = ContentSources.newInstance(url);
        ResourceBuilder builder = source.getResourceBuilder();

        // This code attempts to prevent downloads from occuring when the
        // 'back' button is pressed. It is ignored for JavaScript builders,
        // that are not triggered by matching URLs.
        if (mIgnoreNextPageLoad) {
            mIgnoreNextPageLoad = false;
            if (!builder.isJSType()) {
                Log.d("Not parsing: " + url.toExternalForm());
                if (!isPageLoaded) {
                    continueURLLoading(view, url.toExternalForm());
                }
                return;
            }
        }

        Log.d("Page Loaded: " + isPageLoaded);

        if (isPageLoaded) {
            // i.e., handle after page load (now!)
            if (builder.isJSType() && builder.canParse()) {
                Log.d("JS Builder found for URL [" + url.toExternalForm() + "]");
                builder.injectJS(view);
                // if download triggered, onJsAlert will be called
                return;
            }
            if (isBrowserActive() && !builder.shouldInterceptPageLoad() && builder.canParse()) {
                Log.d("Page-Loaded Builder found for URL [" + url.toExternalForm() + "]");
                mBrowser.confirmDownload(builder);
                return;
            }
        } else {
            // i.e., we shouldn't progress to the next page
            if (isBrowserActive() && builder.shouldInterceptPageLoad() && builder.canParse()) {
                Log.d("Intercept Builder found for URL [" + url.toExternalForm() + "]");
                mBrowser.confirmDownload(builder);
                return;
            }
            if (isIntercepted) {
                continueURLLoading(view, url.toExternalForm());
            }
        }
    }

    /** Load link locally instead of issuing Intent. */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String sURL) {
        Log.d("shouldOverrideUrlLoading: (url=" + sURL + ")");

        URL url = Utils.urlFromString(sURL);
        if (url == null) {
            // Saw the about:blank page showing up here after clicking on
            // search results in google, no idea why.  The correct fix would
            // be to prevent that from occuring, but wasn't able to figure out
            // how that was occuring.
            Log.d("url is null after conversion, ignoring");
            return true;
        }

        handleURL(view, url, false, true);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String sURL, Bitmap favicon) {
        Log.d("onPageStarted: (url=" + sURL + ")");
        handleURL(view, Utils.urlFromString(sURL), false, false);
    }

    /**
     * For some reason, shouldOverrideUrlLoading is not called for all URLs.
     * Handle such cases here.
     */
    @Override
    public void onPageFinished(WebView view, String sURL) {
        super.onPageFinished(view, sURL);

        Log.d("onPageFinished: (url=" + sURL + ")");

        handleURL(view, Utils.urlFromString(sURL), true, false);
        mBrowser.loadFinished();
    }

    @SuppressLint("NewApi")
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String sURL) {
        //Log.d("shouldInterceptRequest: (url=" + sURL + ")");

        URL link = Utils.urlFromString(sURL);
        if (link != null) {
            mBlinkxBuilder.setURL(link);
            if (mBlinkxBuilder.canParse()) {
                handleURL(view, link, true, false);
            }
        }
        return super.shouldInterceptRequest(view, sURL);
    }
}
