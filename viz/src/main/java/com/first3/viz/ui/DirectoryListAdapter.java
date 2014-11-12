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

package com.first3.viz.ui;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.first3.viz.R;
import com.first3.viz.VizApp;

public class DirectoryListAdapter extends BaseAdapter implements ListAdapter {
    public final static int IMAGE_VIEWTYPE = 0;
    public final static int TEXT_VIEWTYPE = 1;

    private LinearLayout dirUpLayout;
    private ArrayList<TextView> children;
    private boolean isTopLevel;

    public DirectoryListAdapter() {
        super();
        children = new ArrayList<TextView>();
        isTopLevel = false;
    }

    public void addDirectories(File parentDirectory, List<String> folderList) {
        clear();

        for (String s : folderList) {
            add(s);
        }

        setTopLevel(parentDirectory.getParent() == null);

        notifyDataSetChanged();
    }

    private void add(String filename) {
        TextView tv = (TextView) VizApp.getInflator().inflate(android.R.layout.simple_list_item_1, null);
        tv.setText(filename);
        children.add(tv);
    }

    public void clear() {
        children.clear();
    }

    @Override
    public int getCount() {
        if (isTopLevel) {
            return children.size();
        } else {
            return children.size() + 1;
        }
    }

    @Override
    public View getItem(int position) {
        if (!isValidPosition(position)) {
            throw new InvalidParameterException("Invalid paramter: " + position);
        }

        if (isTopLevel) {
            return children.get(position);
        } else if (position == 0) {
            return dirUpLayout;
        }

        return children.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        if (!isValidPosition(position)) {
            return 0;
        } else {
            return getView(position, dirUpLayout, null).getId();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getItem(position);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && dirUpLayout != null) {
            return IMAGE_VIEWTYPE;
        } else {
            return TEXT_VIEWTYPE;
        }
    }

    public void setTopLevel(boolean isTopLevel) {
        this.isTopLevel = isTopLevel;
        if (isTopLevel) {
            dirUpLayout = null;
        } else {
            dirUpLayout = (LinearLayout) VizApp.getInflator().inflate(R.layout.directory_up, null);
        }
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < getCount());
    }
}
