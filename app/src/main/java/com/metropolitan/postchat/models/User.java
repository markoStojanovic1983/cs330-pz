package com.metropolitan.postchat.models;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by mare on 8/8/17.
 */
@IgnoreExtraProperties
public class User {
    //Definisanje atributa klase User
    public String username;
    public String email;

    /* Obavezni prazan konstruktor koji se koristi za pozivanje DataSnapshot.getValue(User.class)
       koji prikazuje vrednosti objketa User
    */
    public User() {

    }

    /* Konstruktor klase User
     */
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
}

