package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;

import org.wordpress.android.R;
import org.wordpress.android.ui.RequestCodes;

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

    @Override
    protected CoordinatorLayout getCoordinatorLayout() {
        return (CoordinatorLayout) findViewById(R.id.root_view_main);
    }

    @Override
    protected int getContentResourceId() {
        return R.id.scroll_view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.EDIT_POST:
            case RequestCodes.CREATE_BLOG:
                if (resultCode == RESULT_OK) {
                    MySiteFragment mySiteFragment = (MySiteFragment) getContentFragment();
                    if (mySiteFragment != null) {
                        mySiteFragment.onActivityResult(requestCode, resultCode, data);
                    }
                }
                break;
            case RequestCodes.SITE_PICKER:
                MySiteFragment mySiteFragment = (MySiteFragment) getContentFragment();
                if (mySiteFragment != null) {
                    mySiteFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
        }
    }
}
