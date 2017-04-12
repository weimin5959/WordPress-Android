package org.wordpress.android.ui.accounts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

public class PostLoginActivity extends AppCompatActivity implements PostLoginFragment.OnPostLoginInteraction {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.post_login_activity);

        if (savedInstanceState == null) {
            addPostLoginFragment();
        }

    }

    protected void addPostLoginFragment() {
        PostLoginFragment postLoginFragment = new PostLoginFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, postLoginFragment, PostLoginFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public void onContinue() {
        finish();
    }

    @Override
    public void onConnectanotherSite() {

    }
}
