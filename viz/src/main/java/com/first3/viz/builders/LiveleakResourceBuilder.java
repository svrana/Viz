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

import com.first3.viz.content.ContentSource;
import com.first3.viz.content.ContentType;
import com.first3.viz.download.Container;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.StringBuffer;

public class LiveleakResourceBuilder extends ContainerResourceBuilder {
    private String mURLstr;

    @Override
    public ContentSource getContentSource() {
        return ContentSource.LIVELEAK;
    }

    @Override
    public String getDownloadURL(Container container) {
        Log.d();
        StringBuffer sb = StringBuffer.fromString(container.toString());

        mTitle = sb.stringBetween("<title>LiveLeak.com - ", "</title>");
        if (mTitle == null) {
            mTitle = sb.stringBetween("<title>", "</title>");
        }
        Log.d("found title: " + mTitle);
        mURLstr = sb.stringBetween("file: \"", "\",");
        Log.d("found URL: " + mURLstr);
        return mURLstr;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.MP4;
    }

    /**
     * url like: http://www.liveleak.com/view?i=7ef_1331661603
     */
    @Override
    public boolean canParse() {
        String file = mURL.toExternalForm();
        if (file.matches(".*/view?.*")) {//file.matches(".*/watch?.*") &
            Log.d("can parse url " + file);
            return true;
        }
        Log.d("cannot parse " + file);
        return false;
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
