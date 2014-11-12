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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.webkit.WebView;

import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentType;
import com.first3.viz.download.Container;
import com.first3.viz.utils.Log;

public class FunnyOrDieResourceBuilder extends ContainerResourceBuilder {

    @Override
    public boolean isJSType() {
        return true;
    }

    @Override
    public void injectJS(WebView wv) {
        final String js = "javascript:"
                        + "$('.video-preview').unbind('click');"
                        + "var lnk = function(){"
                        + "$('.video-preview').unbind('click').click(function(){alert('http://funnyordie.com' + '%%__%%' + $('a h4', this).text() + '%%__%%'+$('a', this).attr('href'));}}"
                        + ")};" + "lnk();";
        wv.loadUrl(js);
    }

    @Override
    public boolean canParse() {
        String file = mURL.toExternalForm();
        boolean canParse = file.contains("funnyordie");
        Log.d("canParse: file: " + file + ": " + canParse);
        return canParse;
    }

    /*
     * link to video page is
     * http://www.funnyordie.com/videos/af8af15b6c/obamacare-or-shut-up-with-billy-eichner-and-olivia-wilde link to the
     * mp4 is http://vo.fod4.com/v/af8af15b6c/v600.mp4 basically after /videos/ there is an id and you can use for find
     * the mp4 However some of them have different v110 v440 or v600 so best option is just look for the link is on the
     * class="video-link-fallback"
     */
    @Override
    public String getDownloadURL(Container container) {
        Document document = Jsoup.parse(container.toString());
        Element title = document.select("a[itemprop*=name]").first();
        mTitle = title == null ? null : title.text();
        Element links = document.select("a.video-link-fallback[href]").first();
        return links == null ? null : links.attr("href");
    }

    @Override
    public String getTitle(Container container) {
        return mTitle;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.MP4;
    }

    @Override
    public ContentSource getContentSource() {
        return ContentSource.FOD;
    }

    @Override
    public boolean shouldInterceptPageLoad() {
        return true;
    }

    @Override
    public void setURL(URL url) {
        mURL = url;
        mDownloadURL = mURL.toExternalForm();
    }
}
