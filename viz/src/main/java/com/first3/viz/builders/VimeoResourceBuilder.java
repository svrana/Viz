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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.webkit.WebView;

import com.first3.viz.Config;
import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.content.ContentSource;
import com.first3.viz.download.Container;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.StringBuffer;
import com.first3.viz.utils.Utils;

public class VimeoResourceBuilder extends CombinedResourceBuilder {
    private final static String VIMEO_DETAIL = ".*vimeo.com/m/\\d+";
    private final static String VIMEO_LOGIN = ".*(vimeo.com/m/log_in|vimeo.com/log_in)";

    private boolean useVimeoSearchFix() {
        return Utils.isLowerThanKitkat() || Config.forceVimeoSearchFix();
    }

    @Override
    public boolean isJSType() {
        return mURL.toString().equals("http://vimeo.com/m/") ||
               mURL.toString().equals("https://vimeo.com/m/") ||
               (useVimeoSearchFix() &&
                    mURL.toString().contains("vimeo.com/m/search"));
    }

    @Override
    public void injectJS(WebView wv) {
        String js = "javascript:";

        // KitKat's webview handles the css animation in Vimeo's search
        // box so this isn't needed.
        String searchFix = "$('.faux_players').unbind('click');"
                        + "$('#header').replaceWith('<header><form name=\"input\" "
                        + "action=\"search\"><input type=\"text\" name=\"q\" class=\"viz\" "
                        + "style=\"height:35px;width:80%;margin-left:5px\"></input>"
                        + "<button class=\"btn viz-search\""
                        + "style=\"height:35px;width:15%;margin:5px;\">"
                        + VizApp.getResString(R.string.search)
                        + "</button></form></header>');";

        if (useVimeoSearchFix()) {
            js += searchFix;
        }

        String downloadJs = "var lnk = function(){"
                        + "$('.faux_player').unbind('click').click("
                        + "function() { alert('http://vimeo.com/m' + '%%__%%' "
                        + "+ $('video', this).attr('data-clip_title') + '%%__%%'"
                        + "+ $('video', this).attr('data-src'));}"
                        + ")};" + "lnk();";

        js += downloadJs;

        wv.loadUrl(js);
    }

    @Override
    public boolean canParse() {
        String sURL = mURL.toExternalForm();
        if (sURL.contains("vimeo") && !sURL.matches(VIMEO_LOGIN)) {
            Log.d("can Parse " + sURL);
            return true;
        }
        Log.d("cannot Parse " + sURL);
        return false;
    }

    @Override
    public String getDownloadURL(Container container) {
        StringBuffer sb = StringBuffer.fromString(container.toString());

        mTitle = sb.stringBetween("<title>", "</title>");

        String url = sb.stringBetween("\"url\":\"", "\",\"");
        return url;
    }

    @Override
    public String getContainerURL() {
        Log.d(mURL.getFile());
        String id = getVideoIdFromURL();
        if (id != null) {
            return "http://player.vimeo.com/video/" + id;
        }
        return null;
    }

    private String getVideoIdFromURL() {
        String url = mURL.toString();
        Pattern pattern = Pattern.compile(".*vimeo.com/m/");
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            int index = matcher.end();
            return url.substring(index);
        }
        return null;
    }

    @Override
    public ContentSource getContentSource() {
        return ContentSource.VIMEO;
    }

    @Override
    public boolean isContainerURL() {
        if (mURL.toString().matches(VIMEO_DETAIL)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldInterceptPageLoad() {
        return mURL.toString().matches(VIMEO_DETAIL);
    }
}
