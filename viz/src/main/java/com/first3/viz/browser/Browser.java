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

package com.first3.viz.browser;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import com.first3.viz.Preferences;
import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.builders.ResourceBuilder;
import com.first3.viz.models.Favorite;
import com.first3.viz.models.Resource;
import com.first3.viz.provider.VizContract;
import com.first3.viz.ui.ActivityDelegate;
import com.first3.viz.ui.Downloads;
import com.first3.viz.ui.ProgressDialogFragment;
import com.first3.viz.ui.ProgressDialogFragment.DialogFragmentListener;
import com.first3.viz.utils.FetchContainerTask;
import com.first3.viz.utils.FragmentParent;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.TabsAdapter;
import com.first3.viz.utils.Utils;
import com.first3.viz.utils.VizUtils;

@SuppressLint("SetJavaScriptEnabled")
public class Browser extends FragmentParent implements TabsAdapter.TabListener {
    private WebView mVizWebView;
    private VizWebViewClient mVizWebViewClient;
    private VizWebChromeClient mWebChromeClient;
    private EditText urlBar;
    Browser mBrowser;
    ProgressBar mProgressBar;
    private static final String DIALOG_FRAGMENT_TAG = "progressDialog";
    static boolean initialized;
    static boolean mSelected = false;
    private boolean mConfirmationInProgress = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d();
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mBrowser = this;

        mVizWebViewClient = new VizWebViewClient(this);
        mWebChromeClient = new VizWebChromeClient(this);

        mVizWebView = new WebView(getActivity()); // get attributes?
        mVizWebView.setWebViewClient(mVizWebViewClient);
        mVizWebView.setWebChromeClient(mWebChromeClient);
        if (savedInstanceState != null) {
            Log.d("restoring web view state");
            mVizWebView.restoreState(savedInstanceState);
        }
        WebSettings s = mVizWebView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        s.setBuiltInZoomControls(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSaveFormData(true);

        mVizWebView.setId(61377); // PhoneWindow complained about no id (focus couldn't be saved)

        // Loading homepage here results in an exception on ICS, not sure why.
        // Update: post-poning until onCreatingView doesn't entirely fix the
        // issue either.
        //mVizWebView.loadUrl(defaultURL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d();
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.browser, null);
        urlBar = (EditText) v.findViewById(R.id.urlbar);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progressbar);

        // Add webview as 3rd child
        // TODO:   I think I did this so it wouldn't be destroyed or state
        // lost, don't really remember.
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.progressbar);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        v.addView(mVizWebView, 2, layoutParams);

        /*
        if (initialized == false) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String defaultUrl = prefs.getString("browser_homepage", "http://vimeo.com");
            Log.d("Got default url: " + defaultUrl);
            urlBar.setText(defaultUrl);
            loadUrlFromUrlBar();
            initialized = true;
        }
        */

        urlBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event!=null && event.getAction() == KeyEvent.ACTION_DOWN) ||
                        actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.d("onEditorAction(actionId=" + actionId + ", event=" + event + ")");
                    Activity activity = getActivity();
                    if (activity == null) {
                        return false;
                    }
                    mBrowser.loadUrlFromUrlBar();
                    InputMethodManager inputManager = (InputMethodManager)
                        VizApp.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                    return true;
                }
                return false;
            }
        });

        setCanGoBack();
        //mVizWebView.requestFocus();
        // android issue #7189 (webview text fields not causing virtual
        // keyboard to popup)
        mVizWebView.requestFocus(View.FOCUS_DOWN);
        mVizWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d();
        if (savedInstanceState != null) {
            mVizWebView.restoreState(savedInstanceState);
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        Log.d();
        super.onStart();
    }

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    private void refreshUI() {
        Log.d();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String url = prefs.getString(Preferences.LASTPAGE_LOADED, "http://vimeo.com");
        Log.d("Got url from preferences: " + url);
        String currentURL = mVizWebView.getUrl();
        if (TextUtils.isEmpty(currentURL) || !url.equals(currentURL)) {
            urlBar.setText(url);
            loadUrlFromUrlBar();
        }
    }

    @Override
    public void onResume() {
        Log.d();
        super.onResume();
        refreshUI();

    }

    @Override
    public void onPause() {
        Log.d();
        String url = null;
        if (mVizWebView != null) {
            url = mVizWebView.getUrl();
        }
        if (url != null) {
            Log.d("Storing url for later: " + url);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(Preferences.LASTPAGE_LOADED, url);
            ed.commit();
        }
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d();
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Log.d("Home pressed, go back in web history");
            goBack();
            return true;
        } else if (id == R.id.add_favorite) {
            Log.d("Add favorite");
            Favorite favorite = Favorite.newInstance(mVizWebView.getTitle(),
                    mVizWebView.getUrl(), mVizWebView.getFavicon());
            ContentValues map = favorite.toContentValues();
            getActivity().getContentResolver().insert(VizContract.Favorites.CONTENT_URI, map);
            getActivity().getContentResolver().notifyChange(VizContract.Favorites.CONTENT_URI, null);
            CharSequence text = favorite.getTitle() + " " + VizApp.getResString(R.string.favorites_added);
            Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d();
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.browser_menu, menu);
        //setCanGoBack();
    }

    @Override
    public void onDestroyView() {
	Log.d();
        ViewGroup v = (ViewGroup) getActivity().findViewById(R.id.browserRelativeLayout);
        v.removeView(mVizWebView);
	super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mVizWebView.saveState(outState);
        outState.putString("bla", "Value1");
        Log.d();
    }

    @Override
    public void onStop() {
        super.onStop();
        mVizWebView.stopLoading();
    }

    public boolean goBack() {
        if (mVizWebView.canGoBack()) {
            mVizWebViewClient.goingBack();
            Log.d();
            mVizWebView.goBack();
            return true;
        }
        return false;
    }

    private void setCanGoBack(ActionBar bar) {
        boolean bCanGoBack;

        if (!mSelected) {
            return;
        }

        if (mVizWebView == null) {
            bCanGoBack = false;
        } else {
            bCanGoBack = mVizWebView.canGoBack();
        }

        bar.setDisplayHomeAsUpEnabled(bCanGoBack);
    }

    private void setCanGoBack() {
        ActivityDelegate a = getActivityDelegate();
        if (a == null) {
            Log.d("Activity is null");
            return;
        }
        setCanGoBack(a.getSupportActionBar());
    }

    private String normalizeUrl(String url)	{
        Log.d("Normalizing url: ", url);
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        } else {
            return "http://www.google.com/search?q=" + url;
        }
    }

    /**
     * Tried to maintain a stack of urls first, but b/c shouldOverride is not
     * called each time, it proved to be a poor solution.  Instead, we just
     * query the webview for the loaded page after it's been loaded.  This can
     * make the urlbar slow to update, but at least it's accurate.
     */
    public void loadFinished() {
        String url = mVizWebView.getUrl();
        if (!TextUtils.isEmpty(url)) {
            if (urlBar != null) {
                urlBar.setText(url);
            }
        }
        setCanGoBack();
    }

    /**
     * This is called from the webviewclient so needs to be very careful
     * about what objects exists as it can be called at odd times.
     */
    public void loadUrl(String url, boolean storeFavIcon) {
        Log.d("(url=" + url + ")");

        if (urlBar == null || mVizWebView == null) {
            return;
        }

        if (storeFavIcon && mWebChromeClient != null) {
            mWebChromeClient.storeFavIcon(url);
        }

        urlBar.setText(url);
        mVizWebView.loadUrl(url);
        setCanGoBack();
        //urlBar.setText(url);
    }

    public void loadUrl(String url) {
        loadUrl(url, false);
    }

    private void loadUrlFromUrlBar() {
        String url = normalizeUrl(urlBar.getText().toString());
        Log.d("(url=" + url + ")");
        urlBar.setText(url);
        mVizWebView.loadUrl(url);
        setCanGoBack();
    }

    public void confirmDownload(ResourceBuilder builder) {
        Context context = getActivity();
        if (context == null) {
            Log.w("Can not confirm download without context");
            return;
        }

        // Block the save dialog from popping up over an existing popup. This
        // is a hack put in place for the DailyMotion builder that triggers
        // multiple downloads for some reason.
        if (mConfirmationInProgress) {
            Log.w("Ignoring download request from builder!!!");
            return;
        }

        mConfirmationInProgress = true;

        if (builder.isContainerURL()) {
            // if this is mysterious, it's no surprise -- it sucks.  The link
            // the user clicked on was not a direct link to the content, so we
            // need to parse the page to get the URL. Unfortunately, we don't
            // have access to the downloaded content, so we have to
            // re-download the page and parse it.  This is embarrasing and
            // should be fixed as it would make the user experience better,
            // but not sure how to do it and there are other, more interesting
            // goals.
            Log.d("Found container URL.");
            new ResourceParserTask().run(builder);
        } else {
            sendMessage(ActivityDelegate.MSG_BROWSER,
                    ActivityDelegate.MSG_BROWSER_SAVEDIALOG_SHOW, builder);
        }
    }

    public void showProgressDialog(Object obj) {
        final ResourceParserTask task = (ResourceParserTask) obj;
        FragmentManager manager = getActivityDelegate().getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();

        Fragment prev = manager.findFragmentByTag(DIALOG_FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        ft.commit();

        ProgressDialogFragment dialog = ProgressDialogFragment.newInstance();
        //dialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.OneProgressBar);
        dialog.setDialogFragmentListener(new DialogFragmentListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                task.cancel(true);
            }
        });
        dialog.show(manager, DIALOG_FRAGMENT_TAG);
    }

    public void removeProgressDialog() {
        Log.d();
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        Fragment prev = manager.findFragmentByTag(DIALOG_FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();
    }

    private class ResourceParserTask extends FetchContainerTask {
        ResourceBuilder mResourceBuilder;
        boolean result;

        public ResourceParserTask() {
        }

        @Override
        protected Void doInBackground(ResourceBuilder... builders) {
            Void v = null;
            mResourceBuilder = builders[0];
            Log.d("Fetching container from " + mResourceBuilder);
            result = mResourceBuilder.fetchContainer(this);
            return v;
        }

        @Override
        protected void onPreExecute() {
            // Block user from selecting more content to download, dim screen, etc.
            Log.d("Sending show message");
            Browser.this.sendMessage(ActivityDelegate.MSG_BROWSER,
                    ActivityDelegate.MSG_BROWSER_TASKDIALOG_SHOW, this);
        }

        @Override
        protected void onPostExecute(Void v) {
            Log.d("Sending dimiss message");

            if (!result) {
                // if an error occurs, need to reset this so subsequent
                // downloads can occur
                mConfirmationInProgress = false;
            }

            Browser.this.sendMessage(ActivityDelegate.MSG_BROWSER,
                    ActivityDelegate.MSG_BROWSER_TASKDIALOG_DISMISS, null);

            if (result) {
                Browser.this.sendMessage(ActivityDelegate.MSG_BROWSER,
                        ActivityDelegate.MSG_BROWSER_SAVEDIALOG_SHOW, mResourceBuilder);
            }
        }

        @Override
        protected void onCancelled(Void v) {
            mConfirmationInProgress = false;
            Toast.makeText(VizApp.getContext(), VizApp.getResString(R.string.download_cancelled), Toast.LENGTH_SHORT).show();
        }
    }

    public void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager)
            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void startDownload(Resource resource) {
        mConfirmationInProgress = false;

        ActivityDelegate ad = getActivityDelegate();
        if (ad == null) {
            return;
        }

        resource.setDownloadDirectory(VizUtils.getDownloadDir());

        // check download directory in case sd card or whatever has been
        // unmounted and we can no longer write to it.
        File directory = resource.getDownloadDirectory();
        if (!Utils.directoryCreate(directory)) {
            new AlertDialog.Builder((SherlockFragmentActivity)ad)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(VizApp.getResString(R.string.download_failed))
                .setMessage(VizApp.getResString(R.string.storage_error))
                .setNeutralButton(R.string.ok, null)
                .create()
                .show();
            return;
        }

        Uri uri = VizApp.getResolver().insert(VizContract.Downloads.CONTENT_URI,
                resource.toContentValues());
        if (uri == null) {
            Log.e("Could not add download to database error");
            Toast.makeText(VizApp.getContext(),
                VizApp.getResString(R.string.database_access_error), Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("(uri=" + uri + ")");

        resource.setDownloadUri(uri);

        Downloads downloads = ad.getDownloadsFragment();
        downloads.queue(resource);
    }

    /**
     * Return true if download should continue, otherwise false;
     */
    private boolean errorCheckPathAndFile(String filename) {
        if (TextUtils.isEmpty(filename)) {
            Toast.makeText(VizApp.getContext(), VizApp.getResString(R.string.invalid_filename), Toast.LENGTH_LONG).show();
            return false;
        }

        File downloadDir = VizUtils.getDownloadDir();
        if (downloadDir == null || !downloadDir.exists()) {
            Toast.makeText(VizApp.getContext(), VizApp.getResString(R.string.directory_access_error), Toast.LENGTH_LONG).show();
            return false;
        }
        if (!downloadDir.canWrite() || !downloadDir.canRead()) {
            Toast.makeText(VizApp.getContext(), VizApp.getResString(R.string.directory_access_error), Toast.LENGTH_LONG).show();
            return false;
        }
        File file = VizUtils.getVideoFile(filename);
        if (file != null && file.exists()) {
            Toast.makeText(VizApp.getContext(), VizApp.getResString(R.string.file_exists_error), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * Called through sendMessage with id MSG_BROWSER_SAVEDIALOG_SHOW
     */
    public AlertDialog saveDialog(final Context context, Object obj) {
        final ResourceBuilder resourceBuilder = (ResourceBuilder) obj;
        LayoutInflater factory = LayoutInflater.from(context);
        final View root = factory.inflate(R.layout.savefiledialog, null);

        TextView title = (TextView) root.findViewById(R.id.savefile_title);
        title.setText(resourceBuilder.getTitle(mVizWebView));

        final EditText editor = (EditText) root.findViewById(R.id.savefile_filename);

        final AlertDialog dialog = new AlertDialog.Builder(context)
            .setIcon(R.drawable.ic_launcher)
            .setTitle(VizApp.getResString(R.string.savedialog_ok))
            .setView(root)
            .setPositiveButton(R.string.savedialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String userFilename = editor.getText().toString();
                    if (!errorCheckPathAndFile(userFilename)) {
                        sendMessage(ActivityDelegate.MSG_BROWSER,
                            ActivityDelegate.MSG_BROWSER_SAVEDIALOG_SHOW, resourceBuilder);
                        return;
                    }
                    Log.d("User downloading to: " + userFilename);
                    resourceBuilder.setFilename(userFilename);
                    startDownload(resourceBuilder.build());
                }
            })
            .setNegativeButton(R.string.savedialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Log.d("User canceled download");
                    mConfirmationInProgress = false;
                }
            })
            .create();

        editor.setText(resourceBuilder.getDefaultFilename(mVizWebView));
        editor.setSingleLine();
        editor.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            // an enter on the keyboard triggers the download.
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String userFilename = null;
                if (editor.getText() != null) {
                    userFilename = editor.getText().toString();
                }
                if (!errorCheckPathAndFile(userFilename)) {
                    sendMessage(ActivityDelegate.MSG_BROWSER,
                        ActivityDelegate.MSG_BROWSER_SAVEDIALOG_SHOW, resourceBuilder);
                    return true;
                }
                resourceBuilder.setFilename(userFilename);
                Log.d("User downloading to: " + userFilename);
                startDownload(resourceBuilder.build());
                dialog.dismiss();
                return true;
            }
        });

        return dialog;
    }

    @Override
    public void onTabSelected(ActionBar actionBar) {
        mSelected = true;
        setCanGoBack(actionBar);
    }

    @Override
    public void onTabUnselected(ActionBar actionBar) {
        mSelected = false;
        actionBar.setDisplayHomeAsUpEnabled(false);
    }
}

