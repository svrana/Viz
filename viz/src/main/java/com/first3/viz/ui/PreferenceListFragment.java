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

/* The MIT License (MIT)
 *
 *  Copyright (c) 2011-2014 <copyright holders>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.first3.viz.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.first3.viz.Preferences;
import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.ui.PinSelectorDialogFragment.ConfirmNewPinListener;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Utils;
import com.first3.viz.utils.VizUtils;

/**
 * Android does not have a PreferenceFragment in its support library. This is
 * a hack around that, using reflection to get access to parts of the
 * PreferenceManager that were not made public.
 * <p/>
 * There preferences are defined in xml @ res/preferences.xml
 */
public class PreferenceListFragment extends SherlockListFragment implements ConfirmNewPinListener {
    private PreferenceManager mPreferenceManager;

    private ListView mListView;
    private ViewGroup mParentView;
    private ViewGroup mSettingsHeader;
    private TextView mSupportText;
    private int mParentViewId = R.layout.settings;
    private int mListViewId = android.R.id.list;

    /**
     * The Resource Id of your preference screen.
     */
    private int mPreferenceScreenId = R.xml.preferences;

    private OnPreferenceAttachedListener mListener;

    /**
     * The starting request code given out to preference framework.
     */
    private static final int FIRST_REQUEST_CODE = 100;
    private static final int MSG_BIND_PREFERENCES = 0;

    private PreferenceScreen mPrefScreen;
    private CheckBoxPreference mUnlockVideosCheckbox;
    private CheckBoxPreference mPinLock;
    private CheckBoxPreference mExternalPlayerPref;
    private PinSelectorDialogFragment pinSelectorDialogFragment;
    private DownloadDirectoryDialogPreference mSelectDownloadDirectory;
    private PreferenceGroup mOtherPreferences;
    private ListPreference mDownloadQualityListPref;

    private Handler mHandler = new Handler(VizApp.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BIND_PREFERENCES:
                    bindPreferences();
                    break;
            }
        }
    };

    public PreferenceListFragment() {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ViewParent p = mParentView.getParent();
        if (p != null) {
            ((ViewGroup) p).removeView(mParentView);
        }
    }

    private void trigger_updatePurchaseUI() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updatePurchaseUI();
            }
        });
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            mPreferenceScreenId = bundle.getInt("xml");
        }
        mPreferenceManager = onCreatePreferenceManager();

        mParentView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(mParentViewId, null);
        mListView = (ListView) mParentView.findViewById(mListViewId);
        mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        addPreferencesFromResource(mPreferenceScreenId);
        postBindPreferences();

        if (mListener != null) {
            mListener.onPreferenceAttached(getPreferenceScreen(), mPreferenceScreenId);
        }

        mPrefScreen = getPreferenceScreen();
        if (mPrefScreen != null) {
            mOtherPreferences = (PreferenceGroup) mPrefScreen.findPreference("other_preferences");
            mDownloadQualityListPref = (ListPreference) mPrefScreen.findPreference(Preferences.DOWNLOAD_QUALITY);
            mUnlockVideosCheckbox = (CheckBoxPreference) mPrefScreen.findPreference(Preferences.SHARE_VIDEOS);
            mSelectDownloadDirectory = (DownloadDirectoryDialogPreference) mPrefScreen
                    .findPreference(Preferences.DOWNLOAD_DIRECTORY);
            mPinLock = (CheckBoxPreference) mPrefScreen.findPreference(Preferences.PIN_LOCKED);
            pinSelectorDialogFragment = PinSelectorDialogFragment.newInstance(getString(R.string.enter_new_pin), true);
            pinSelectorDialogFragment.registerConfirmPinListener(this);
            pinSelectorDialogFragment.registerDialogDismissedListener(getActivity());
            mExternalPlayerPref = (CheckBoxPreference) mPrefScreen.findPreference(Preferences.USE_EXTERNAL_PLAYER);

            mDownloadQualityListPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String downloadQuality = (String) newValue;
                    Log.i("Changing download quality preference to: " + downloadQuality);
                    // not sure why we have to change this manually..
                    SharedPreferences.Editor editor = VizApp.getPrefs().edit();
                    editor.putString(Preferences.DOWNLOAD_QUALITY, downloadQuality);
                    editor.commit();
                    updatePurchaseUI(); // update the preference summary
                    return true;
                }
            });

            mUnlockVideosCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean unlock = (Boolean) newValue;
                    Log.d("onPreferenceChange(preference=" + preference + ", key=" + preference.getKey() + ")");
                    if (unlock) {
                        new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_launcher)
                                .setTitle(R.string.video_accessibility).setMessage(R.string.va_description)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                                .create()
                                .show();

                        VizUtils.setDownloadDir(VizUtils.getVideosPublicDir());
                        Log.d("Setting download directory to:  " + VizUtils.getVideosPublicPath());
                    } else {
                        VizUtils.setDownloadDir(VizUtils.getVideosPrivateDir());
                        Log.d("Setting download directory to:  " + VizUtils.getVideosPrivatePath());
                    }

                    Log.i("UnlockVideoCheckboxPreferenceChangeEvent: selection set to unlock: " + unlock);

                    // Update the UI after we return so that mUnlockVideos.isChecked returns true.
                    // Could probably listen to an event that is triggered after the preference change..
                    trigger_updatePurchaseUI();
                    return true;
                }
            });

            mSelectDownloadDirectory.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d("changing to download dir: " + newValue);
                    updatePurchaseUI();
                    return true;
                }
            });

            mPinLock.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Boolean isLocked = (Boolean) newValue;

                    // If lock has been turned on, set a new pin
                    if (isLocked) {
                        pinSelectorDialogFragment.show(PreferenceListFragment.this.getSherlockActivity().getSupportFragmentManager(),
                                PinSelectorDialogFragment.PIN_SELECTOR_DIALOG_TAG);
                        // Preference will be saved by the dialog
                        return false;
                    } else {
                        VizUtils.showVizThumbnailInTray(getActivity());
                        // Just turn it off
                        VizApp.getPrefs().edit().putBoolean(Preferences.PIN_LOCKED, false).commit();
                        return true;
                    }
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
        Log.d();
        postBindPreferences();
        updatePurchaseUI();
        return mParentView;
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
    public void onStop() {
        super.onStop();
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityStop");
            m.setAccessible(true);
            m.invoke(mPreferenceManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListView = null;
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityDestroy");
            m.setAccessible(true);
            m.invoke(mPreferenceManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("xml", mPreferenceScreenId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityResult", int.class, int.class,
                    Intent.class);
            m.setAccessible(true);
            m.invoke(mPreferenceManager, requestCode, resultCode, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Posts a message to bind the preferences to the list view.
     * <p/>
     * Binding late is preferred as any custom preference types created in {@link #onCreate(Bundle)} are able to have
     * their views recycled.
     */
    private void postBindPreferences() {
        if (mHandler.hasMessages(MSG_BIND_PREFERENCES)) {
            return;
        }
        mHandler.obtainMessage(MSG_BIND_PREFERENCES).sendToTarget();
    }

    private void bindPreferences() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        if ((preferenceScreen != null) && (mListView != null)) {
            preferenceScreen.bind(mListView);
        }
    }

    /**
     * Creates the {@link PreferenceManager}.
     *
     * @return The {@link PreferenceManager} used by this activity.
     */
    private PreferenceManager onCreatePreferenceManager() {
        try {
            Constructor<PreferenceManager> c = PreferenceManager.class
                    .getDeclaredConstructor(Activity.class, int.class);
            c.setAccessible(true);
            PreferenceManager preferenceManager = c.newInstance(this.getActivity(), FIRST_REQUEST_CODE);
            return preferenceManager;
        } catch (Exception e) {
            Log.w("Could not create preference manager");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the {@link PreferenceManager} used by this activity.
     *
     * @return The {@link PreferenceManager}.
     */
    public PreferenceManager getPreferenceManager() {
        return mPreferenceManager;
    }

    /**
     * Sets the root of the preference hierarchy that this activity is showing.
     *
     * @param preferenceScreen The root {@link PreferenceScreen} of the preference hierarchy.
     */
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("setPreferences", PreferenceScreen.class);
            m.setAccessible(true);
            boolean result = (Boolean) m.invoke(mPreferenceManager, preferenceScreen);
            if (result && (preferenceScreen != null)) {
                postBindPreferences();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the root of the preference hierarchy that this activity is showing.
     *
     * @return The {@link PreferenceScreen} that is the root of the preference hierarchy.
     */
    public PreferenceScreen getPreferenceScreen() {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("getPreferenceScreen");
            m.setAccessible(true);
            return (PreferenceScreen) m.invoke(mPreferenceManager);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Adds preferences from activities that match the given {@link Intent}.
     *
     * @param intent The {@link Intent} to query activities.
     */
    public void addPreferencesFromIntent(Intent intent) {
        throw new RuntimeException("unimplemented, sorry");
    }

    /**
     * Inflates the given XML resource and adds the preference hierarchy to the current preference hierarchy.
     *
     * @param preferencesResId The XML resource ID to inflate.
     */
    public void addPreferencesFromResource(int preferencesResId) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("inflateFromResource", Context.class, int.class,
                    PreferenceScreen.class);
            m.setAccessible(true);
            PreferenceScreen prefScreen = (PreferenceScreen) m.invoke(mPreferenceManager, getActivity(),
                    preferencesResId, getPreferenceScreen());
            setPreferenceScreen(prefScreen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds a {@link Preference} based on its key.
     *
     * @param key The key of the preference to retrieve.
     * @return The {@link Preference} with the key, or null.
     * @see PreferenceGroup#findPreference(CharSequence)
     */
    public Preference findPreference(CharSequence key) {
        if (mPreferenceManager == null) {
            return null;
        }
        return mPreferenceManager.findPreference(key);
    }

    public void setOnPreferenceAttachedListener(OnPreferenceAttachedListener listener) {
        mListener = listener;
    }

    public void updatePurchaseUI() {
        if (mUnlockVideosCheckbox == null ||
                mSelectDownloadDirectory == null || mOtherPreferences == null ||
                mPinLock == null) {
            // ui hasn't been created yet, nothing to do
            // seems like we'd just need one null check here but they've been
            // here for so long that I'm too scared to remove them.
            return;
        }

        if (Preferences.isHighQualityDownloadDesired()) {
            mDownloadQualityListPref.setSummary(R.string.download_quality_high_preference);
            mDownloadQualityListPref.setDefaultValue(1);
        } else {
            mDownloadQualityListPref.setSummary(R.string.download_quality_low_preference);
            mDownloadQualityListPref.setDefaultValue(0);
        }

        mUnlockVideosCheckbox.setEnabled(true);
        mUnlockVideosCheckbox.setSummaryOff(getString(R.string.lock_videos_description));

        if (mUnlockVideosCheckbox.isChecked()) {
            // if the user has selected to show the videos in the gallery,
            // this is effectively forcing the download directory to a
            // specific location, so we disable the ability to change
            // download directory without first unchecking
            mSelectDownloadDirectory.setEnabled(false);
        } else {
            mSelectDownloadDirectory.setEnabled(true);
        }
        // always show paying users the directory.. lots of questions from
        // users about where the files are stored
        mSelectDownloadDirectory.setSummary(VizUtils.getDownloadPath());

        if (VizUtils.getDownloadPath().equals(VizUtils.getVideosPublicPath())) {
            if (!mUnlockVideosCheckbox.isChecked()) {
                // user selected the public download directory in the
                // directory picker. Update to show them that the file
                // will be shown in the Gallery.
                mUnlockVideosCheckbox.setChecked(true);
                updatePurchaseUI();
            }
        } else {
            // the user has changed the download directory
        }

        mPinLock.setEnabled(true);
        mPinLock.setSummaryOff(getString(R.string.lock_screen_summary_off));

        mExternalPlayerPref.setEnabled(true);
        mExternalPlayerPref.setSummaryOff(getString(R.string.lock_videos_description));
    }

    public interface OnPreferenceAttachedListener {
        public void onPreferenceAttached(PreferenceScreen root, int xmlId);
    }

    @Override
    public void confirmedNewPin(boolean confirmed) {
        mPinLock.setChecked(confirmed);
        if (confirmed) {
            VizUtils.hideVizThumbnailInTray(getActivity());
        }
    }
}
