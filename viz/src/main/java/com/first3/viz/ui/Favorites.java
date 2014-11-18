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

import android.app.ListFragment;

import com.first3.viz.R;
import com.first3.viz.provider.VizContract;
import com.first3.viz.provider.VizContract.FavoritesColumns;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.first3.viz.utils.Log;
import android.database.Cursor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Bundle;

import android.provider.BaseColumns;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.widget.CursorAdapter;

import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class Favorites extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_ID = 1;
    private SimpleCursorAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d();

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        fillData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d();
        View v = super.onCreateView(inflater, container, savedInstanceState);
        //getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        setListAdapter(mAdapter);
        // now let's get a loader or reconnect to existing one
        getLoaderManager().initLoader(LOADER_ID, null, this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        Log.d();

        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                getActivity().startActionMode(new FavoritesActionMode(position, id));
                return true;
            }
        });
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        String url = cursor.getString(cursor.getColumnIndex(FavoritesColumns.URL));
        final byte[] blob = cursor.getBlob(FavoritesQuery.FAVICON);
        boolean fetchFavIcon = false;
        if (blob == null || blob.length == 0) {
            Log.d("Favorite does not have a favicon");
            fetchFavIcon = true;
        }
        ActivityDelegate delegate = (ActivityDelegate) getActivity();
        Log.d("User selected favorite url: " + url);
        delegate.loadFavorite(url, fetchFavIcon);
    }

    private void fillData() {
        String[] from = new String[] { FavoritesColumns.NAME, FavoritesColumns.FAVICON };
        //String[] from = new String[] { FavoritesColumns.NAME };
        int[] to = new int[] { R.id.favorite_name, R.id.favicon };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.favorites_row,
                null, from, to, CursorAdapter.NO_SELECTION);

        mAdapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == FavoritesQuery.FAVICON) {
                    ImageView faviconView = (ImageView) view;
                    // views are recycled, so need to reset the image in each
                    // one before returning or we'll get the wrong image for a
                    // favorite without a favicon
                    faviconView.setImageBitmap(null);

                    final byte[] blob = cursor.getBlob(FavoritesQuery.FAVICON);
                    if (blob == null || blob.length == 0) {
                        faviconView.setVisibility(View.GONE);
                        return true;
                    }
                    faviconView.setVisibility(View.VISIBLE);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
                    if (bitmap != null) {
                        faviconView.setImageBitmap(bitmap);
                        faviconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	Log.d();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("blargh", 3);
        Log.d();
        super.onSaveInstanceState(outState);
    }

    private class FavoritesActionMode implements ActionMode.Callback {
        final int fPosition;
        final long fId;

        public FavoritesActionMode(int position, long id) {
            fPosition = position;
            fId = id;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add("Remove favorite")
                .setIcon(R.drawable.ic_action_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            String favoriteId = String.valueOf(fId);

            Cursor cursor = (Cursor) mAdapter.getItem(fPosition);
            String favoriteTitle = cursor.getString(cursor.getColumnIndex(FavoritesColumns.NAME));

            getActivity().getContentResolver().delete(
                    VizContract.Favorites.buildFavoriteUri(favoriteId), null, null);
            getActivity().getContentResolver().notifyChange(VizContract.Favorites.CONTENT_URI, null);

            CharSequence text = favoriteTitle + " " + getString(R.string.favorites_favoriteremoved);
            Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();

            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), VizContract.Favorites.CONTENT_URI,
                FavoritesQuery.PROJECTION, null, null, VizContract.Favorites.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    private interface FavoritesQuery {
         String[] PROJECTION = { BaseColumns._ID, FavoritesColumns.NAME, FavoritesColumns.URL, FavoritesColumns.FAVICON };

         //int NAME = 1;
         //int URL = 1;
         int FAVICON = 3;
    }
}
