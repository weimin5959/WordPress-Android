package org.wordpress.android.ui;

import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.wordpress.android.R;
import org.wordpress.android.ui.media.services.MediaUploadService;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.widgets.WPTextView;

import de.greenrobot.event.EventBus;

public class UploadStatusBarActivity extends AppCompatActivity {
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        RelativeLayout fullView = (RelativeLayout) getLayoutInflater().inflate(R.layout.test_master_bar, null);
        LinearLayout activityContainer = (LinearLayout) fullView.findViewById(R.id.activity_content);
        getLayoutInflater().inflate(layoutResID, activityContainer, true);
        super.setContentView(fullView);
        if (!MediaUploadService.isUploadingMedia()) {
            WPTextView bar = (WPTextView) findViewById(R.id.upload_bar);
            bar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        updateStatusBarVisibility();
        super.onResume();
    }

    @Override
    protected void onStart() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.MediaUploadProgress event) {
        WPTextView bar = (WPTextView) findViewById(R.id.upload_bar);
        bar.setText("Uploading media! - " + event.progress);
    }

    private void updateStatusBarVisibility() {
        WPTextView bar = (WPTextView) findViewById(R.id.upload_bar);
        if (!MediaUploadService.isUploadingMedia()) {
            bar.setVisibility(View.GONE);
        } else {
            bar.setVisibility(View.VISIBLE);
        }
    }
}
