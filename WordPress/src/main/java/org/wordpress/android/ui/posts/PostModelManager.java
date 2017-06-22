package org.wordpress.android.ui.posts;

import android.content.Context;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.util.DateTimeUtils;

import javax.inject.Inject;

public class PostModelManager {
    private int mLocalPostId;
    private PostModel mPost;

    @Inject Dispatcher mDispatcher;
    @Inject PostStore mPostStore;

    PostModelManager(Context context, int localPostId) {
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mLocalPostId = localPostId;
        refreshPost();
    }

    PostModel getPost() {
        return mPost;
    }

    void refreshPost() {
        mPost = mPostStore.getPostByLocalPostId(mLocalPostId);
    }

    // Update methods
    void updateExcerpt(String excerpt) {
        mPost.setExcerpt(excerpt);
        dispatchUpdatePostAction();
    }

    private void dispatchUpdatePostAction() {
        mPost.setIsLocallyChanged(true);
        mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(mPost));
    }
}
