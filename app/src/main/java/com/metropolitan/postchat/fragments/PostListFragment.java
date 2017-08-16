package com.metropolitan.postchat.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.metropolitan.postchat.activities.EditPostActivity;
import com.metropolitan.postchat.models.Post;
import com.metropolitan.postchat.activities.PostDetailActivity;
import com.metropolitan.postchat.PostViewHolder;
import com.metropolitan.postchat.R;

/**
 * Created by mare on 8/10/17.
 */

public abstract class PostListFragment extends Fragment {
    // Definisanje Firebase referenci
    private DatabaseReference mDatabase;


    //Definisanje UI elemenata
    private FirebaseRecyclerAdapter<Post, PostViewHolder> mAdapter;
    private RecyclerView mRecycler;
    private LinearLayoutManager mManager;

    public PostListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_all_posts, container, false);

        // Inicijalizacija referenci Firebase referenci
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //Inicijalizacija UI elemenata
        mRecycler = (RecyclerView) rootView.findViewById(R.id.messages_list);
        mRecycler.setHasFixedSize(true);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Inicijalizacija Layout managera
        mManager = new LinearLayoutManager(getActivity());
        mManager.setReverseLayout(true);
        mManager.setStackFromEnd(true);
        mRecycler.setLayoutManager(mManager);

        // Inicijalizacija FirebaseRecyclerAdapter
        Query postsQuery = getQuery(mDatabase);
        mAdapter = new FirebaseRecyclerAdapter<Post, PostViewHolder>(Post.class, R.layout.item_post,
                PostViewHolder.class, postsQuery) {
            @Override
            protected void populateViewHolder(final PostViewHolder viewHolder, final Post model, final int position) {
                final DatabaseReference postRef = getRef(position);

                // OnClickListener za post
                final String postKey = postRef.getKey();
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Pokretanje PostDetailActivity
                        Intent intent = new Intent(getActivity(), PostDetailActivity.class);
                        intent.putExtra(PostDetailActivity.EXTRA_POST_KEY, postKey);
                        startActivity(intent);
                    }
                });

                // Provera da li je korisnik lajkovao neke postove
                if (model.likes.containsKey(getUid())) {
                    viewHolder.likeView.setImageResource(R.drawable.like_2);
                } else {
                    viewHolder.likeView.setImageResource(R.drawable.like_1);
                }

                // OnClickListener za lajk dugme
                viewHolder.bindToPost(model, new View.OnClickListener() {
                    @Override
                    public void onClick(View likeView) {

                        DatabaseReference globalPostRef = mDatabase.child("posts").child(postRef.getKey());
                        DatabaseReference userPostRef = mDatabase.child("user-posts").child(model.uid).child(postRef.getKey());
                        onLikeClicked(globalPostRef);
                        onLikeClicked(userPostRef);
                    }
                });

                viewHolder.deletePost(model, new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        //Inicijalizacija novog Alert dijaloga
                        Context context = view.getContext();
                        final CharSequence[] items = {"Edit post", "Delete post"};
                        new AlertDialog.Builder(context).setTitle("Manage post")
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {
                                        if (item == 0) {
                                            editPost(model.uid, postKey);

                                        } else if (item == 1) {
                                            deletePost(model.uid, postRef.getKey());
                                        }
                                        dialog.dismiss();
                                    }
                                }).show();
                        return false;
                    }
                });

            }
        };
        mRecycler.setAdapter(mAdapter);
    }

    /* Metod zaduzen za brisanje posta
     */
    private void deletePost(String uID, String postReferenceKey) {
        final String userId = getUid();
        if (uID.equals(userId)) {
            DatabaseReference globalPostRef = mDatabase.child("posts").child(postReferenceKey);
            DatabaseReference userPostRef = mDatabase.child("user-posts").child(uID).child(postReferenceKey);
            DatabaseReference commentPostRef = mDatabase.child("post-comments").child(postReferenceKey);
            globalPostRef.setValue(null);
            userPostRef.setValue(null);
            commentPostRef.setValue(null);
        } else {
            Toast.makeText(getContext(), "This is not your post, you can't delete it", Toast.LENGTH_SHORT).show();
        }

    }

    /* Metod zaduzen za editovanje posta
     */
    private void editPost(String uID, String postKey) {
        final String userId = getUid();
        if (uID.equals(userId)) {
            Intent intent = new Intent(getActivity(), EditPostActivity.class);
            intent.putExtra(EditPostActivity.EXTRA_POST_KEY, postKey);
            intent.putExtra(EditPostActivity.MODEL_UID, uID);
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "This is not your post, you can't edit it", Toast.LENGTH_SHORT).show();
        }

    }

    /* Metod zaduzen lajkovanje posta
     */
    private void onLikeClicked(DatabaseReference postRef) {
        postRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Post p = mutableData.getValue(Post.class);
                if (p == null) {
                    return Transaction.success(mutableData);
                }

                if (p.likes.containsKey(getUid())) {
                    p.likeCount
                            = p.likeCount - 1;
                    p.likes.remove(getUid());
                } else {
                    p.likeCount = p.likeCount + 1;
                    p.likes.put(getUid(), true);
                }

                mutableData.setValue(p);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                // Transaction completed

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.cleanup();
        }
    }

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public abstract Query getQuery(DatabaseReference databaseReference);
}
