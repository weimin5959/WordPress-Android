package org.wordpress.android.ui.accounts.login;

import org.wordpress.android.R;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LoginHostingFragment extends Fragment {

    public static final String TAG = "login_hosting_fragment_tag";

    public interface OnLoginHostingInteraction {
        void onWordPressComTapped();
        void onMyUrlTapped();
        void onImNotSureTapped();
        void onCreateSiteTapped();
    }
    private OnLoginHostingInteraction mListener;

    public static LoginHostingFragment newInstance() {
        LoginHostingFragment fragment = new LoginHostingFragment();
        return fragment;
    }

    public LoginHostingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_hosting_screen, container, false);

        view.findViewById(R.id.login_hosting_wpcom_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onWordPressComTapped();
                }
            }
        });
        view.findViewById(R.id.login_hosting_selfhosted_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onMyUrlTapped();
                }
            }
        });
        view.findViewById(R.id.login_hosting_dontknow_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onImNotSureTapped();
                }
            }
        });

        view.findViewById(R.id.create_site_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onCreateSiteTapped();
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLoginHostingInteraction) {
            mListener = (OnLoginHostingInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnLoginHostingInteraction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
