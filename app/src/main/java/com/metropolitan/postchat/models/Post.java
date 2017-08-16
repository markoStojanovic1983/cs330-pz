package com.metropolitan.postchat.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mare on 8/8/17.
 */
@IgnoreExtraProperties
public class Post {
    //Definisanje atributa klase Post
    public String uid;
    public String author;
    public String title;
    public String body;
    public String imageUrl;
    public int likeCount = 0;
    public Map<String, Boolean> likes = new HashMap<>();

    /* Obavezni prazan konstruktor koji se koristi za pozivanje DataSnapshot.getValue(User.class)
       koji prikazuje vrednosti objketa Post
        */
    public Post() {

    }

    /* Konstruktor klase Post
     */
    public Post(String uid, String author, String title, String body, String imageUrl) {
        this.uid = uid;
        this.author = author;
        this.title = title;
        this.body = body;
        this.imageUrl = imageUrl;
    }

    /* Kreiranje Hash mape sa vrednostima atributa objekta klase Post
     */
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("author", author);
        result.put("title", title);
        result.put("body", body);
        result.put("imageUrl", imageUrl);
        result.put("likeCount", likeCount);
        result.put("likes", likes);

        return result;
    }
}

