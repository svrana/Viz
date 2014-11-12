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

package com.first3.viz.players;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.text.TextUtils;
import android.widget.MediaController;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.first3.viz.Preferences;
import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.ui.VizMediaPlayer;
import com.first3.viz.utils.FragmentParent;
import com.first3.viz.utils.Log;
import com.first3.viz.provider.VizContract.Resources;
import com.first3.viz.provider.VizContract;
import com.first3.viz.utils.Utils;

public class VideoPlayer extends FragmentParent implements VizMediaPlayer.VizMediaPlayerEventListener {
    private VizMediaPlayer mVideoView;
    private EventListener mListener;
    private Uri mCurrentUri;
    private View videoLayoutView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        videoLayoutView = (View) inflater.inflate(R.layout.videoplayer, container, false);
        return videoLayoutView;
    }

    private VizMediaPlayer getVideoView() {
        if (mVideoView == null) {
            mVideoView = (VizMediaPlayer) getActivity().findViewById(R.id.videoview);
        }
        return mVideoView;
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

    private void resumeFromPosition(int startPosition) {
        int newpos = startPosition - 1000 > 0 ? startPosition - 1000 : startPosition;
        CharSequence text = getString(R.string.resuming_from_position) + " " +
            Utils.msecs_toReadableForm(newpos);
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
        getVideoView().seekTo(newpos);
        getVideoView().start();
    }

    private void resumeDialog(String title, final int startPosition) {
        new AlertDialog.Builder(getActivity())
            .setIcon(R.drawable.ic_launcher)
            .setTitle(VizApp.getResString(R.string.videoplayer_resumeplaying))
            .setMessage(title)
            .setPositiveButton(R.string.videoplayer_resumeplaying,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            resumeFromPosition(startPosition);
                        }
                    })
            .setNegativeButton(R.string.videoplayer_noresume,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getVideoView().start();
                    }
                })
        .create()
        .show();
    }

    public void start(Uri video) {
        Log.d("start playing " + video);
        mCurrentUri = video;

        VizMediaPlayer vv = getVideoView();
        vv.setVizMediaPlayerEventListener(this);
        vv.setMediaController(new MediaController(getActivity()));
        vv.setVideoURI(video);
        videoLayoutView.setVisibility(View.VISIBLE);
        vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d();
                saveCurrentPosition(getCurrentPosition());
                if (mListener != null) {
                    mListener.onVideoCompleted();
                }
            }
        });

        /*
         * This function could be called with a filetype URI (from the
         * downloads page) in which case we should get a null cursor.
         */
        int startPosition = 0;
        int duration = 0;
        String title = null;
        String filename = null;


        if (isResourceUri(mCurrentUri)) {
            Cursor cursor = VizApp.getResolver().query(mCurrentUri, new String[] { Resources._ID,
                    Resources.POSITION, Resources.DURATION, Resources.TITLE, Resources.FILENAME },
                    null, null, null);
            if (cursor.moveToFirst()) {
                startPosition  = cursor.getInt(cursor.getColumnIndex(Resources.POSITION));
                duration = cursor.getInt(cursor.getColumnIndex(Resources.DURATION));
                title = cursor.getString(cursor.getColumnIndex(Resources.TITLE));
                filename = cursor.getString(cursor.getColumnIndex(Resources.FILENAME));
            }
            cursor.close();
        }

        Log.d("start(position= " + Utils.msecs_toReadableForm(startPosition) + ", duration=" +
                Utils.msecs_toReadableForm(duration) + ")");

        // consider the video finished with 2 seconds left.  Who watches the
        // end?  Names, boring.
        if (duration > 0 && startPosition > 0 && startPosition < (duration - 2000)) {
            if (TextUtils.isEmpty(title)) {
                title = filename;
            }
            boolean autoResume = VizApp.getPrefs().getBoolean(Preferences.AUTO_RESUME, false);
            if (autoResume) {
                resumeFromPosition(startPosition);
            } else {
                resumeDialog(title, startPosition);
            }
        } else {
            vv.start();
        }
    }

    private boolean isResourceUri(Uri uri) {
        return mCurrentUri.getPath().contains(VizContract.PATH_RESOURCES);
    }

    public Uri getUri() {
        return mCurrentUri;
    }

    public int getCurrentPosition() {
        return getVideoView().getCurrentPosition();
    }

    public void stop() {
        Log.d("stopping playback");
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (getVideoView().isPlaying()) {
            saveCurrentPosition(getCurrentPosition());
            getVideoView().stopPlayback();
        }
        videoLayoutView.setVisibility(View.GONE);
    }

    @Override
    public void onVideoPlay() {
        Log.d();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("bla", "Value1");
        Log.d();
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d();

        saveCurrentPosition(getCurrentPosition());

        if (mListener != null) {
            mListener.onVideoPaused();
        }
    }

    @Override
    public void onVideoPause() {
        Log.d();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void addEventListener(EventListener l) {
        mListener = l;
    }

    private void saveCurrentPosition(final int position) {
        final Uri uri = getUri();
        if (uri == null || !isResourceUri(uri)) {
            return;
        }

        Thread t = new Thread() {
            @Override
            public void run() {
                ContentValues map = new ContentValues();
                map.put(Resources.POSITION, position);
                VizApp.getResolver().update(uri, map, null, null);
            }
        };
        Utils.threadStart(t, "Error updating video position");
    }

    public interface EventListener {
        public void onVideoCompleted();
        public void onVideoPaused();
    }
}
