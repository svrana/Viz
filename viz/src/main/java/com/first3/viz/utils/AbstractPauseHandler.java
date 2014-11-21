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

import java.util.Vector;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

/**
 * Message Handler class that supports buffering up of messages when the
 * activity is paused i.e. in the background.
 */
public abstract class AbstractPauseHandler extends Handler {

    Activity mActivity = null;

    /**
     * Message Queue Buffer
     */
    final Vector<Message> messageQueueBuffer = new Vector<Message>();

    /**
     * Flag indicating the pause state
     */
    private boolean paused;

    public final Activity getActivity() {
        return mActivity;
    }

    public final void setActivity(Activity activity) {
        this.mActivity = activity;
    }

    /**
     * Resume the handler
     */
    public final void resume() {
        paused = false;

        while (messageQueueBuffer.size() > 0) {
            final Message msg = messageQueueBuffer.elementAt(0);
            messageQueueBuffer.removeElementAt(0);
            sendMessage(msg);
        }
    }

    /**
     * Pause the handler
     */
    public final void pause() {
        paused = true;
    }

    /**
     * Notification that the message is about to be stored as the activity is
     * paused. If not handled the message will be saved and replayed when the
     * activity resumes.
     *
     * @return true if the message is to be stored
     */
    protected abstract boolean storeMessage(Message message);

    /**
     * Notification message to be processed. This will either be directly from
     * handleMessage or played back from a saved message when the activity was
     * paused.
     *
     * @param message the message to be handled
     */
    protected abstract void processMessage(Message message);

    /** {@inheritDoc} */
    @Override
    public final void handleMessage(Message msg) {
        if (paused) {
            if (storeMessage(msg)) {
                Message msgCopy = new Message();
                msgCopy.copyFrom(msg);
                messageQueueBuffer.add(msgCopy);
            }
        } else {
            processMessage(msg);
        }
    }
}
