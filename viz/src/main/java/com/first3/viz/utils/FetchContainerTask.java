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

package com.first3.viz.utils;

import com.first3.viz.builders.ResourceBuilder;

import android.os.AsyncTask;

/**
 * Helper class to pass this particular task around without having to specify
 * the type each time.
 */
public abstract class FetchContainerTask extends AsyncTask<ResourceBuilder, Void, Void> {
    /**
     * Run threads in parallel.
     */
    public AsyncTask<ResourceBuilder, Void, Void> run(ResourceBuilder...param ) {
        if (Utils.isHoneycombOrHigher()) {
            return executeOnExecutor(THREAD_POOL_EXECUTOR, param);
        } else {
            return execute(param);
        }
    }
}
