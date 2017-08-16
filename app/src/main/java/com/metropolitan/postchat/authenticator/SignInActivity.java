package com.metropolitan.postchat.authenticator;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.metropolitan.postchat.activities.BaseActivity;
import com.metropolitan.postchat.MainActivity;
import com.metropolitan.postchat.R;
import com.metropolitan.postchat.models.User;

/**
 * Created by mare on 8/8/17.
 */

public class SignInActivity extends BaseActivity {

    // Definisanje Firebase referenci
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    //Definisanje UI pogleda
    private EditText mEmailField;
    private EditText mPasswordField;
    private Button mSignInButton;
    private TextView mSignUptxt;
    private TextView mRecoveryEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_sign_in);
        // Inicijalizacija referenci Baze podataka
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Inicijalizacije UI pogleda
        mSignUptxt = (TextView) findViewById(R.id.sign_up_txt);
        mEmailField = (EditText) findViewById(R.id.email_field_sign_in);
        mPasswordField = (EditText) findViewById(R.id.pass_field_sign_in);
        mSignInButton = (Button) findViewById(R.id.sign_in);
        mRecoveryEmail = (TextView) findViewById(R.id.recovery_email_txt);
        mRecoveryEmail.setVisibility(View.GONE);

        // // Inicijalizacija Click listenera
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
        mRecoveryEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignInActivity.this, PasswordResetActivity.class));

            }
        });

        //Inicijalizacija onClick osluskivaca, kreiranje nove namere i pokretanje SignUp aktivnosti
        mSignUptxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Provera da li je korisnik vec ulogovan
        if (mAuth.getCurrentUser() != null) {
            onAuthSuccess(mAuth.getCurrentUser());
        }
    }

    /*Metod zaduzen za logovanje korisnika
     */
    private void signIn() {
        if (!validateForm()) {
            return;
        }
        showProgressDialog();
        String email = mEmailField.getText().toString();
        String password = mPasswordField.getText().toString();
        //Kreiranje Buildera koji proverava da li su unete vrednosti validne
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        hideProgressDialog();
                        if (task.isSuccessful()) {
                            onAuthSuccess(task.getResult().getUser());
                        } else {
                            Toast.makeText(SignInActivity.this, "Sign In Failed",
                                    Toast.LENGTH_SHORT).show();
                            mRecoveryEmail.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    /*Metod koji kreira username korisnika na osnovu njegove email adrese
     */
    private String usernameFromEmail(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        } else {
            return email;
        }
    }

    /*Metod koji se poziva nakon sto su uneti podaci za logovanje ispravni
     */
    private void onAuthSuccess(FirebaseUser user) {
        String username = usernameFromEmail(user.getEmail());
        // Upisuje novog korisnika
        writeNewUser(user.getUid(), username, user.getEmail());
        // Pokrece se glavna aktivnost
        startActivity(new Intent(SignInActivity.this, MainActivity.class));
        finish();
    }

    /*Metod zaduzen za dodavanje novog korisnika
     */
    private void writeNewUser(String userId, String name, String email) {
        User user = new User(name, email);
        mDatabase.child("users").child(userId).setValue(user);
    }

    /*Metod zaduzen za validaciju unetih podataka
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
}

