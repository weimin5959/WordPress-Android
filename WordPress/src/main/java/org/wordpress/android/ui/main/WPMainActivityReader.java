package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderPostListFragment;

/**
 * MySites activity
 */
public class WPMainActivityReader extends WPMainActivityBottomBar {

    @Override
    protected int getBottomBarPosition() {
        return 1;
    }

    @Override
    protected @StringRes int getScreenTitle() {
        return R.string.reader;
    }

    @Override
    protected Fragment newFragmentInstance() {
        return ReaderPostListFragment.newInstance();
    }

    @Override
    protected CoordinatorLayout getCoordinatorLayout() {
        return (CoordinatorLayout) findViewById(R.id.coordinator);
    }

    @Override
    protected int getContentResourceId() {
        return R.id.reader_recycler_view;
    }
}
