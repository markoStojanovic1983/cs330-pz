package com.metropolitan.postchat.activities;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.metropolitan.postchat.authenticator.SignUpActivity;
import com.metropolitan.postchat.models.Post;
import com.metropolitan.postchat.models.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mare on 8/8/17.
 */
public class NewPostActivity extends BaseActivity implements View.OnClickListener {

    private static final String REQUIRED = "Required";

    // Definisanje request broja za izbor slika
    private static final int PICK_IMAGE_REQUEST = 234;

    //Definisanje Uri objekta za putanju slike
    private Uri filePath;

    // Definisanje Firebase referenci
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    //Definisanje UI pogleda
    private EditText mTitleField;
    private EditText mBodyField;
    private FloatingActionButton mSubmitButton;
    private ImageButton mUploadPhoto;
    private ImageView mImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        // Inicijalizacija referenci Firebase referenci
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://postchat-57d56.appspot.com/");

        // Inicijalizacije UI pogleda
        mTitleField = (EditText) findViewById(R.id.field_title);
        mBodyField = (EditText) findViewById(R.id.field_body);
        mSubmitButton = (FloatingActionButton) findViewById(R.id.fab_submit_post);
        mUploadPhoto = (ImageButton) findViewById(R.id.upload_photo);
        mImageView = (ImageView) findViewById(R.id.imageView);

        // Ukoliko post ne sadrzi sliku, UI pogled se sklanja
        if (filePath == null) {
            mImageView.setVisibility(View.INVISIBLE);
        }

        // Inicijalizacija Click listenera
        mUploadPhoto.setOnClickListener(this);
        mSubmitButton.setOnClickListener(this);
    }

    /*Metod koji smesta sve podatke vezane za post u bazu podataka
     */
    private void submitPost() {
        final String title = mTitleField.getText().toString();
        final String body = mBodyField.getText().toString();
        final String userId = getUid();

        // Proveravanje unosa naslova posta
        if (TextUtils.isEmpty(title)) {
            mTitleField.setError(REQUIRED);
            return;
        }

        // Proveravanje unosa sadrzaja posta
        if (TextUtils.isEmpty(body)) {
            mBodyField.setError(REQUIRED);
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
                        User user = dataSnapshot.getValue(User.class);
                        //Provera da li je korisnik ulogovan
                        if (user == null) {
                            Toast.makeText(NewPostActivity.this,
                                    "Error: Could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            //Upload slike
                            uploadFile(userId, user.username, title, body);
                            setEditingEnabled(true);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                        setEditingEnabled(true);

                    }
                });
    }


    /*Metod koji upisuje vrednosti posta
     */
    private void writeNewPost(String userId, String username, String title, String body, String imgUrl) {

        //Kreira nov kljuc za Firebaes, smesta podatke o postu u mapu
        String key = mDatabase.child("posts").push().getKey();
        Post post = new Post(userId, username, title, body, imgUrl);
        Map<String, Object> postValues = post.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/" + key, postValues);
        childUpdates.put("/user-posts/" + userId + "/" + key, postValues);

        //Upisuje podatke u bazu podataka
        mDatabase.updateChildren(childUpdates);
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
                            writeNewPost(userId, username, title, body, taskSnapshot.getDownloadUrl().toString());
                            startActivity(new Intent(NewPostActivity.this, MainActivity.class));
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
        } else {
            //Smesta podatke o postu u Firebase bez linka za sliku
            writeNewPost(userId, username, title, body, null);
            finish();
        }
    }

    /*Metod zaduzen za prikaz odabira slike iz galerije
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
        mTitleField.setEnabled(enabled);
        mBodyField.setEnabled(enabled);
        if (enabled) {
            mSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mSubmitButton.setVisibility(View.GONE);
        }
    }

    /*Metod koji sluzi za dobijanje rezultata izbora slike, kao i njen prikaz
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                mImageView.setImageBitmap(bitmap);
                mImageView.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*Metod zaduzen za detekciju klika korisnika
         */
    @Override
    public void onClick(View view) {
        if (view == mUploadPhoto) {
            showFileChooser();
        } else if (view == mSubmitButton) {
            submitPost();
        }
    }
}


