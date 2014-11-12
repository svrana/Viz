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

import com.first3.viz.VizApp;
import com.first3.viz.utils.Log;

public class BlinkxResourceBuilder extends JSResourceBuilder {
    @Override
    public void injectJS(final WebView wv) {
        final String js = "javascript:"
                        + "$('.playerDiv').unbind('click');"
                        + "var lnk = function(){"
                        + "$('.playerDiv').unbind('click').click(function(){if($('[id^=\"video\"]', this).attr('src')){alert('http://blinkx.com' + '%%__%%' + $('.playerInfo h2', this).text() + '%%__%%'+$('[id^=\"video\"]', this).attr('src'));}}"
                        + ")};" + "lnk();";

        Runnable task = new Runnable() {
            @Override
            public void run() {
                Log.d("Inject JS");
                wv.loadUrl(js);
            }
        };
        VizApp.getHandler().postDelayed(task, 1500);
    }

    @Override
    public boolean canParse() {
        if (mURL != null) {
            String file = mURL.toExternalForm();
            boolean canParse = file.contains("blinkx");
            return canParse;
        }
        return false;
    }

}
