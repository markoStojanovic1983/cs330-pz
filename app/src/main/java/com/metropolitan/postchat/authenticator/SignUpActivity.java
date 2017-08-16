package com.metropolitan.postchat.authenticator;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.metropolitan.postchat.activities.BaseActivity;
import com.metropolitan.postchat.MainActivity;
import com.metropolitan.postchat.R;
import com.metropolitan.postchat.models.User;

/**
 * Created by mare on 8/8/17.
 */

public class SignUpActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    // Definisanje request coda za Google Sign in
    private static final int RC_SIGN_IN = 9001;

    // Definisanje Firebase referenci
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    // Definisanje Google API klijenta
    private GoogleApiClient mGoogleApiClient;

    // Definisanje UI pogleda
    private SignInButton mGoogleSignInButton;
    private EditText mEmailField;
    private EditText mPasswordField;
    private Button mSignUpButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_sign_up);

        // Inicijalizacija referenci Baze podataka
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Inicijalizacija Google API klijenta
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // Inicijalizacije UI pogleda
        mGoogleSignInButton = (SignInButton) findViewById(R.id.sign_up_google);
        mEmailField = (EditText) findViewById(R.id.email_field_sign_up);
        mPasswordField = (EditText) findViewById(R.id.pass_field_sign_up);
        mSignUpButton = (Button) findViewById(R.id.sign_up);

        // Incijalizacija onClick osluskivaca
        mSignUpButton.setOnClickListener(this);
        mGoogleSignInButton.setOnClickListener(this);
    }

    /* Metod zaduzen za kreiranje novog korisnika
     */
    private void signUp() {
        // Proverava da li su uneti parametri
        if (!validateForm()) {
            return;
        }

        showProgressDialog();
        String email = mEmailField.getText().toString();
        String password = mPasswordField.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        hideProgressDialog();

                        if (task.isSuccessful()) {
                            onAuthSuccess(task.getResult().getUser());
                        } else {
                            Toast.makeText(SignUpActivity.this, "Sign Up Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    /* Metod koji se pozivasto je kreiran nov user
     */
    private void onAuthSuccess(FirebaseUser user) {
        String username = usernameFromEmail(user.getEmail());

        // Write new user
        writeNewUser(user.getUid(), username, user.getEmail());

        // Go to MainActivity
        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
        finish();
    }

    /* Metod koji kreira username korisnika na osnovu njegove email adrese
     */
    private String usernameFromEmail(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        } else {
            return email;
        }
    }

    /* Metod zaduzen za dodavanje novog korisnika
     */
    private void writeNewUser(String userId, String name, String email) {
        User user = new User(name, email);

        mDatabase.child("users").child(userId).setValue(user);
    }

    /* Metod zaduzen za autentifikaciju pomocu Google naloga
        */
    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {

        final AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {

                            Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            AuthResult result = task.getResult();
                            FirebaseUser user = result.getUser();
                            onAuthSuccess(user);
                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                });
    }

    /* Metod zaduzen za validaciju unetih podataka
         */
    private boolean validateForm() {
        boolean result = true;
        if (TextUtils.isEmpty(mEmailField.getText().toString())) {
            mEmailField.setError("Required");
            result = false;
        } else {
            mEmailField.setError(null);
        }

        if (TextUtils.isEmpty(mPasswordField.getText().toString())) {
            mPasswordField.setError("Required");
            result = false;
        } else {
            mPasswordField.setError(null);
        }
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Rezultat izvsavanja namere GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In uspresan, autentifikacija u Firebase-u
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);

            } else {
                // Google Sign-In neuspesan
                Toast.makeText(this, "Google Sign in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*Metod koji se poziva ukoliko aplikacija nije mogla da se poveze sa bazom podataka
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Detektovanje greske pri Google Sign In-u

        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    /* Metod zaduzen za detekciju izbora vrste kreiranja novog naloga od strane korisnika
     */
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sign_up) {
            signUp();
        } else if (i == R.id.sign_up_google) {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    }
}
