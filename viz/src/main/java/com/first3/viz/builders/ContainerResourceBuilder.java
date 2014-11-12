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

package com.first3.viz.builders;

import java.net.URL;

import android.webkit.WebView;

import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentType;
import com.first3.viz.download.Container;
import com.first3.viz.download.StringContainer;
import com.first3.viz.models.Resource;
import com.first3.viz.utils.FetchContainerTask;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.VizUtils;

/**
 * This builder is typically used when you need to download a webpage
 * and parse its contents in order to determine the download location
 * of the video the user would like to download.
 */
public abstract class ContainerResourceBuilder implements ResourceBuilder {
    protected String mTitle;
    protected String mFilename;
    protected String mErrorMsg;
    protected URL mURL;
    protected String mDefaultFilename;
    protected Container mContainer;
    protected String mDownloadURL;

    public abstract String getDownloadURL(Container container);
    public abstract ContentType getContentType();
    public abstract ContentSource getContentSource();

    public String getContainerURL() {
        return mURL.toExternalForm();
    }

    @Override
    public boolean isJSType() {
        return false;
    }

    @Override
    public void injectJS(WebView wv) {
    }

    /**
     * Implementers of this type must override this.
     */
    @Override
    public boolean canParse() {
        return false;
    }

    @Override
    public boolean shouldInterceptPageLoad() {
        return false;
    }

     public final Container getDocument(FetchContainerTask task, ContentSource source, String sURL) {
        Container container = new StringContainer();
        if (container.downloadURL(task, sURL)) {
            return container;
        }
        return null;
     }

    @Override
    public boolean isContainerURL() {
        return true;
    }

    /*
     * Cannot be called on the UI thread.
     */
    @Override
    public boolean fetchContainer(FetchContainerTask task) {
        String sURL = getContainerURL();
        if (sURL == null) {
            Log.d("Null container url");
            return false;
        }

        mContainer = getDocument(task, getContentSource(), sURL);
        if (mContainer == null) {
            mErrorMsg = "Could not fetch container URL " + sURL;
            return false;
        }

        mDownloadURL = getDownloadURL(mContainer);
        if (mDownloadURL == null) {
            mErrorMsg = "Could not get download URL";
            Log.w(mErrorMsg);
            return false;
        }
        Log.d("Download URL: " + mDownloadURL);

        mTitle = getTitle(mContainer);
        if (mTitle == null) {
            mErrorMsg = "Could not get download title";
            Log.w(mErrorMsg);
            return false;
        }
        mTitle = VizUtils.normalizeTitle(mTitle);
        Log.d("Title: " + mTitle);

        setDefaultFilename(mTitle);
        Log.d("Default filename: " + mDefaultFilename);

        return true;
    }

    @Override
    public String getTitle(WebView wv) {
        return mTitle;
    }

    public String getTitle(Container container) {
        return mTitle;
    }

    @Override
    public String getDefaultFilename(WebView wv) {
        return mDefaultFilename;
    }

    public void setDefaultFilename(String title) {
        mDefaultFilename = VizUtils.normalizeFilename(title, getContentType().toString());
    }

    @Override
    public String getErrorMessage() {
        return mErrorMsg;
    }

    @Override
    public void setFilename(String filename) {
        mFilename = filename;
    }

    @Override
    public Resource build() {
        Log.d("Building Resource for [" + mURL.toExternalForm() + "]");
        Resource resource = Resource.create()
            .setContainerURL(getContainerURL())
            .setURL(mDownloadURL)
            .setFilename(mFilename)
            .setTitle(mTitle);
        mContainer = null;
        return resource;
    }

    @Override
    public void setTitle(String title) {
        mTitle = VizUtils.normalizeTitle(title);
        setDefaultFilename(title);
    }

    @Override
    public void setURL(URL url) {
        mURL = url;
    }
}
