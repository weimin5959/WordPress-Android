package org.wordpress.android.ui.main;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarBadge;
import com.roughike.bottombar.OnMenuTabClickListener;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMMessageService;
import org.wordpress.android.GCMRegistrationIntentService;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.login.MagicLinkSignInActivity;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.posts.PromoDialog;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.AppSettingsFragment;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.CoreEvents.UserSignedOutCompletely;
import org.wordpress.android.util.CoreEvents.UserSignedOutWordPressCom;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import de.greenrobot.event.EventBus;

/**
 * Main activity which hosts sites, reader, me and notifications tabs
 */
public abstract class WPMainActivityBottomBar extends AppCompatActivity implements Bucket.Listener<Note> {
    private static final String KEY_FRAGMENT = "KEY_FRAGMENT";

    private TextView mConnectionBar;
    private int  mAppBarElevation;

    private BottomBar mBottomBar;
    private BottomBarBadge mUnreadMessagesBadge;

    public static final String ARG_OPENED_FROM_PUSH = "opened_from_push";

    /*
     * tab fragments implement this if their contents can be scrolled, called when user
     * requests to scroll to the top
     */
    public interface OnScrollToTopListener {
        void onScrollToTop();
    }

    /*
     * tab fragments implement this and return true if the fragment handles the back button
     * and doesn't want the activity to handle it as well
     */
    public interface OnActivityBackPressedListener {
        boolean onActivityBackPressed();
    }

    private boolean mIsBottomBarSetup = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        ProfilingUtils.split("WPMainActivity.onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mConnectionBar = (TextView) findViewById(R.id.connection_bar);
        mConnectionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // slide out the bar on click, then re-check connection after a brief delay
                AniUtils.animateBottomBar(mConnectionBar, false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            checkConnection();
                        }
                    }
                }, 2000);
            }
        });

        if (savedInstanceState == null) {
            if (AccountHelper.isSignedIn()) {
                // open note detail if activity called from a push, otherwise return to the tab
                // that was showing last time
                boolean openedFromPush = (getIntent() != null && getIntent().getBooleanExtra(ARG_OPENED_FROM_PUSH,
                        false));
                if (openedFromPush) {
                    getIntent().putExtra(ARG_OPENED_FROM_PUSH, false);
                    launchWithNoteId();
                } else {
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, newFragmentInstance(),
                            KEY_FRAGMENT).commit();

                    checkMagicLinkSignIn();
                }
            } else {
                ActivityLauncher.showSignInForResult(this);
            }
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout coordinatorLayout = getCoordinatorLayout();
                if (coordinatorLayout != null) {
                    mBottomBar = BottomBar.attachShy(coordinatorLayout, findViewById(getContentResourceId()),
                            savedInstanceState);
                } else {
                    mBottomBar = BottomBar.attach(WPMainActivityBottomBar.this, savedInstanceState);
                }
                // set the tab listener before the items are set to avoid triggering it on set
                mBottomBar.setOnMenuTabClickListener(new OnMenuTabClickListener() {
                    @Override
                    public void onMenuTabSelected(@IdRes int menuItemId) {
                        if (!mIsBottomBarSetup) {
                            return;
                        }

                        switch (menuItemId) {
                            case R.id.bottomBarItemSites:
                                ActivityLauncher.viewSites(WPMainActivityBottomBar.this);
                                break;
                            case R.id.bottomBarItemReader:
                                ActivityLauncher.viewReader(WPMainActivityBottomBar.this);
                                break;
                            case R.id.bottomBarItemMe:
                                ActivityLauncher.viewMe(WPMainActivityBottomBar.this);
                                break;
                            case R.id.bottomBarItemNotifications:
                                new UpdateLastSeenTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                ActivityLauncher.viewNotifications(WPMainActivityBottomBar.this);
                                break;
                        }
                    }

                    @Override
                    public void onMenuTabReSelected(@IdRes int menuItemId) {
                        //The user reselected an item so, maybe scroll your content to top.
                    }
                });
                mBottomBar.noTabletGoodness();
                mBottomBar.noTopOffset();
                mBottomBar.noNavBarGoodness();
                mBottomBar.noScalingGoodness();
                mBottomBar.noResizeGoodness();
//                mBottomBar.useFixedMode();
                mBottomBar.setItems(R.menu.bottom_bar_main);
                int white = ContextCompat.getColor(WPMainActivityBottomBar.this, R.color.blue_wordpress);
                mBottomBar.mapColorForTab(0, white);
                mBottomBar.mapColorForTab(1, white);
                mBottomBar.mapColorForTab(2, white);
                mBottomBar.mapColorForTab(3, white);
                mBottomBar.selectTabAtPosition(getBottomBarPosition(), false);
                mIsBottomBarSetup = true;
            }
        });
    }

    protected abstract int getBottomBarPosition();
    protected abstract @StringRes int getScreenTitle();
    protected abstract Fragment newFragmentInstance();
    protected abstract CoordinatorLayout getCoordinatorLayout();
    protected abstract @IdRes int getContentResourceId();

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Necessary to restore the BottomBar's state, otherwise we would
        // lose the current tab on orientation change.
        mBottomBar.onSaveInstanceState(outState);
    }

    private void showVisualEditorPromoDialogIfNeeded() {
        if (AppPrefs.isVisualEditorPromoRequired() && AppPrefs.isVisualEditorEnabled()) {
            DialogFragment newFragment = PromoDialog.newInstance(R.drawable.new_editor_promo_header,
                    R.string.new_editor_promo_title, R.string.new_editor_promo_desc,
                    R.string.new_editor_promo_button_label);
            newFragment.show(getFragmentManager(), "visual-editor-promo");
            AppPrefs.setVisualEditorPromoRequired(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AppLog.i(T.MAIN, "main activity > new intent");
        if (intent.hasExtra(NotificationsListFragment.NOTE_ID_EXTRA)) {
            launchWithNoteId();
        }
    }

    /*
     * called when app is launched from a push notification, switches to the notification tab
     * and opens the desired note detail
     */
    private void launchWithNoteId() {
        if (isFinishing() || getIntent() == null) return;

        // Check for push authorization request
        if (getIntent().hasExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN)) {
            Bundle extras = getIntent().getExtras();
            String token = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TOKEN, "");
            String title = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TITLE, "");
            String message = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_MESSAGE, "");
            long expires = extras.getLong(NotificationsUtils.ARG_PUSH_AUTH_EXPIRES, 0);

            long now = System.currentTimeMillis() / 1000;
            if (expires > 0 && now > expires) {
                // Show a toast if the user took too long to open the notification
                ToastUtils.showToast(this, R.string.push_auth_expired, ToastUtils.Duration.LONG);
                AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_EXPIRED);
            } else {
                NotificationsUtils.showPushAuthAlert(this, token, title, message);
            }
        }

        ActivityLauncher.viewNotifications(this);

        boolean shouldShowKeyboard = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false);
        if (GCMMessageService.getNotificationsCount() == 1) {
            String noteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            if (!TextUtils.isEmpty(noteId)) {
                GCMMessageService.bumpPushNotificationsTappedAnalytics(noteId);
                NotificationsListFragment.openNote(this, noteId, shouldShowKeyboard);
            }
        } else {
          // mark all tapped here
            GCMMessageService.bumpPushNotificationsTappedAllAnalytics();
        }

        GCMMessageService.clearNotifications();
    }

    @Override
    protected void onPause() {
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().removeListener(this);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start listening to Simperium Note bucket
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().addListener(this);
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                updateNotesBadge();
            }
        });

//        // We need to track the current item on the screen when this activity is resumed.
//        // Ex: Notifications -> notifications detail -> back to notifications
//        trackLastVisibleTab(mViewPager.getCurrentItem(), false);

        checkConnection();

        ProfilingUtils.split("WPMainActivity.onResume");
        ProfilingUtils.dump();
        ProfilingUtils.stop();
    }

    protected Fragment getContentFragment() {
        return getFragmentManager().findFragmentByTag(KEY_FRAGMENT);
    }

    @Override
    public void onBackPressed() {
        // let the fragment handle the back button if it implements our OnParentBackPressedListener
        Fragment fragment = getContentFragment();
        if (fragment instanceof OnActivityBackPressedListener) {
            boolean handled = ((OnActivityBackPressedListener) fragment).onActivityBackPressed();
            if (handled) {
                return;
            }
        }
        super.onBackPressed();
    }

    private void checkMagicLinkSignIn() {
        if (getIntent() !=  null) {
            if (getIntent().getBooleanExtra(MagicLinkSignInActivity.MAGIC_LOGIN, false)) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_SUCCEEDED);
                startWithNewAccount();
            }
        }
    }

//    private void trackLastVisibleTab(int position, boolean trackAnalytics) {
//        if (position ==  WPMainTabAdapter.TAB_MY_SITE) {
//            showVisualEditorPromoDialogIfNeeded();
//        }
//        switch (position) {
//            case WPMainTabAdapter.TAB_MY_SITE:
//                ActivityId.trackLastActivity(ActivityId.MY_SITE);
//                if (trackAnalytics) {
//                    AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.MY_SITE_ACCESSED);
//                }
//                break;
//            case WPMainTabAdapter.TAB_READER:
//                ActivityId.trackLastActivity(ActivityId.READER);
//                if (trackAnalytics) {
//                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_ACCESSED);
//                }
//                break;
//            case WPMainTabAdapter.TAB_ME:
//                ActivityId.trackLastActivity(ActivityId.ME);
//                if (trackAnalytics) {
//                    AnalyticsTracker.track(AnalyticsTracker.Stat.ME_ACCESSED);
//                }
//                break;
//            case WPMainTabAdapter.TAB_NOTIFS:
//                ActivityId.trackLastActivity(ActivityId.NOTIFICATIONS);
//                if (trackAnalytics) {
//                    AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED);
//                }
//                break;
//            default:
//                break;
//        }
//    }

    private void moderateCommentOnActivityResult(Intent data) {
        try {
            if (SimperiumUtils.getNotesBucket() != null) {
                Note note = SimperiumUtils.getNotesBucket().get(StringUtils.notNullStr(data.getStringExtra
                        (NotificationsListFragment.NOTE_MODERATE_ID_EXTRA)));
                CommentStatus status = CommentStatus.fromString(data.getStringExtra(
                        NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA));
                NotificationsUtils.moderateCommentForNote(note, status, findViewById(R.id.root_view_main));
            }
        } catch (BucketObjectMissingException e) {
            AppLog.e(T.NOTIFS, e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    // Register for Cloud messaging
                    startWithNewAccount();
                } else if (!AccountHelper.isSignedIn()) {
                    // can't do anything if user isn't signed in (either to wp.com or self-hosted)
                    finish();
                }
                break;
            case RequestCodes.REAUTHENTICATE:
                if (resultCode == RESULT_CANCELED) {
                    ActivityLauncher.showSignInForResult(this);
                } else {
                    // Register for Cloud messaging
                    startService(new Intent(this, GCMRegistrationIntentService.class));
                }
                break;
            case RequestCodes.NOTE_DETAIL:
                if (resultCode == RESULT_OK && data != null) {
                    moderateCommentOnActivityResult(data);
                }
                break;
            case RequestCodes.BLOG_SETTINGS:
                if (resultCode == SiteSettingsFragment.RESULT_BLOG_REMOVED) {
                    handleBlogRemoved();
                }
                break;
            case RequestCodes.APP_SETTINGS:
                if (resultCode == AppSettingsFragment.LANGUAGE_CHANGED) {
                    // nothing special to do
                }
                break;
        }
    }

    private void startWithNewAccount() {
        startService(new Intent(this, GCMRegistrationIntentService.class));
//        resetFragments();
    }

//    /*
//     * returns the my site fragment from the sites tab
//     */
//    private MySiteFragment getMySiteFragment() {
//        Fragment fragment = mTabAdapter.getFragment(WPMainTabAdapter.TAB_MY_SITE);
//        if (fragment instanceof MySiteFragment) {
//            return (MySiteFragment) fragment;
//        }
//        return null;
//    }

    // Updates `last_seen` notifications flag in Simperium and removes tab indicator
    protected class UpdateLastSeenTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            return SimperiumUtils.updateLastSeenTime();
        }

        @Override
        protected void onPostExecute(Boolean lastSeenTimeUpdated) {
            if (isFinishing()) return;

            if (lastSeenTimeUpdated) {
                hideNotesBadge();
            }
        }
    }

    // Events

    @SuppressWarnings("unused")
    public void onEventMainThread(UserSignedOutWordPressCom event) {
//        resetFragments();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UserSignedOutCompletely event) {
        ActivityLauncher.showSignInForResult(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidCredentialsDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.RestApiUnauthorized event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.TwoFactorAuthenticationDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidSslCertificateDetected event) {
        SelfSignedSSLCertsManager.askForSslTrust(this, null);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.LoginLimitDetected event) {
        ToastUtils.showToast(this, R.string.limit_reached, ToastUtils.Duration.LONG);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsChanged event) {
        updateNotesBadge();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ConnectionChangeReceiver.ConnectionChangeEvent event) {
        updateConnectionBar(event.isConnected());
    }

    private void checkConnection() {
        updateConnectionBar(NetworkUtils.isNetworkAvailable(this));
    }

    private void updateConnectionBar(boolean isConnected) {
        if (isConnected && mConnectionBar.getVisibility() == View.VISIBLE) {
            AniUtils.animateBottomBar(mConnectionBar, false);
        } else if (!isConnected && mConnectionBar.getVisibility() != View.VISIBLE) {
            AniUtils.animateBottomBar(mConnectionBar, true);
        }
    }

    private void handleBlogRemoved() {
        if (!AccountHelper.isSignedIn()) {
            ActivityLauncher.showSignInForResult(this);
        } else {
            Blog blog = WordPress.getCurrentBlog();
            MySiteFragment mySiteFragment = null;//getMySiteFragment();
            if (mySiteFragment != null) {
                mySiteFragment.setBlog(blog);
            }

            if (blog != null) {
                int blogId = blog.getLocalTableBlogId();
                ActivityLauncher.showSitePickerForResult(this, blogId);
            }
        }
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

                    updateNotesBadge();
                }
            });
        }
    }

    private boolean isViewingNotificationsTab() {
        return false;//mViewPager.getCurrentItem() == WPMainTabAdapter.TAB_NOTIFS;
    }

    protected void updateNotesBadge() {
        int unreadCount = SimperiumUtils.hasUnreadNotes();
        if (unreadCount > 0) {
            if (mUnreadMessagesBadge == null) {
                mUnreadMessagesBadge = mBottomBar.makeBadgeForTabAt(3, getResources().getColor(R.color.alert_red), 0);
            }
            mUnreadMessagesBadge.setText("" + unreadCount);
            mUnreadMessagesBadge.show();
        } else {
            if (mUnreadMessagesBadge != null) {
                mUnreadMessagesBadge.hide();
            }
        }
    }

    protected void hideNotesBadge() {
        if (mUnreadMessagesBadge != null) {
            mUnreadMessagesBadge.hide();
        } else {
            // nothing special to do
        }
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onSaveObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }
}
