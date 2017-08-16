package com.metropolitan.postchat.activities;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.metropolitan.postchat.MainActivity;
import com.metropolitan.postchat.R;
import com.metropolitan.postchat.models.Post;
import com.metropolitan.postchat.models.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mare on 8/14/17.
 */

public class EditPostActivity extends BaseActivity implements View.OnClickListener {
    //Definisanje staticnih atributa potrebnih za izvrsavanje aktivnosti
    private static final String REQUIRED = "Required";
    public static final String EXTRA_POST_KEY = "post_key";
    public static final String MODEL_UID = "model";
    // Definisanje request broja za izbor slika
    private static final int PICK_IMAGE_REQUEST = 234;

    //Definisanje ValueEventListenera Posta
    private ValueEventListener mPostListener;

    //Definisanje atributa vezanih za post
    private String imageUrl;
    private String mPostKey;
    private String modelUid;

    //Definisanje Uri objekta za putanju slike
    private Uri filePath;

    // Definisanje Firebase referenci
    private DatabaseReference mDatabase;
    private DatabaseReference mPostReference;
    private StorageReference mStorageRef;

    //Definisanje UI pogleda
    private EditText mEditTitleField;
    private EditText mEditBodyField;
    private FloatingActionButton mEditSubmitButton;
    private ImageView mEditImageView;
    private ImageButton mEditUploadPhoto;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);
        // Inicijalizacija referenci Baze podataka
        mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);
        modelUid = getIntent().getStringExtra(MODEL_UID);
        if (mPostKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
        }
        if (modelUid == null) {
            throw new IllegalArgumentException("Must pass Model");
        }
        // Inicijalizacija referenci Firebase referenci
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mPostReference = FirebaseDatabase.getInstance().getReference()
                .child("posts").child(mPostKey);
        mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://postchat-57d56.appspot.com/");

        // Inicijalizacije UI pogleda
        mEditTitleField = (EditText) findViewById(R.id.edit_field_title);
        mEditBodyField = (EditText) findViewById(R.id.edit_field_body);
        mEditSubmitButton = (FloatingActionButton) findViewById(R.id.edit_fab_submit_post);
        mEditUploadPhoto = (ImageButton) findViewById(R.id.edit_upload_photo);
        mEditImageView = (ImageView) findViewById(R.id.edit_imageView);


        // Inicijalizacija Click listenera
        mEditUploadPhoto.setOnClickListener(this);
        mEditSubmitButton.setOnClickListener(this);
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
                    mEditImageView.setVisibility(View.INVISIBLE);
                    mEditUploadPhoto.setVisibility(View.VISIBLE);
                } else
                    imageUrl = post.imageUrl;
                mEditTitleField.setText(post.title);
                mEditBodyField.setText(post.body);
                //Prikaz slike pomocu Glide-a
                Glide.with(getApplicationContext()).load(post.imageUrl).into(mEditImageView);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Prikaz log fajla pri gresci u prikazu posta
                Toast.makeText(EditPostActivity.this, "Failed to load post",
                        Toast.LENGTH_SHORT).show();
            }
        };

        mPostReference.addValueEventListener(postListener);
        mPostListener = postListener;
    }


    /*Metod koji smesta sve podatke vezane za post u bazu podataka
         */
    private void submitPost() {
        final String title = mEditTitleField.getText().toString();
        final String body = mEditBodyField.getText().toString();
        final String userId = getUid();

        // Proveravanje unosa naslova posta
        if (TextUtils.isEmpty(title)) {
            mEditTitleField.setError(REQUIRED);
            return;
        }

        // Proveravanje unosa sadrzaja posta
        if (TextUtils.isEmpty(body)) {
            mEditBodyField.setError(REQUIRED);
            return;
        }

        // Onemogucava duplo kreiranje posta
        setEditingEnabled(false);
        Toast.makeText(this, "Posting...", Toast.LENGTH_SHORT).show();

        //Unos novog posta
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //
                        User user = dataSnapshot.getValue(User.class);
                        //Provera da li je korisnik ulogovan
                        if (user == null) {
                            Toast.makeText(EditPostActivity.this,
                                    "Error: Could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            //Upload slike
                            uploadFile(userId, user.username, title, body);
                            setEditingEnabled(true);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        setEditingEnabled(true);

                    }
                });
    }

    /*Metod koji upisuje nove vrednosti posta
        */
    private void editPost(String userId, String username, String title, String body, String imgUrl) {

        //Kreira reference, smesta podatke o postu u mapu i osvezava njihove vrednosti u bazi podataka
        DatabaseReference globalPostRef = mDatabase.child("posts").child(mPostKey);
        DatabaseReference userPostRef = mDatabase.child("user-posts").child(modelUid).child(mPostKey);
        Post post = new Post(userId, username, title, body, imgUrl);
        Map<String, Object> postValues = post.toMap();
        globalPostRef.updateChildren(postValues);
        userPostRef.updateChildren(postValues);
    }

    /*Metod zaduzen za upload slike
         */
    private void uploadFile(final String userId, final String username, final String title, final String body) {

        // Provera da li je slika izabrana
        if (filePath != null) {
            // Prikaz dijaloga dok se slika uploaduje
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Posting");
            progressDialog.show();

            //Definisanje reference slike u bazi podataka
            StorageReference sRef = mStorageRef.child("images/" + System.currentTimeMillis() + "." + getFileExtension(filePath));

            //Dodavanje fajla u bazu
            sRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Post and picture uploaded ", Toast.LENGTH_LONG).show();
                            editPost(userId, username, title, body, taskSnapshot.getDownloadUrl().toString());
                            startActivity(new Intent(EditPostActivity.this, MainActivity.class));
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            // Prikaz procenta zavrsenog uploada slike
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });

        } else if (imageUrl != null) {
            //Smesta nove podatke o postu u Firebase sa linkom stare slike, jer ona nije promenjena
            editPost(userId, username, title, body, imageUrl);
            finish();
        } else {
            //Smesta nove podatke o postu u Firebase bez linka za sliku
            editPost(userId, username, title, body, null);
            finish();
        }
    }

    /*Metod zaduzen za kreiranje namere za izbor slike iz galerije
     */
    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);

    }

    /*Metod zaduzen za vracanje ekstenzije izabrane slike
     */
    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    /*Metod koji sklanja submit dugme
         */
    private void setEditingEnabled(boolean enabled) {
        mEditTitleField.setEnabled(enabled);
        mEditBodyField.setEnabled(enabled);
        if (enabled) {
            mEditSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mEditSubmitButton.setVisibility(View.GONE);
        }
    }

    /*Metod zaduzen za otvaranje prozora za izbor slike, i prikazuje odabranu sliku
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                mEditImageView.setVisibility(View.VISIBLE);
                mEditImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*Metod zaduzen za detekciju klika korisnika
         */
    @Override
    public void onClick(View view) {
        if (view == mEditUploadPhoto) {
            showFileChooser();
        } else if (view == mEditSubmitButton) {
            submitPost();
        }
    }
}
