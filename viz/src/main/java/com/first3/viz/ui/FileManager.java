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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android. widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.utils.FragmentParent;
import com.first3.viz.utils.ImageUtilities;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Utils;
import com.first3.viz.utils.VizUtils;
import com.first3.viz.models.Resource;
import com.first3.viz.provider.VizContract;
import com.first3.viz.provider.VizContract.Resources;
import com.first3.viz.provider.VizContract.ResourcesColumns;

public class FileManager extends FragmentParent implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_ID = 2;
    private SimpleCursorAdapter mAdapter;
    private GridView mFileList;
    private TextView mEmptyList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d();
	super.onCreate(savedInstanceState);
        fillData();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	Log.d();
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.filemanager, null);
        mFileList = (GridView) v.findViewById(R.id.filesList);
        mFileList.setAdapter(mAdapter);

        // now let's get a loader or reconnect to existing one
        getLoaderManager().initLoader(LOADER_ID, null, this);

        mFileList.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                getActivityDelegate().play(getResourceUriFromPosition(position));
            }
        });

        mFileList.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showVideoOptionsPopup(position);
                return true;
            }
        });

        mEmptyList = (TextView) v.findViewById(android.R.id.empty);
        mEmptyList.setVisibility(View.INVISIBLE);

        setCanGoBack();
        setContentShowing();
        return v;
    }

    @Override
    public void onStart() {
        Log.d();
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d();
        super.onResume();
    }

    private void fillData() {
        String[] from = new String[] { ResourcesColumns.FILENAME, ResourcesColumns.TITLE, ResourcesColumns.DURATION };
        int[] to = new int[] { R.id.thumbnail, R.id.name, R.id.duration };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.filemanager_listitem,
                null, from, to, CursorAdapter.NO_SELECTION);
        mAdapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == ResourcesQuery.FILENAME) {
                    final ImageView thumbnail = (ImageView) view;
                    final int position = cursor.getPosition();

                    thumbnail.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Uri resourceUri = getResourceUriFromPosition(position);
                            getActivityDelegate().play(resourceUri);
                        }
                    });

                    thumbnail.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            showVideoOptionsPopup(position);
                            return true;
                        }
                    });

                    final String thumbUriString = cursor.getString(ResourcesQuery.THUMBNAIL);
                    if (TextUtils.isEmpty(thumbUriString)) {
                        return true;
                    }
                    Uri thumbnailUri = Uri.parse(thumbUriString);
                    thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    // hardcoding the width/height sucks, but querying the
                    // view at this point returns 0, 0.. must be a better way.
                    thumbnail.setImageDrawable(ImageUtilities.getCachedBitmap(getActivity(), thumbnailUri,
                                250, 175));
                    return true;
                }

                if (columnIndex == ResourcesQuery.TITLE) {
                    final TextView titleView = (TextView) view;
                    String title = cursor.getString(ResourcesQuery.TITLE);
                    if (Resource.createNullResource().getTitle().equals(title)) {
                        title = cursor.getString(ResourcesQuery.FILENAME);
                    }
                    titleView.setText(title);
                    return true;
                }

                if (columnIndex == ResourcesQuery.DURATION) {
                    final TextView durationView = (TextView) view;
                    int duration = cursor.getInt(ResourcesQuery.DURATION);
                    if (duration == 0) {
                        durationView.setVisibility(View.GONE);
                    } else {
                        durationView.setVisibility(View.VISIBLE);
                        durationView.setText(Utils.msecs_toReadableForm(duration));
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void setContentShowing() {
        boolean hasContent = (mAdapter != null && mAdapter.getCount() > 0);
        Log.d("setContentShowing(hasContent=" + hasContent + ")");
        if (mEmptyList != null) {
            if (hasContent) {
                mEmptyList.setVisibility(View.INVISIBLE);
            } else {
                mEmptyList.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroy() {
	Log.d();
	super.onDestroy();
    }

    @Override
    public void onDetach() {
	Log.d();
	super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // workaround for android crash
        outState.putString("bla", "Value1");
        Log.d();
        super.onSaveInstanceState(outState);
    }

    public void setCanGoBack() {
        Log.d();
        //getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    private Uri getResourceUriFromPosition(int position) {
        String resourceId = String.valueOf(mFileList.getItemIdAtPosition(position));
        return Resources.buildResourceUri(resourceId);
    }

    private void deleteItem(int position) {
        Uri uri = getResourceUriFromPosition(position);
        getActivity().getContentResolver().delete(uri, null, null);
        ImageUtilities.deleteCachedCover(uri);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), VizContract.Resources.CONTENT_URI,
                ResourcesQuery.PROJECTION, null, null, VizContract.Resources.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.changeCursor(data);
        setContentShowing();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.changeCursor(null);
    }

    private interface ResourcesQuery {
         String[] PROJECTION = { BaseColumns._ID, ResourcesColumns.FILENAME, ResourcesColumns.TITLE,
             ResourcesColumns.THUMBNAIL, ResourcesColumns.DURATION, ResourcesColumns.CONTENT,
             ResourcesColumns.DIRECTORY, ResourcesColumns.CONTAINER_URL, ResourcesColumns.URL };

         // int _ID = 0;
         int FILENAME = 1;
         int TITLE = 2;
         int THUMBNAIL = 3;
         int DURATION = 4;
    }

    private void delete(final int position) {
        int dialogTitle = R.string.filemanagerdialog_deletefile;
        String resourceTitle;

        Cursor cursor = (Cursor) mAdapter.getItem(position);
        resourceTitle = cursor.getString(cursor.getColumnIndex(ResourcesColumns.TITLE));

        if (Resource.createNullResource().getTitle().equals(resourceTitle)) {
            resourceTitle = cursor.getString(cursor.getColumnIndex(ResourcesColumns.FILENAME));
        }

        new AlertDialog.Builder(getActivity())
            .setIcon(R.drawable.ic_launcher)
            .setTitle(getString(dialogTitle))
            .setMessage(resourceTitle)
            .setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            deleteItem(position);
                        }
                    })
            .setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Log.d("Delete cancelled");
                        }
                    })
            .create()
            .show();
    }

    /**
     * Moves the the video to the directory where videos are automatically
     * picked up by the Gallery so users can view them outside of Viz.
     */
    private void unlock(int position) {
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        String resourceTitle = cursor.getString(cursor.getColumnIndex(ResourcesColumns.TITLE));
        final Uri resourceUri = getResourceUriFromPosition(position);
        final Context context = getActivity();

        if (context == null) {
            return;
        }

        if (Resource.createNullResource().getTitle().equals(resourceTitle)) {
            resourceTitle = cursor.getString(cursor.getColumnIndex(ResourcesColumns.FILENAME));
        }
        final String dir = cursor.getString(cursor.getColumnIndex(ResourcesColumns.DIRECTORY));

        if (!TextUtils.isEmpty(dir) && !dir.equals(VizUtils.getVideosPrivatePath())) {
            // already been added dialog
            new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(resourceTitle)
                .setMessage(VizApp.getResString(R.string.filemanager_video_unlock_error))
                .setPositiveButton(R.string.download_failed_accept,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                .create()
                .show();
        } else {
            new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.filemanager_unlock_dialog)
                .setMessage(resourceTitle)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String filename = cursor.getString(cursor.getColumnIndex(ResourcesColumns.FILENAME));
                                File videoFile = VizUtils.getVideoFile(dir, filename);
                                VizUtils.unlockVideo(resourceUri, videoFile);
                                Toast.makeText(context, R.string.filemanager_video_freed, Toast.LENGTH_LONG).show();
                            }
                        })
                .setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                .create()
                .show();
        }
    }

    private void showVideoOptionsPopup(final int position) {
        String[] choices = new String[] { VizApp.getResString(R.string.filemanager_delete_menuitem),
                                          VizApp.getResString(R.string.filemanager_unlock_menuitem) };

        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_launcher)
            .setTitle(VizApp.getResString(R.string.filemanager_video_options))
            .setItems(choices, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        delete(position);
                    } else if (which == 1) {
                        unlock(position);
                    }
                }}).create();
        dialog.show();
    }
}
