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

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;

import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * This is a helper class that implements the management of tabs and all
 * details of connecting a ViewPager with associated TabHost. It relies on a
 * trick. Normally a tab host has a simple API for supplying a View or
 * Intent that each tab will show. This is not sufficient for switching
 * between pages. So instead we make the content part of the tab host 0dp
 * high (it is not shown) and the TabsAdapter supplies its own dummy view to
 * show as the tab content. It listens to changes in tabs, and takes care of
 * switch to the correct paged in the ViewPager whenever the selected tab
 * changes.
 */
public class TabsAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener, ActionBar.TabListener {
    private final Context mContext;
    private final ActionBar mActionBar;
    private final ViewPager mViewPager;
    private final ArrayList<String> mTabs = new ArrayList<String>();
    private final ArrayList<Bundle> mArgs = new ArrayList<Bundle>();
    private final HashMap<ActionBar.Tab, Fragment> mFragments = Maps.newHashMap();

    public TabsAdapter(SherlockFragmentActivity activity, ActionBar actionBar, ViewPager pager) {
        super(activity.getSupportFragmentManager());
        mContext = activity;
        mActionBar = actionBar;
        mViewPager = pager;
        mViewPager.setAdapter(this);
        mViewPager.setOnPageChangeListener(this);
        // try to recommend that android create and not destroy the fragments
        // that aren't on screen.  This is a hack for the Downloads fragment
        // which we communicate with from a service and we need the Activity to
        // exist so we can make ui updates.
        mViewPager.setOffscreenPageLimit(3);
    }

    public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
        mTabs.add(clss.getName());
        mArgs.add(args);
        mActionBar.addTab(tab.setTabListener(this));

        Fragment frag = SherlockFragment.instantiate(mContext, clss.getName(), args);
        mFragments.put(tab, frag);

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public Fragment getItem(int position) {
        ActionBar.Tab tab = mActionBar.getTabAt(position);
        return mFragments.get(tab);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        mActionBar.setSelectedNavigationItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    // FIXME: fragile
    public String getFragmentName(Tab tab) {
        return "android:switcher:" + mViewPager.getId() + ":" + tab.getPosition();
    }

    public Fragment getFragForTab(Tab tab) {
        return mFragments.get(tab);
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());

        Fragment frag = mFragments.get(tab);
        if (frag != null && frag instanceof TabListener) {
            ((TabListener)frag).onTabSelected(mActionBar);
        }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        Fragment frag = mFragments.get(tab);
        if (frag != null && frag instanceof TabListener) {
            ((TabListener)frag).onTabUnselected(mActionBar);
        }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    public interface TabListener {
        public void onTabSelected(ActionBar actionBar);
        public void onTabUnselected(ActionBar actionBar);
    }
}

