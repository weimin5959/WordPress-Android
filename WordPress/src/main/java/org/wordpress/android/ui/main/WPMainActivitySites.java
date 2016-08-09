package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.support.annotation.StringRes;

import org.wordpress.android.R;

/**
 * MySites activity
 */
public class WPMainActivitySites extends WPMainActivityBottomBar {

    @Override
    protected int getBottomBarPosition() {
        return 0;
    }

    @Override
    protected @StringRes int getScreenTitle() {
        return R.string.tabbar_accessibility_label_my_site;
    }

    @Override
    protected Fragment newFragmentInstance() {
        return MySiteFragment.newInstance();
    }
}
