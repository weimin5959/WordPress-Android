package org.wordpress.android.ui.posts;

import android.content.Context;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

interface PostModelListener {
    void updatedExcerpt(String excerpt);
    void updatedFeaturedImage();
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

    private void refreshPost() {
        mPost = mPostStore.getPostByLocalPostId(mLocalPostId);
    }

    void addListener(PostModelListener listener) {
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

    void updateSlug(String slug) {
        mPost.setSlug(slug);
        dispatchUpdatePostAction();
    }

    void updatePassword(String password) {
        mPost.setPassword(password);
        dispatchUpdatePostAction();
    }

    void updateCategories(List<Long> categoryList) {
        if (categoryList == null) {
            return;
        }
        mPost.setCategoryIdList(categoryList);
        dispatchUpdatePostAction();
    }

    void updatePostStatus(String postStatus) {
        mPost.setStatus(postStatus);
        dispatchUpdatePostAction();
    }

    void updatePostFormat(String postFormat) {
        mPost.setPostFormat(postFormat);
        dispatchUpdatePostAction();
    }

    void updateTags(String selectedTags) {
        if (!TextUtils.isEmpty(selectedTags)) {
            String tags = selectedTags.replace("\n", " ");
            mPost.setTagNameList(Arrays.asList(TextUtils.split(tags, ",")));
        } else {
            mPost.setTagNameList(null);
        }
        dispatchUpdatePostAction();
    }

    void updatePublishDate(Calendar calendar) {
        mPost.setDateCreated(DateTimeUtils.iso8601FromDate(calendar.getTime()));
        dispatchUpdatePostAction();
    }

    void updateFeaturedImage(long featuredImageId) {
        mPost.setFeaturedImageId(featuredImageId);
        dispatchUpdatePostAction();
        for (PostModelListener listener : mListeners) {
            if (listener != null) {
                listener.updatedFeaturedImage();
            }
        }
    }

    private void dispatchUpdatePostAction() {
        mPost.setIsLocallyChanged(true);
        mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(mPost));
    }
}
