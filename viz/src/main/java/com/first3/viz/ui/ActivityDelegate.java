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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import com.first3.viz.BuildConfig;
import com.first3.viz.Constants;
import com.first3.viz.Preferences;
import com.first3.viz.R;
import com.first3.viz.VersionChangeNotifier;
import com.first3.viz.VizApp;
import com.first3.viz.browser.Browser;
import com.first3.viz.models.Favorite;
import com.first3.viz.players.VideoPlayer;
import com.first3.viz.provider.VizDatabase;
import com.first3.viz.ui.Downloads;
import com.first3.viz.ui.PinSelectorDialogFragment;
import com.first3.viz.ui.PinSelectorDialogFragment.DismissPinDialogListener;
import com.first3.viz.ui.Settings;
import com.first3.viz.utils.ActivityParent;
import com.first3.viz.utils.ImageUtilities;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.TabsAdapter;
import com.first3.viz.utils.Utils;
import com.first3.viz.utils.VizUtils;

public class ActivityDelegate extends ActivityParent implements
        DismissPinDialogListener, VersionChangeNotifier.Listener {
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private VideoPlayer mVideoPlayer;
    private Favorites mFavorites;
    private Mode mCurrentMode = Mode.CONTROL;
    private ActionBar mActionBar;
    private ActionBar.Tab mBrowserTab;
    private ActionBar.Tab mFavoritesTab;
    private ActionBar.Tab mDownloadsTab;
    private ActionBar.Tab mFileManagerTab;
    private ActionBar.Tab mSettingsTab;
    private Browser mBrowser;
    private Downloads mDownloads;
    private FileManager mFileManager;
    private Settings mSettings;
    private VizDatabase mVizDatabase;
    private PinSelectorDialogFragment pinSelectorDialogFragment;
    private ImageView mIconView = null;
    private int mIntentTab = -1;


    /**
     * Used for "what" parameter to handler messages.
     *
     * Each component gets their own type.
     */
    public final static int MSG_BROWSER = ('B' << 16) + ('R' << 8) + 'O';
    public final static int MSG_DOWNLOADS = ('D' << 16) + ('O' << 8) + 'W';

    /**
     * Used as the arg1 parameter to handler messages. These start at 1 for each
     * component.
     */
    public final static int MSG_BROWSER_TASKDIALOG_SHOW = 1;
    public final static int MSG_BROWSER_TASKDIALOG_DISMISS = 2;
    public final static int MSG_BROWSER_SAVEDIALOG_SHOW = 3;
    public final static int MSG_DOWNLOADS_FAILURE_SHOW = 1;
    public final static int MSG_DOWNLOADS_FAILURE_DISMISS = 2;

    public static final String BUNDLE_LOAD_TAB = "loadTab";
    private static final String FIRST_APP_LAUNCH = "FirstAppLaunch";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            mIntentTab = b.getInt(BUNDLE_LOAD_TAB);
        }

        if (BuildConfig.DEBUG) {
            Log.d("Viz running in debug mode");
        }

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.main);
        Log.d();

        mActionBar = this.getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);

        mBrowserTab = mActionBar.newTab().setIcon(R.drawable.ic_action_tab_browser);
        mDownloadsTab = mActionBar.newTab().setIcon(R.drawable.ic_action_tab_downloads);
        mFavoritesTab = mActionBar.newTab().setIcon(R.drawable.ic_action_tab_favorites);
        mFileManagerTab = mActionBar.newTab().setIcon(R.drawable.ic_action_tab_filemanager);
        mSettingsTab = mActionBar.newTab().setIcon(R.drawable.ic_action_tab_settings);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mActionBar, mViewPager);
        mTabsAdapter.addTab(mFileManagerTab, FileManager.class, null);
        mTabsAdapter.addTab(mFavoritesTab, Favorites.class, null);
        mTabsAdapter.addTab(mBrowserTab, Browser.class, null);
        mTabsAdapter.addTab(mDownloadsTab, Downloads.class, null);
        mTabsAdapter.addTab(mSettingsTab, Settings.class, null);

        getVideoPlayerFragment().addEventListener(
                new VideoPlayer.EventListener() {
                    @Override
                    public void onVideoCompleted() {
                        switchToTabView();
                    }

                    @Override
                    public void onVideoPaused() {
                    }
                });

        updateUI();
        createDefaultDirectories();
        VersionChangeNotifier.getInstance().start(this);
    }

    private void showFirstLaunchDialog() {
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(getString(R.string.intro_title))
                .setPositiveButton(R.string.ok, null)
                .setView(VizApp.getInflator().inflate(R.layout.intro_dialog, null))
                .create()
                .show();
    }

    private void refreshUI() {
        Log.d();

        if (mIntentTab != -1) {
            mActionBar.setSelectedNavigationItem(mIntentTab);
            mCurrentMode = Mode.CONTROL;
            mIntentTab = -1;
            return;
        }

        int currentTab;
        int currentMode;
        boolean firstLaunch = VizApp.getPrefs().getBoolean(FIRST_APP_LAUNCH, true);
        if (firstLaunch) {
            // Show users the bookmarks tab first, maybe this will be less
            // confusing than showing them an empty videos screen
            currentTab = 1;
            currentMode = Mode.CONTROL.ordinal();
            VizApp.getPrefs().edit().putBoolean(FIRST_APP_LAUNCH, false).commit();
            showFirstLaunchDialog();
        } else {
            currentTab = VizApp.getPrefs().getInt(Preferences.CURRENT_TAB, 0);
            currentMode = VizApp.getPrefs().getInt(Preferences.CURRENT_MODE,
                    Mode.CONTROL.ordinal());
        }

        mActionBar.setSelectedNavigationItem(currentTab);

        if (currentMode == Mode.VIDEO.ordinal()) {
            mCurrentMode = Mode.VIDEO;
            Uri videoUri = Uri.parse(VizApp.getPrefs().getString(
                    Preferences.PLAYING_URI, ""));
            play(videoUri);
        } else {
            mCurrentMode = Mode.CONTROL;
        }
    }

    @Override
    public void onDestroy() {
        Log.d();

        if (mVizDatabase != null) {
            mVizDatabase.close();
            mVizDatabase = null;
        }
        if (mIconView != null) {
            if (mIconView.getBackground() != null) {
                mIconView.getBackground().setCallback(null);
            }
            mIconView.setImageDrawable(null);
            mIconView = null;
            mDownloadsTab.setCustomView(null);
        }
        // ImageUtilities.cleanupCache();
        super.onDestroy();
    }

    /**
     * Called when this activity becomes visible.
     */
    @Override
    protected void onStart() {
        Log.d();
        super.onStart();
    }

    /**
     * Called when this activity is no longer visible.
     */
    @Override
    protected void onStop() {
        Log.d();
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d();
        SharedPreferences.Editor ed = VizApp.getPrefs().edit();
        ed.putInt(Preferences.CURRENT_TAB, getActionBar()
                .getSelectedNavigationIndex());
        ed.putInt(Preferences.CURRENT_MODE, mCurrentMode.ordinal());
        if (mCurrentMode == Mode.VIDEO) {
            ed.putString(Preferences.PLAYING_URI, getVideoPlayerFragment()
                    .getUri().toString());
        }
        ed.commit();
        ImageUtilities.cleanupCache();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d();
        super.onResume();
        pinLock();
        updateUI();
    }

    public void onBackPressed() {
        if (mCurrentMode == Mode.CONTROL) {
            int current = getActionBar().getSelectedNavigationIndex();
            Log.d("Back button pressed in control mode.  Selected page: " + current);
            if (isCurrentTabBrowser()) {
                getBrowserFragment().goBack();
                return;
            }
        } else if (mCurrentMode == Mode.VIDEO) {
            Log.d("Back button in Video mode");
            // should pause, saved pause position, etc.
            switchToTabView();
            return;
        }
        super.onBackPressed();
    }

    private boolean isCurrentTab(Tab tab) {
        return (getActionBar().getSelectedNavigationIndex() == tab.getPosition());
    }

    public boolean isCurrentTabBrowser() {
        return isCurrentTab(mBrowserTab);
    }

    public void switchToTabView() {
        Log.d();
        getVideoPlayerFragment().stop();
        mViewPager.setVisibility(View.VISIBLE);
        mActionBar.show();
        switchToFullscreen(false);
        mCurrentMode = Mode.CONTROL;
    }

    public void switchToVideoView() {
        Log.d();
        mActionBar.hide();
        mViewPager.setVisibility(View.GONE);
        switchToFullscreen(true);
        mCurrentMode = Mode.VIDEO;
    }

    public void switchToFullscreen(boolean fullscreen) {
        if (fullscreen) {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
    }

    public Fragment getFragment(FragmentName name) {
        switch (name) {
        case BROWSER:
            String browserTag = mTabsAdapter.getFragmentName(mBrowserTab);
            return getFragmentManager().findFragmentByTag(browserTag);
        case DOWNLOADS:
            return mTabsAdapter.getFragForTab(mDownloadsTab);
        case FAVORITES:
            String favoritesTag = mTabsAdapter.getFragmentName(mFavoritesTab);
            return getFragmentManager().findFragmentByTag(favoritesTag);
        case FILEMANAGER:
            String fileManagerTag = mTabsAdapter
                    .getFragmentName(mFileManagerTab);
            return getFragmentManager()
                    .findFragmentByTag(fileManagerTag);
        case VIDEOPLAYER:
            return getFragmentManager().findFragmentById(
                    R.id.videoplayer);
        case SETTINGS:
            String settingsTag = mTabsAdapter.getFragmentName(mSettingsTab);
            return getFragmentManager().findFragmentByTag(settingsTag);
        default:
            return null;
        }
    }

    public Browser getBrowserFragment() {
        if (mBrowser == null) {
            mBrowser = (Browser) getFragment(FragmentName.BROWSER);
        }
        return mBrowser;
    }

    public Downloads getDownloadsFragment() {
        if (mDownloads == null) {
            mDownloads = (Downloads) getFragment(FragmentName.DOWNLOADS);
        }
        return mDownloads;
    }

    public FileManager getFileManagerFragment() {
        if (mFileManager == null) {
            mFileManager = (FileManager) getFragment(FragmentName.FILEMANAGER);
        }
        return mFileManager;
    }

    public VideoPlayer getVideoPlayerFragment() {
        if (mVideoPlayer == null) {
            mVideoPlayer = (VideoPlayer) getFragment(FragmentName.VIDEOPLAYER);
        }
        return mVideoPlayer;
    }

    public Favorites getFavoritesFragment() {
        if (null == mFavorites) {
            mFavorites = (Favorites) getFragment(FragmentName.FAVORITES);
        }
        return mFavorites;
    }

    public Settings getSettingsFragment() {
        if (mSettings == null) {
            mSettings = (Settings) getFragment(FragmentName.SETTINGS);
        }
        return mSettings;
    }

    public enum FragmentName {
        BROWSER, DOWNLOADS, FAVORITES, FILEMANAGER, SETTINGS, VIDEOPLAYER,
    }

    private enum Mode {
        CONTROL, // tabs are showing, user is controlling the app
        VIDEO // tabs are hidden, user is watching a video
    };

    public void switchToTab(int tab) {
        mActionBar.setSelectedNavigationItem(tab);
    }

    /**
     * Switch to browser tab and load in browser.
     */
    public void loadFavorite(String url, boolean fetchFavIcon) {
        // make the browser the current tab
        mTabsAdapter.onPageSelected(mBrowserTab.getPosition());
        // load url in browser
        getBrowserFragment().loadUrl(url, fetchFavIcon);
    }

    public void play(Uri videoUri) {
        boolean useExternalPlayer = VizApp.getPrefs().getBoolean(Preferences.USE_EXTERNAL_PLAYER, false);

        if (useExternalPlayer) {
            Intent intent = new Intent(Intent.ACTION_VIEW, videoUri);
            intent.setDataAndType(videoUri, "video/*");
            startActivity(intent);
        } else {
            switchToVideoView();
            getVideoPlayerFragment().start(videoUri);
        }
    }

    private void vizNotUsableExit() {
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(VizApp.getResString(R.string.cannot_use_app))
                .setMessage(VizApp.getResString(R.string.storage_error))
                .setNeutralButton(R.string.exit_viz,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int button) {
                                ActivityDelegate.this.finish();
                            }
                        }).create()
                          .show();
    }

    private void createDefaultDirectories() {
        if (!VizUtils.isExtStorageAvailable()) {
            vizNotUsableExit();
            return;
        }

        // directory should always exist as the call should create it
        // according to the documentation, but doing it manually just in case
        File dir = VizUtils.getVideosPrivateDir();
        if (!Utils.directoryCreate(dir)) {
            vizNotUsableExit();
            return;
        }

        dir = VizUtils.getVideosThumbnailDir();
        if (!Utils.directoryCreate(dir)) {
            vizNotUsableExit();
            return;
        }
    }

    private void updateUI() {
        if (mActionBar != null) {
            mActionBar.setTitle(R.string.app_name);
        }
    }

    @Override
    public void processMessage(Message msg) {
        switch (msg.what) {
        case MSG_BROWSER:
            switch (msg.arg1) {
            case MSG_BROWSER_TASKDIALOG_SHOW:
                getBrowserFragment().showProgressDialog(msg.obj);
                break;
            case MSG_BROWSER_TASKDIALOG_DISMISS:
                getBrowserFragment().removeProgressDialog();
                break;
            case MSG_BROWSER_SAVEDIALOG_SHOW:
                getBrowserFragment().saveDialog(this, msg.obj).show();
                break;
            default:
                Log.d("Unhandled MSG_BROWSER command: " + msg.arg1);
                break;
            }
            break;
        case MSG_DOWNLOADS:
            switch (msg.arg1) {
            case MSG_DOWNLOADS_FAILURE_SHOW:
                getDownloadsFragment().showDownloadFailedDialog(this, msg.obj);
                break;
            default:
                Log.d("Unhandled MSG_DOWNLOADS command: " + msg.arg1);
                break;
            }
            break;
        }
    }

    private void pinLock() {
        boolean isLocked = VizApp.getPrefs().getBoolean(Preferences.PIN_LOCKED, false);
        // Don't refresh the UI until the pin has been entered
        if (isLocked) {
            // Don't show a thumbnail in the "Recent Apps" menu if lockscreen is
            // enabled, i.e., don't freak
            // the little kiddies out with the viewing habits of their parents
            VizUtils.hideVizThumbnailInTray(this);
            // if the dialog is already showing (for instance, if the user
            // previously navigated away without unlocking),
            // dismiss the previous dialog
            if (pinSelectorDialogFragment != null
                    && pinSelectorDialogFragment.isAdded()) {
                return;
            } else {
                pinSelectorDialogFragment = PinSelectorDialogFragment
                        .newInstance(getString(R.string.enter_pin), false);
                pinSelectorDialogFragment.registerDialogDismissedListener(this);
                pinSelectorDialogFragment.show(getFragmentManager(),
                        PinSelectorDialogFragment.PIN_SELECTOR_DIALOG_TAG);
            }
        } else {
            refreshUI();
        }
    }

    @Override
    public void pinDialogDismissed() {
        refreshUI();
    }

    @Override
    public void onAppUpgrade() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Log.d("App upgraded (or installed) adding default favorites");
                Favorite.addSupportedSites();
            }
        };
        Utils.threadStart(t, "Failed to add default favorites");
    }
}
