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

import com.first3.viz.content.ContentType;
import com.first3.viz.download.Container;

public abstract class FlashPlayerResourceBuilder extends ContainerResourceBuilder {

    /*
     * We can assume we can parse the URL here b/c this is a secondary
     * builder, i.e., user isn't interacting with the site, presumably another
     * builder that knows what it's doing is interacting with the site.
     */
    @Override
    public boolean canParse() {
        return true;
    }

    @Override
    public String getTitle(Container container) {
        return mTitle;
    }

    @Override
    public void setTitle(String title) {
        mTitle = title;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.MP4;
    }

    @Override
    public boolean isContainerURL() {
        return true;
    }
}
