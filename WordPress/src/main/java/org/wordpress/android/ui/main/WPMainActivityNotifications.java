package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.os.AsyncTask;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;

import com.simperium.client.Bucket;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
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

    @Override
    protected CoordinatorLayout getCoordinatorLayout() {
        return (CoordinatorLayout) findViewById(R.id.notifications_root_view);
    }

    @Override
    protected int getContentResourceId() {
        return R.id.recycler_view_notes;
    }

    /*
     * Simperium Note bucket listeners
     */
    @Override
    public void onNetworkChange(Bucket<Note> noteBucket, Bucket.ChangeType changeType, String s) {
        if (changeType == Bucket.ChangeType.INSERT || changeType == Bucket.ChangeType.MODIFY) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isFinishing()) return;

                    new UpdateLastSeenTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
        }
    }

}
