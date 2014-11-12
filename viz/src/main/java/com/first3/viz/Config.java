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


package com.first3.viz;

/* GPL */

/**
 * This file used to be generated at build time. That part of the build
 * process was not updated during the switch to gradle. It is mostly
 * unimportant now (it's main use was to facilitate doing different things for
 * different market stores and for premium/free versions).
 */
public class Config
{
    /**
     * The string used by the Provider to locate its database.  This is
     * always com.first.viz unless it's the free Amazon version then it's
     * com.first.vizfree;
     */
    public static final String CONTENT_AUTHORITY = "com.first3.viz";

    /** Whether or not this is an Amazon build */
    public static final boolean isAmazonVersion() {
        return false;
    }

    public static final boolean forceVimeoSearchFix() {
        return false;
    }
}
