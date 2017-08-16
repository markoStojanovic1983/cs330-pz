package com.metropolitan.postchat.authenticator;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.metropolitan.postchat.R;
import com.metropolitan.postchat.activities.BaseActivity;

/**
 * Created by mare on 8/12/17.
 */


public class PasswordResetActivity extends BaseActivity {
    // Definisanje Firebase referenci
    private FirebaseAuth auth;
    //Definisanje UI pogleda
    private EditText mEmailResetField;
    private Button mResetBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_reset_password);
        // Inicijalizacija referenci Baze podataka
        auth = FirebaseAuth.getInstance();
        // Inicijalizacije UI pogleda
        mEmailResetField = (EditText) findViewById(R.id.email_resset_txt);
        mResetBtn = (Button) findViewById(R.id.reset_email_button);

        //Definisanje onClick osluskivaca, validacija forme, i pokretanje metoda sendPasswordResetEmail
        mResetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!validateForm()) {
                    return;
                }
                String email = mEmailResetField.getText().toString().trim();

                auth.sendPasswordResetEmail(email)

                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(PasswordResetActivity.this,
                                            "We have sent you instructions to reset your password!",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(PasswordResetActivity.this,
                                            "Wrong email adress",
                                            Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
            }
        });
    }

    /*Metod zaduzen za validaciju unetih podataka
        */
    private boolean validateForm() {
        boolean result = true;
        if (TextUtils.isEmpty(mEmailResetField.getText().toString())) {
            mEmailResetField.setError("Required");
            result = false;
        }
        return result;
    }
}


