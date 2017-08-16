package com.metropolitan.postchat.models;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by mare on 8/8/17.
 */
@IgnoreExtraProperties
public class Comment {

    public String uid;
    public String author;
    public String text;

    /* Obavezni prazan konstruktor koji se koristi za pozivanje DataSnapshot.getValue(User.class)
       koji prikazuje vrednosti objketa Post
        */
    public Comment() {
    }

    /* Konstruktor klase Post
         */
    public Comment(String uid, String author, String text) {
        this.uid = uid;
        this.author = author;
        this.text = text;
    }

}

