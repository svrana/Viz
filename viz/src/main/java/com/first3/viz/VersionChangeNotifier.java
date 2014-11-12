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

import com.first3.viz.utils.Utils;

/**
 * A class that notifies another class when the app version has changed.
 */
public class VersionChangeNotifier {
    private static final VersionChangeNotifier instance = new VersionChangeNotifier();
    private static final String ON_APP_UPGRADE = "version-started";

    public static VersionChangeNotifier getInstance() {
        return instance;
    }

    public String getVersionPrefString() {
        return ON_APP_UPGRADE + "-v" + Utils.getVersion(VizApp.getContext());
    }

    public void start(final Listener listener) {
        if (VizApp.getPrefs().getBoolean(getVersionPrefString(), false)) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    /* Prevent never-ending crashes by updating preference
                     * before calling the listener.
                     */
                    VizApp.getPrefs().edit()
                                     .putBoolean(getVersionPrefString(), true)
                                     .commit();
                    listener.onAppUpgrade();
                }
            };

            VizApp.getHandler().post(task);
        }
    }

    /** All methods called on the UI thread */
    public interface Listener {
        public void onAppUpgrade();
    }
}
