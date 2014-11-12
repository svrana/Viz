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

import com.first3.viz.content.ContentType;
import com.first3.viz.models.Resource;
import com.first3.viz.utils.FetchContainerTask;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.VizUtils;

/**
 * The preferred implementation of ResourceBuilder as it doesn't require
 * downloading anything and as a result is fast.
 */
public abstract class JSResourceBuilder implements ResourceBuilder {
    String mFilename;
    String mTitle;
    URL mURL;

    public abstract boolean canParse();

    public abstract void injectJS(final WebView wv);

    @Override
    public void setURL(URL url) {
        mURL = url;
    }

    @Override
    public boolean isJSType() {
        return true;
    }

    @Override
    public boolean shouldInterceptPageLoad() {
        return false;
    }

    @Override
    public boolean isContainerURL() {
        return false;
    }

    @Override
    public boolean fetchContainer(FetchContainerTask task) {
        return false;
    }

    @Override
    public String getTitle(WebView wv) {
        return mTitle;
    }

    @Override
    public String getDefaultFilename(WebView wv) {
        return mFilename;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public void setFilename(String filename) {
        mFilename = filename;
        Log.d("Setting filename to: " + mFilename);
    }

    @Override
    public Resource build() {
        return Resource.create().setFilename(mFilename)
                                .setTitle(mTitle)
                                .setURL(mURL.toExternalForm());
    }

    @Override
    public void setTitle(String title) {
        mTitle = VizUtils.normalizeTitle(title);

        setFilename(VizUtils.normalizeFilename(mTitle,
                    getContentType().toString()));
    }

    private ContentType getContentType() {
        return ContentType.MP4;
    }
}
