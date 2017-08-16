package com.metropolitan.postchat.fragments;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
/**
 * Created by mare on 8/10/17.
 */
public class RecentPostsFragment extends PostListFragment {

    public RecentPostsFragment() {}

    @Override
    public Query getQuery(DatabaseReference databaseReference) {
        //Definisanje Querya i prikaz poslendjih 50 postova u bazi podataka
        Query recentPostsQuery = databaseReference.child("posts")
                .limitToFirst(200);

        return recentPostsQuery;
    }
}
