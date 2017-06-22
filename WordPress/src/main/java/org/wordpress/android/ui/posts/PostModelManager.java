package org.wordpress.android.ui.posts;

import android.content.Context;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.PostStore;

import javax.inject.Inject;

public class PostModelManager {
    private int mLocalPostId;
    private PostModel mPostModel;

    @Inject PostStore mPostStore;

    public PostModelManager(Context context, int localPostId) {
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mLocalPostId = localPostId;
    }

    public PostModel getPost() {
        if (mPostModel == null) {
            mPostModel = mPostStore.getPostByLocalPostId(mLocalPostId);
        }
        return mPostModel;
    }

    public void refreshPost() {
        mPostModel = mPostStore.getPostByLocalPostId(mLocalPostId);
    }
}
