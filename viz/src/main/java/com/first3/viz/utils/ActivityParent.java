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

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;

import android.app.FragmentTransaction;
import android.app.Fragment;
import android.app.FragmentManager;

public abstract class ActivityParent extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final Fragment state = new State();
            final FragmentManager fm = getFragmentManager();
            final FragmentTransaction ft = fm.beginTransaction();
            ft.add(state, State.TAG);
            ft.commit();
        }
    }

    public abstract void processMessage(Message msg);

    public void sendMessage(int what, int command, Object obj) {
        final FragmentManager fm = getFragmentManager();
        State fragment = (State) fm.findFragmentByTag(State.TAG);
        if (fragment != null) {
            Log.d("sendMessage(what=" + what + ", command=" + command + ")");
            fragment.handler.sendMessage(fragment.handler.obtainMessage(what, command, 0, obj));
        }
    }

   /**
    * Message Handler class that supports buffering up of messages when the
    * activity is paused i.e. in the background and not able to display
    * dialogs without crashing.
    */
    public static class PauseHandler extends AbstractPauseHandler {
        @Override
        public boolean storeMessage(Message message) {
            return true;
        }

        @Override
        public final void processMessage(Message msg) {
            ActivityParent parent = (ActivityParent) this.getActivity();
            if (parent != null) {
                parent.processMessage(msg);
            }
        }
    }

    public static class State extends Fragment {
        public static final String TAG = "State";

        public State() {
        }

        /**
         * Handler for this activity.
         */
        public AbstractPauseHandler handler = new PauseHandler();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onResume() {
            super.onResume();
            handler.setActivity(getActivity());
            handler.resume();
        }

        @Override
        public void onPause() {
            super.onPause();
            handler.pause();
        }

        public void onDestroy() {
            super.onDestroy();
            handler.setActivity(null);
        }
    }
}
