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

import android.webkit.WebView;

import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentType;
import com.first3.viz.download.Container;
import com.first3.viz.utils.StringBuffer;

public class GoGoAnimeResourceBuilder extends ContainerResourceBuilder {
    @Override
    public boolean canParse() {
        String file = mURL.toExternalForm();
        boolean canParse = file.contains("gogoanime");
        return canParse;
    }

    @Override
    public boolean isContainerURL() {
        return true;
    }

    @Override
    public boolean isJSType() {
        return true;
    }

    /*
     * GoGoAnime uses iframe to load the flash player. In order to get the
     * touch, injects a JS that changes the iframe by divs. Unfortunately
     * gogoanime doesn't host videos, so the app gets a link to another site and
     * this is the one that it's parsed in order to get the url.
     */
    @Override
    public void injectJS(WebView wv) {
        String js = "javascript:"
            + "var lnk = function(){"
            + "jQuery('.postcontent iframe').each(function(index) "
            + "{jQuery(this).replaceWith('<h1><a class=\"videoAnimeViz\" "
            + "src=\"'+jQuery('.postcontent iframe').attr('src')+'\">"
            + "'+jQuery('title').text()+' Video '+index+'</a></h1>');}); "
            + "jQuery('.videoAnimeViz').unbind('click').click(function(){"
            + "alert(jQuery(this).attr('src')+  '%%__%%' + jQuery(this).text() "
            + "+  '%%__%%' +jQuery(this).attr('src'));});};"
            + "lnk();";

        wv.loadUrl(js);
    }

    @Override
    public String getDownloadURL(Container container) {
        StringBuffer sb = StringBuffer.fromString(container.toString());
        String url = sb.stringBetween("file: ", "\",");
        if (url != null) {
            return url;
        }
        url = sb.stringBetween("{url: \"http:", ", autoPlay: false");
        return url;
    }

    @Override
    public String getContainerURL() {
        return mURL.toString();
    }

    @Override
    public ContentType getContentType() {
        return ContentType.MP4;
    }

    @Override
    public ContentSource getContentSource() {
        return ContentSource.GOGOANIME;
    }
}
