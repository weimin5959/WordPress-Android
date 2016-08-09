package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.support.annotation.StringRes;

import org.wordpress.android.R;
import org.wordpress.android.ui.notifications.NotificationsListFragment;

/**
 * MySites activity
 */
public class WPMainActivityNotifications extends WPMainActivityBottomBar {

    @Override
    protected int getBottomBarPosition() {
        return 3;
    }

    @Override
    protected @StringRes int getScreenTitle() {
        return R.string.notifications;
    }

    @Override
    protected Fragment newFragmentInstance() {
        return NotificationsListFragment.newInstance();
    }
}
