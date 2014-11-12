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

/**
 * A ResourceBuilder that combines both JavaScript and Container downloading
 * and parsing.  Use a JSResourceBuilder if you can..
 */
public abstract class CombinedResourceBuilder extends ContainerResourceBuilder {
    @Override
    public abstract boolean canParse();

    @Override
    public abstract boolean shouldInterceptPageLoad();

    @Override
    public abstract void injectJS(final WebView wv);

    @Override
    public void setURL(URL url) {
        mURL = url;
        mDownloadURL = mURL.toExternalForm();
    }

    @Override
    public boolean isJSType() {
        return true;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.MP4;
    }
}
