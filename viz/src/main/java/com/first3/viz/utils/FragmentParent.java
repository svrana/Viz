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

import android.app.Fragment;

import com.first3.viz.ui.ActivityDelegate;


public class FragmentParent extends Fragment {

    public ActivityDelegate getActivityDelegate() {
        return (ActivityDelegate) getActivity();
    }

    public void sendMessage(int what, int command, Object obj) {
        ActivityDelegate delegate = getActivityDelegate();
        if (delegate != null) {
            delegate.sendMessage(what, command, obj);
        }
    }
}
