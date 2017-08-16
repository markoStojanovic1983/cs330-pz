package com.metropolitan.postchat.fragments;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

/**
 * Created by mare on 8/10/17.
 */

public class MyPostsFragment extends PostListFragment {

    public MyPostsFragment() {}

    @Override
    public Query getQuery(DatabaseReference databaseReference) {
        // Prikaz svih postova postavljenih od strane korisnika
        return databaseReference.child("user-posts")
                .child(getUid());
    }
}
