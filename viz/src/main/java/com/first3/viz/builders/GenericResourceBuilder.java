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

import com.first3.viz.builders.ResourceBuilder;
import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentType;
import com.first3.viz.content.ContentTypes;
import com.first3.viz.models.Resource;
import com.first3.viz.utils.FetchContainerTask;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.StringBuffer;

import android.webkit.WebView;

public class GenericResourceBuilder implements ResourceBuilder
{
    static final String mErrorMsg = "Could not parse url";
    String mDefaultFilename;
    String mFilename;
    String mTitle;
    URL mURL;
    ContentType mType;

    @Override
    public boolean isJSType() {
        return false;
    }

    @Override
    public void injectJS(WebView wv) {
    }

    @Override
    public void setURL(URL url) {
        mURL = url;
    }

    @Override
    public boolean canParse() {
        ContentType type = ContentTypes.fromURL(mURL);
        if (type == ContentType.NULL) {
            return false;
        }
        mDefaultFilename = getFilename(mURL);
        if (mDefaultFilename == null) {
            Log.d("canParse: got null filename");
            return false;
        }
        Log.d("can parse");
        mType = type;
        return true;
    }

    @Override
    public boolean shouldInterceptPageLoad() {
        return true;
    }

    @Override
    public Resource build() {
        return Resource.create()
                    .setFilename(mFilename)
                    .setTitle(mTitle)
                    .setContainerURL(mURL.toExternalForm())
                    .setURL(mURL.toExternalForm());
    }

    public ContentSource getContentSource() {
        return ContentSource.GENERIC;
    }

    private String getFilename(URL url) {
        String file = url.toExternalForm();
        StringBuffer sb = StringBuffer.fromString(file);

        StringBuffer filenameAndQuery = sb.lastAfter("/");
        String filename = filenameAndQuery.stringBefore("?");
        if (filename == null) {
            return filenameAndQuery.toString();
        } else {
            return filename;
        }
    }

    @Override
    public boolean isContainerURL() {
        return false;
    }

    @Override
    public boolean fetchContainer(FetchContainerTask task) {
        // exception
        return false;
    }

    @Override
    public String getTitle(WebView wv) {
        mTitle = null;
        if (wv != null && wv.getTitle() != null) {
            mTitle = wv.getTitle().toString();
            return mTitle;
        }
        return null;
    }

    @Override
    public String getDefaultFilename(WebView wv) {
        return mDefaultFilename;
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
    public String toString() {
        return "GenericResourceBuilder: " + mURL.toExternalForm();
    }

    @Override
    public void setTitle(String title) {
    }
}
