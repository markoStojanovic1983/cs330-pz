package com.metropolitan.postchat.fragments;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

/**
 * Created by mare on 8/10/17.
 */
public class MyTopPostsFragment extends PostListFragment {

    public MyTopPostsFragment() {}

    @Override
    public Query getQuery(DatabaseReference databaseReference) {
        //Definisanje Querya i prikaz postova postavljenih od strane korisnika sortiranih prema broju lakova
        String myUserId = getUid();
        Query myTopPostsQuery = databaseReference.child("user-posts").child(myUserId)
                .orderByChild("likeCount");

        return myTopPostsQuery;
    }
}
