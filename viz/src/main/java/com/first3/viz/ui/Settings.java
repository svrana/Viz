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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.first3.viz.Preferences;
import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.ui.PinSelectorDialogFragment.ConfirmNewPinListener;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.VizUtils;

/**
 * Settings to configure various options.
 *
 * Preferences are defined in xml @ res/preferences.xml
 */
public class Settings extends PreferenceFragment implements ConfirmNewPinListener {
    private PreferenceManager mPreferenceManager;
    private ListView mListView;
    private ViewGroup mParentView;
    private int mParentViewId = R.layout.settings;
    private int mListViewId = android.R.id.list;
    private int mPreferenceScreenId = R.xml.preferences;
    private OnPreferenceAttachedListener mListener;

    /**
     * The starting request code given out to preference framework.
     */
    private static final int FIRST_REQUEST_CODE = 100;
    private static final int MSG_BIND_PREFERENCES = 0;

    private CheckBoxPreference mUnlockVideosCheckbox;
    private CheckBoxPreference mPinLock;
    private CheckBoxPreference mExternalPlayerPref;
    private PinSelectorDialogFragment pinSelectorDialogFragment;
    private DownloadDirectoryDialogPreference mSelectDownloadDirectory;
    private PreferenceGroup mOtherPreferences;
    private ListPreference mDownloadQualityListPref;
    private Handler mHandler = new Handler(VizApp.getLooper());

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

        mParentView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(mParentViewId, null);
        mListView = (ListView) mParentView.findViewById(mListViewId);
        mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        addPreferencesFromResource(R.xml.preferences);
        postBindPreferences();

        if (mListener != null) {
            mListener.onPreferenceAttached(getPreferenceScreen(), mPreferenceScreenId);
        }

        PreferenceScreen prefScreen = getPreferenceScreen();
        if (prefScreen != null) {
            mOtherPreferences = (PreferenceGroup) prefScreen.findPreference("other_preferences");
            mDownloadQualityListPref = (ListPreference) prefScreen.findPreference(Preferences.DOWNLOAD_QUALITY);
            mUnlockVideosCheckbox = (CheckBoxPreference) prefScreen.findPreference(Preferences.SHARE_VIDEOS);
            mSelectDownloadDirectory = (DownloadDirectoryDialogPreference) prefScreen
                    .findPreference(Preferences.DOWNLOAD_DIRECTORY);
            mPinLock = (CheckBoxPreference) prefScreen.findPreference(Preferences.PIN_LOCKED);
            pinSelectorDialogFragment = PinSelectorDialogFragment.newInstance(getString(R.string.enter_new_pin), true);
            pinSelectorDialogFragment.registerConfirmPinListener(this);
            pinSelectorDialogFragment.registerDialogDismissedListener(getActivity());
            mExternalPlayerPref = (CheckBoxPreference) prefScreen.findPreference(Preferences.USE_EXTERNAL_PLAYER);

            mDownloadQualityListPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String downloadQuality = (String) newValue;
                    Log.i("Changing download quality preference to: " + downloadQuality);
                    // not sure why we have to change this manually..
                    SharedPreferences.Editor editor = VizApp.getPrefs().edit();
                    editor.putString(Preferences.DOWNLOAD_QUALITY, downloadQuality);
                    editor.apply();
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
                        pinSelectorDialogFragment.show(Settings.this.getActivity().getFragmentManager(),
                                PinSelectorDialogFragment.PIN_SELECTOR_DIALOG_TAG);
                        // Preference will be saved by the dialog
                        return false;
                    } else {
                        VizUtils.showVizThumbnailInTray(getActivity());
                        // Just turn it off
                        VizApp.getPrefs().edit().putBoolean(Preferences.PIN_LOCKED, false).apply();
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("xml", mPreferenceScreenId);
        super.onSaveInstanceState(outState);
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
