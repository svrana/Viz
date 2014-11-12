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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentType;
import com.first3.viz.download.Container;
import com.first3.viz.utils.Log;

public class MetacafeResourceBuilder extends ContainerResourceBuilder {
    @Override
    public boolean canParse() {
        String file = mURL.toExternalForm();
        if (file.matches(".*/watch/.*")) {
            Log.d("can parse " + file);
            return true;
        }
        Log.d("cannot parse " + file);
        return false;
    }

    @Override
    public String getDownloadURL(Container container) {
        Document document = Jsoup.parse(container.toString());
        Element title = document.select("title").first();
        mTitle = title == null ? null : title.text();
        Element links = document.select("#FlashWrap > video").first();
        return links == null ? null : links.attr("src");
    }

    @Override
    public ContentType getContentType() {
        return ContentType.MP4;
    }

    @Override
    public ContentSource getContentSource() {
        return ContentSource.MCAFE;
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
