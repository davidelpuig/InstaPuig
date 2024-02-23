package com.example.instapuig;

import java.util.HashMap;
import java.util.Map;

public class Comment {

    public String commentId;
    public String uid;
    public String author;
    public String authorPhotoUrl;
    public String content;
    /*public String mediaUrl;
    public String mediaType;*/
    public Map<String, Boolean> likes = new HashMap<>();
    public long time;

    // Constructor vacio requerido por Firestore
    public Comment() {}

    public Comment(String uid, String author, String authorPhotoUrl, String content, long time/*, String mediaUrl, String mediaType*/) {
        this.uid = uid;
        this.author = author;
        this.authorPhotoUrl = authorPhotoUrl;
        this.content = content;
        this.time = time;
        /*this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;*/
    }
}
