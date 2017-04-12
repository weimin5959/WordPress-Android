package org.wordpress.android.ui.accounts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class PostLoginFragment extends android.support.v4.app.Fragment {
    public static final String TAG = "post_login_fragment_tag";

    private WPNetworkImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private View mSitesProgress;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;

    private SitesListAdapter mAdapter;

    public interface OnPostLoginInteraction {
        void onContinue();
        void onConnectanotherSite();
    }
    private OnPostLoginInteraction mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_post_login_screen, container, false);

        mAvatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.avatar);
        mDisplayNameTextView = (TextView) rootView.findViewById(R.id.display_name);
        mUsernameTextView = (TextView) rootView.findViewById(R.id.username);

        mSitesProgress = rootView.findViewById(R.id.sites_progress);

        rootView.findViewById(R.id.post_login_continue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onContinue();
                }
            }
        });

//        mEmailEditText = (EditText) rootView.findViewById(R.id.login_email);
//        mEmailEditText.addTextChangedListener(this);
//        mNextButton = (Button) rootView.findViewById(R.id.login_email_next_button);
//        mNextButton.setOnClickListener(mNextClickListener);
//        mUsernamePasswordButton = rootView.findViewById(R.id.login_email_username_password);
//
//        mEmailEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (!hasFocus) {
//                    autocorrectEmail();
//                }
//            }
//        });
//
//        autofillFromBuildConfig();
//
//        mEmailEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if ((didPressNextKey(actionId, event) || didPressEnterKey(actionId, event))) {
//                    next();
//                    return true;
//                } else {
//                    return false;
//                }
//            }
//        });
//
//        mUsernamePasswordButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mListener.onLoginViaUsernamePassword();
//            }
//        });

        RecyclerView sitesList = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        sitesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        sitesList.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        sitesList.setItemAnimator(null);
        sitesList.setAdapter(getAdapter());

        return rootView;
    }

    private SitesListAdapter getAdapter() {
        if (mAdapter == null) {
            setNewAdapter();
        }
        return mAdapter;
    }

    private void setNewAdapter() {
        mAdapter = new SitesListAdapter(
                getActivity(),
                new SitesListAdapter.OnDataLoadedListener() {
                    @Override
                    public void onBeforeLoad(boolean isEmpty) {
                        if (isEmpty) {
                            showProgress(true);
                        }
                    }
                    @Override
                    public void onAfterLoad() {
                        showProgress(false);
                    }
                });
    }

    public void showProgress(boolean show) {
        if (mSitesProgress != null) {
            mSitesProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPostLoginInteraction) {
            mListener = (OnPostLoginInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnPostLoginInteraction");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
//        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
//        mDispatcher.unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAccountDetails();
    }

    private void refreshAccountDetails() {
        // we only want to show user details for WordPress.com users
        if (mAccountStore.hasAccessToken()) {
            AccountModel defaultAccount = mAccountStore.getAccount();

            mDisplayNameTextView.setVisibility(View.VISIBLE);
            mUsernameTextView.setVisibility(View.VISIBLE);

            final String avatarUrl = constructGravatarUrl(mAccountStore.getAccount());
            loadAvatar(avatarUrl);

            mUsernameTextView.setText("@" + defaultAccount.getUserName());

            String displayName = StringUtils.unescapeHTML(defaultAccount.getDisplayName());
            if (!TextUtils.isEmpty(displayName)) {
                mDisplayNameTextView.setText(displayName);
            } else {
                mDisplayNameTextView.setText(defaultAccount.getUserName());
            }
        } else {
            mDisplayNameTextView.setVisibility(View.GONE);
            mUsernameTextView.setVisibility(View.GONE);
        }
    }

    private String constructGravatarUrl(AccountModel account) {
        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        return GravatarUtils.fixGravatarUrl(account.getAvatarUrl(), avatarSz);
    }

    private void loadAvatar(String avatarUrl) {
        mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR, null);
    }

    // OnChanged events

//    @SuppressWarnings("unused")
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onAvailabilityChecked(OnAvailabilityChecked event) {
//        if (event.isError()) {
//            AppLog.e(T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
//        }
//
//        switch(event.type) {
//            case EMAIL:
//                handleEmailAvailabilityEvent(event);
//                break;
//            default:
//                // TODO: we're not expecting any other availability check so, we should never have reached this line
//                break;
//        }
//    }
//
//    /**
//     * Handler for an email availability event. If a user enters an email address for their
//     * username an API checks to see if it belongs to a wpcom account.  If it exists the magic links
//     * flow is followed. Otherwise the self-hosted sign in form is shown.
//     * @param event
//     */
//    private void handleEmailAvailabilityEvent(OnAvailabilityChecked event) {
//        if (!event.isAvailable) {
//            // TODO: Email address exists in WordPress.com so, goto magic link offer screen
//            // Email address exists in WordPress.com
//            if (mListener != null) {
//                mListener.onMagicLinkEmailCheckSuccess(mEmail);
//            }
//        } else {
//            showEmailError(R.string.email_not_found);
//        }
//    }
}
