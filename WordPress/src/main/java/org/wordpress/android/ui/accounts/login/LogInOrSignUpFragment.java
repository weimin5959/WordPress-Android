package org.wordpress.android.ui.accounts.login;

import com.airbnb.lottie.LottieAnimationView;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.JetpackCallbacks;
import org.wordpress.android.widgets.WPViewPager;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class LogInOrSignUpFragment extends Fragment {

    public static final String TAG = "login_or_signup_fragment_tag";
    private LogInOrSignUpFragment.OnLogInOrSignUpFragmentInteraction mListener;

    public interface OnLogInOrSignUpFragmentInteraction {
        void onLoginTapped();
        void onCreateSiteTapped();
    }

    private JetpackCallbacks mJetpackCallbacks;

    public static LogInOrSignUpFragment newInstance() {
        LogInOrSignUpFragment fragment = new LogInOrSignUpFragment();
        return fragment;
    }

    public LogInOrSignUpFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_signup_screen, container, false);

        Button loginButton = (Button) view.findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginTapped();
            }
        });
        Button createSiteButton = (Button) view.findViewById(R.id.create_site_button);
        createSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateSiteTapped();
            }
        });

        WPViewPager pager = (WPViewPager) view.findViewById(R.id.intros_pager);
        MyPagerAdapter adapter = new MyPagerAdapter(getChildFragmentManager());
        pager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout_indicator);
        tabLayout.setupWithViewPager(pager, true);

        return view;
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    return LottieFragment.newInstance("data.json", getString(R.string.login_promo_text_onthego));
                case 1:
                    return LottieFragment.newInstance("data.json", getString(R.string.login_promo_text_realtime));
                case 2:
                    return LottieFragment.newInstance("data.json", getString(R.string.login_promo_text_anytime));
                case 3:
                    return LottieFragment.newInstance("data.json", getString(R.string.login_promo_text_notifications));
                case 4:
                    return LottieFragment.newInstance("data.json", getString(R.string.login_promo_text_jetpack));
                default:
                    return null;
            }
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return 5;
        }
    }

    public static class LottieFragment extends Fragment {
        private final static String KEY_ANIMATION_FILENAME = "KEY_ANIMATION_FILENAME";
        private final static String KEY_PROMO_TEXT = "KEY_PROMO_TEXT";

        private String mAnimationFilename;
        private String mPromoText;

        static LottieFragment newInstance(String animationFilename, String promoText) {
            LottieFragment fragment = new LottieFragment();
            Bundle bundle = new Bundle();
            bundle.putString(KEY_ANIMATION_FILENAME, animationFilename);
            bundle.putString(KEY_PROMO_TEXT, promoText);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mAnimationFilename = getArguments().getString(KEY_ANIMATION_FILENAME);
            mPromoText = getArguments().getString(KEY_PROMO_TEXT);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
                savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_intro_template_view, container, false);

            TextView promoText = (TextView) rootView.findViewById(R.id.promo_text);
            promoText.setText(mPromoText);

            LottieAnimationView lottieAnimationView = (LottieAnimationView) rootView.findViewById(R.id.animation_view);
            lottieAnimationView.setAnimation(mAnimationFilename, LottieAnimationView.CacheStrategy.Weak);
            lottieAnimationView.setImageAssetsFolder("images");

            return rootView;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LogInOrSignUpFragment.OnLogInOrSignUpFragmentInteraction) {
            mListener = (LogInOrSignUpFragment.OnLogInOrSignUpFragmentInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
        if (context instanceof JetpackCallbacks) {
            mJetpackCallbacks = (JetpackCallbacks) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement JetpackCallbacks");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    private void onLoginTapped() {
        if (mListener != null) {
            mListener.onLoginTapped();
        }
    }

    private void onCreateSiteTapped() {
        if (mListener != null) {
            mListener.onCreateSiteTapped();
        }
    }
}
