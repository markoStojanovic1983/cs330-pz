package com.metropolitan.postchat.activities;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.metropolitan.postchat.R;
import com.metropolitan.postchat.models.Comment;
import com.metropolitan.postchat.models.Post;
import com.metropolitan.postchat.models.User;

import java.util.ArrayList;
import java.util.List;
/**
 * Created by mare on 8/9/17.
 */
public class PostDetailActivity extends BaseActivity implements View.OnClickListener {

    public static final String EXTRA_POST_KEY = "post_key";

    // Definisanje Firebase referenci
    private DatabaseReference mPostReference;
    private DatabaseReference mCommentsReference;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;

    // Definisanje request broja za trazenje dozvola


    ////Definisanje ValueEventListenera Posta
    private ValueEventListener mPostListener;

    private String imageUrl;
    private String mPostKey;

    //Definisanje UI pogleda
    private ImageView mImageView;
    private TextView mAuthorView;
    private TextView mTitleView;
    private TextView mBodyView;
    private EditText mCommentField;
    private Button mCommentButton;
    private RecyclerView mCommentsRecycler;
    private CommentAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // Get post key from intent
        mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);
        if (mPostKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
        }

        // Inicijalizacija referenci Baze podataka
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://postchat-57d56.appspot.com/");
        mPostReference = FirebaseDatabase.getInstance().getReference()
                .child("posts").child(mPostKey);
        mCommentsReference = FirebaseDatabase.getInstance().getReference()
                .child("post-comments").child(mPostKey);

        // Inicijalizacije UI pogleda
        mAuthorView = (TextView) findViewById(R.id.post_author);
        mTitleView = (TextView) findViewById(R.id.post_title);
        mBodyView = (TextView) findViewById(R.id.post_body);
        mCommentField = (EditText) findViewById(R.id.field_comment_text);
        mCommentButton = (Button) findViewById(R.id.button_post_comment);
        mCommentsRecycler = (RecyclerView) findViewById(R.id.recycler_comments);
        mImageView = (ImageView) findViewById(R.id.image_view_post);
        mCommentButton.setOnClickListener(this);
        mCommentsRecycler.setLayoutManager(new LinearLayoutManager(this));

        //Inicijalizacija OnLongClickListenera u svrhe downloada prikazane slike
        mImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog alertDialog = new AlertDialog.Builder(view.getContext()).create();
                alertDialog.setTitle("Downloading picture");
                alertDialog.setMessage("Do you want do download picture?");
                alertDialog.setButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Uri imgUri = Uri.parse(imageUrl);
                            DownloadImage(imgUri, mPostKey);

                        } catch (SecurityException permissions){
                            Toast.makeText(PostDetailActivity.this, "Not allow to access Storage", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
                alertDialog.show();
                return false;
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        // Inicijalizacija ValueEventListenera za post
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Metod kojim se dobija vrednost Post objekta, radi osvezavanja UI-a
                Post post = dataSnapshot.getValue(Post.class);
                //Proverava se da li post sadrzi sliku
                if (post.imageUrl == null) {
                    mImageView.setVisibility(View.GONE);
                } else
                    imageUrl = post.imageUrl;
                mAuthorView.setText(post.author);
                mTitleView.setText(post.title);
                mBodyView.setText(post.body);
                Glide.with(PostDetailActivity.this).load(post.imageUrl).into(mImageView);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(PostDetailActivity.this, "Failed to load post",
                        Toast.LENGTH_SHORT).show();
            }
        };
        mPostReference.addValueEventListener(postListener);

        // Keep copy of post listener so we can remove it when app stops
        mPostListener = postListener;

        // Osluskivac komentara
        mAdapter = new CommentAdapter(this, mCommentsReference);
        mCommentsRecycler.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Brisanje ValueEventListenera posta
        if (mPostListener != null) {
            mPostReference.removeEventListener(mPostListener);
        }
        // Brisanje osluskivaca komentara
        mAdapter.cleanupListener();
    }

    /*Metod zaduzen za upis posta u bazu podataka
     */
    private void postComment() {
        final String uid = getUid();
        FirebaseDatabase.getInstance().getReference().child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Metod kojim se dobija vrednost Post objekta
                        User user = dataSnapshot.getValue(User.class);
                        String authorName = user.username;

                        // Kreiranje novog objekta tipa comment
                        String commentText = mCommentField.getText().toString();
                        Comment comment = new Comment(uid, authorName, commentText);

                        // Dodavanje komentara u bazu, kao i njegov prikaz u listi
                        mCommentsReference.push().setValue(comment);

                        // Ciscenje polja za unos teksta
                        mCommentField.setText(null);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        }
                });
    }
    /*Metod zaduzen za download slike
     */
    private void DownloadImage(Uri uri, String imgName) {

        //Kreiranje intent filtera i registrovanje Recivera
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);

        //Definisanje DownloadManagera koji kao request prima URI za download slike
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setTitle("Image Download");//Definisanje Naslova requesta
        request.setDescription("Downloading selected Picture");//Definisanje opisa requesta
        request.setDestinationInExternalPublicDir("/PostChat" + "/media", imgName + ".jpg");//Definisanje foldera za smestanje preuzetog fajla
        downloadManager.enqueue(request);//Obradjivanje zahteva i skidanje slike
    }

    //Kreiranje BroadcastReceivera u svrhe prikaza informacije o downloadu slike
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast toast = Toast.makeText(PostDetailActivity.this,
                    "Image Download Complete", Toast.LENGTH_LONG);
            toast.show();
        }
    };

    //Kreiranje klase zaduzene za prikaz komentara u okviru RecyclerViewa
    private static class CommentViewHolder extends RecyclerView.ViewHolder {

        //Definisanje UI pogleda
        public TextView authorView;
        public TextView bodyView;

        //Definisanje CommentViewHoldera
        public CommentViewHolder(View itemView) {
            super(itemView);
            // Inicijalizacije UI pogleda
            authorView = (TextView) itemView.findViewById(R.id.comment_author);
            bodyView = (TextView) itemView.findViewById(R.id.comment_body);
        }
    }

    /*Kreiranje klase adaptera koja sadrzi CommentViewHolder
     */
    private static class CommentAdapter extends RecyclerView.Adapter<CommentViewHolder> {
        //Definisanje kontektsa
        private Context mContext;
        //Definisanje Firebase reference
        private DatabaseReference mDatabaseReference;
        //Definisanje ChildEventListener
        private ChildEventListener mChildEventListener;
        //Definisanje listi za komentare
        private List<String> mCommentIds = new ArrayList<>();
        private List<Comment> mComments = new ArrayList<>();
        //Kreiranje Adaptera za prikaz comentara
        public CommentAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {

                    //Dodavanje novog komentara u listu
                    Comment comment = dataSnapshot.getValue(Comment.class);
                    // OSvezava RecyclerView
                    mCommentIds.add(dataSnapshot.getKey());
                    mComments.add(comment);
                    notifyItemInserted(mComments.size() - 1);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                    Toast.makeText(mContext, "Failed to load comments.",
                            Toast.LENGTH_SHORT).show();
                }
            };

            ref.addChildEventListener(childEventListener);
            mChildEventListener = childEventListener;
        }

        /*Metod koji kreira ViewHolder za komentare
         */
        @Override
        public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        /*Metod koji postavlja komentare unutar VievHoldera
         */
        @Override
        public void onBindViewHolder(CommentViewHolder holder, int position) {
            Comment comment = mComments.get(position);
            holder.authorView.setText(comment.author);
            holder.bodyView.setText(comment.text);
        }

        /* Metod za brojanje komentara
         */
        @Override
        public int getItemCount() {
            return mComments.size();
        }

        /* Metod koji resetuje mChildEventListener
         */
        public void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

    }

    /*Metod zaduzen za detekciju klika korisnika
         */
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.button_post_comment) {
            postComment();
        }
    }
}
