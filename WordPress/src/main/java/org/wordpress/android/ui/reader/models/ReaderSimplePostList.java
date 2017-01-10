package org.wordpress.android.ui.reader.models;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;

import java.util.ArrayList;

public class ReaderSimplePostList extends ArrayList<ReaderSimplePost> {

    public static ReaderSimplePostList fromJsonPosts(@NonNull JSONArray jsonPosts) {
        ReaderSimplePostList posts = new ReaderSimplePostList();
        for (int i = 0; i < jsonPosts.length(); i++) {
            JSONObject jsonRelatedPost = jsonPosts.optJSONObject(i);
            if (jsonRelatedPost != null) {
                ReaderSimplePost relatedPost = ReaderSimplePost.fromJson(jsonRelatedPost);
                if (relatedPost != null) {
                    posts.add(relatedPost);
                }
            }
        }
        return posts;
    }

    public ReaderPostList toReaderPosts(boolean isRecommended) {
        ReaderPostList posts = new ReaderPostList();
        for (ReaderSimplePost simplePost: this) {
            ReaderPost post = simplePost.toReaderPost();
            post.isRecommendedPost = isRecommended;
            posts.add(post);
        }
        return posts;
    }

}
