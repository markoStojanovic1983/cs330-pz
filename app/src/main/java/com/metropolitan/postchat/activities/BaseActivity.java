package com.metropolitan.postchat.activities;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by mare on 8/8/17.
 */

public class BaseActivity extends AppCompatActivity {
    //Definisanje progress dijaloga
    private ProgressDialog mProgressDialog;

    /*Metod koji sluzi za prikaz progres dijaloga
     */
    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage("Loading...");
        }

        mProgressDialog.show();
    }

    /*Metod koji sluzi za sklanjanje progres dijaloga
     */
    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /*Metod koji vraca id trenutno ulogovanog korisnika
     */
    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

}
