package org.wordpress.android.ui.posts;

import android.content.Context;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

interface PostModelListener {
    void updatedExcerpt(String excerpt);
}

public class PostModelManager {
    private int mLocalPostId;
    private PostModel mPost;
    private List<PostModelListener> mListeners;

    @Inject Dispatcher mDispatcher;
    @Inject PostStore mPostStore;

    PostModelManager(Context context, int localPostId) {
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mLocalPostId = localPostId;
        refreshPost();
        mListeners = new ArrayList<>();
    }

    PostModel getPost() {
        return mPost;
    }

    void refreshPost() {
        mPost = mPostStore.getPostByLocalPostId(mLocalPostId);
    }

    public void addListener(PostModelListener listener) {
        mListeners.add(listener);
    }

    // Update methods
    void updateExcerpt(String excerpt) {
        mPost.setExcerpt(excerpt);
        dispatchUpdatePostAction();
        for (PostModelListener listener : mListeners) {
            if (listener != null) {
                listener.updatedExcerpt(mPost.getExcerpt());
            }
        }
    }

    private void dispatchUpdatePostAction() {
        mPost.setIsLocallyChanged(true);
        mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(mPost));
    }
}
