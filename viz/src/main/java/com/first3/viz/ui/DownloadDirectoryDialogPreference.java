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
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Utils;
import com.first3.viz.utils.VizUtils;

public class DownloadDirectoryDialogPreference extends DialogPreference {
    private Spinner directoryHierarchySpinner;
    private ListView directoryListView;
    private Button resetDirectoryButton;
    private DirectoryListAdapter directoryListAdapter;
    private ArrayAdapter<String> directoryHierarchyArrayAdapter;
    private Context appContext;
    private File currentDirectory;

    public DownloadDirectoryDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        appContext = context;
        currentDirectory = VizUtils.getDownloadDir();
        directoryListAdapter = new DirectoryListAdapter();
        directoryHierarchyArrayAdapter = new ArrayAdapter<String>(context, R.layout.directory_path_view);

        setDialogLayoutResource(R.layout.downloaddirectory_dialog);
        setPositiveButtonText(VizApp.getResString(R.string.ok));
        setNegativeButtonText(VizApp.getResString(R.string.cancel));

        setDialogIcon(R.drawable.ic_launcher);
    }

    @Override
    protected void onBindDialogView(View view) {
        Utils.maximizeDialog((Activity) appContext, view);

        resetDirectoryButton = (Button) view.findViewById(R.id.resetDownloadDirectoryButton);
        directoryHierarchySpinner = (Spinner) view.findViewById(R.id.directorySpinner);
        directoryListView = (ListView) view.findViewById(R.id.directoryListView);
        directoryListView.setAdapter(directoryListAdapter);

        directoryHierarchySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, final View view, int pos, long id) {
                TextView tv = (TextView) view;
                openAbsoluteDirectory((String)tv.getText());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        directoryListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String subDirectoryName = "";
                if (directoryListAdapter.getItemViewType(pos) == DirectoryListAdapter.IMAGE_VIEWTYPE) {
                    // This is the "directory up" item
                    subDirectoryName = "..";
                } else {
                    subDirectoryName = (String) ((TextView) view).getText();
                }

                int currentSpinnerPosition = directoryHierarchySpinner.getSelectedItemPosition();

                String hierarchyChildPath = "";
                if (currentSpinnerPosition > 0) {
                    // There is already a subdirectory in the spinner hierarchy, get its value so
                    // we don't rebuild unless the user has taken another path
                    int position = currentSpinnerPosition - 1;
                    hierarchyChildPath = (String) directoryHierarchySpinner.getItemAtPosition(position);
                }

                openSubDirectory(subDirectoryName);

                if (!hierarchyChildPath.endsWith(subDirectoryName)) {
                    // The selected directory isn't present in the current spinner hierarchy,
                    // so we need to rebuild from here
                    setDirectoryHierarchy(currentDirectory);
                } else {
                    // The selected subdirectory is already in the hierarchy, so move to it
                    // no need to check for array bounds
                    directoryHierarchySpinner.setSelection(currentSpinnerPosition - 1);
                }
            }
        });

        resetDirectoryButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAbsoluteDirectory(VizUtils.getVideosPrivatePath());
                setDirectoryHierarchy(currentDirectory);
            }
        });

        getChildDirectories(currentDirectory);
        setDirectoryHierarchy(currentDirectory);

        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        // Only save the result if the user clicked Ok
        if (positiveResult && (currentDirectory != null)) {
            VizUtils.setDownloadDir(currentDirectory);
            callChangeListener(currentDirectory);
        } else {
            currentDirectory = VizUtils.getDownloadDir();
        }
    }

    private void setDirectoryHierarchy(File currentDirectory) {
        List<String> hierarchyList = new ArrayList<String>();

        for (File tmpFile = currentDirectory; tmpFile != null; tmpFile = tmpFile.getParentFile()) {
            hierarchyList.add(tmpFile.getAbsolutePath());
        }

        directoryHierarchyArrayAdapter.clear();
        for (String s : hierarchyList) {
            directoryHierarchyArrayAdapter.add(s);
        }

        directoryHierarchySpinner.setAdapter(directoryHierarchyArrayAdapter);
        directoryHierarchySpinner.setSelection(0);
    }

    /**
     * Returns all visible child directories for which the user has r/w access
     *
     * @param parentDirectory
     * @return children String[] of child names
     */
    private void getChildDirectories(final File parentDirectory) {
        List<String> folderList = new ArrayList<String>();

        if ((parentDirectory != null) && (parentDirectory.list() != null)) {
            for (File f : parentDirectory.listFiles()) {
                if (f.isDirectory() && f.canRead() && !f.isHidden()) {
                    folderList.add(f.getName());
                }
            }

            directoryListAdapter.addDirectories(parentDirectory, folderList);
        }
    }

    // naming is off as this will also open the parent directory if passed ".."
    private void openSubDirectory(String subDirectoryName) {
        String absolutePath;

        if (subDirectoryName.equals("..")) {
            absolutePath = currentDirectory.getParent();
        } else {
            absolutePath = currentDirectory.getAbsolutePath() + '/' + subDirectoryName;
        }

        openAbsoluteDirectory(absolutePath);
    }

    private void openAbsoluteDirectory(String absolutePath) {
        currentDirectory = new File(absolutePath);
        if (!currentDirectory.exists()) {
            Log.e("current directory does not exist: " + currentDirectory);
            return;
        }
        getChildDirectories(currentDirectory);
    }
}
