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

import android.content.Context;

import android.util.AttributeSet;
import android.widget.VideoView;

public class VizMediaPlayer extends VideoView {
    private VizMediaPlayerEventListener mListener;

    public VizMediaPlayer(Context context) {
        super(context);
    }

    public VizMediaPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VizMediaPlayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void pause() {
        super.pause();
        if (mListener != null) {
            mListener.onVideoPause();
        }
    }

    @Override
    public void start() {
        super.start();
        if (mListener != null) {
            mListener.onVideoPlay();
        }
    }

    public void setVizMediaPlayerEventListener(VizMediaPlayerEventListener l) {
        mListener = l;
    }

    public interface VizMediaPlayerEventListener {
        public void onVideoPause();
        public void onVideoPlay();
    }
}
