package com.example.instapuig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Post {
    public String uid;
    public String postId;
    public String author;
    public String authorPhotoUrl;
    public String originalAuthor;
    public String originalAuthorPhotoUrl;
    public String content;
    public String mediaUrl;
    public String mediaType;
    public Map<String, Boolean> likes = new HashMap<>();
    public List<String> hashtags = new ArrayList<>();

    public long time;

    // Constructor vacio requerido por Firestore
    public Post() {}

    public Post(String uid, String author, String authorPhotoUrl, String content, long time, String mediaUrl, String mediaType) {
        this.uid = uid;
        this.author = author;
        this.authorPhotoUrl = authorPhotoUrl;
        this.content = content;
        this.time = time;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.originalAuthor = null;
        this.originalAuthorPhotoUrl = null;

        processHashtags();
    }

    void processHashtags()
    {
        int pos = 0;
        hashtags = new ArrayList<>();
        hashtags.clear();
        while(pos < content.length())
        {
            if(content.charAt(pos) == '#')
            {
                pos++;
                StringBuilder sb = new StringBuilder();
                while(pos < content.length() && content.charAt(pos) != ' ')
                {
                    sb.append(content.charAt(pos));
                    pos++;
                }
                hashtags.add(sb.toString());


            }
            pos++;
        }

    }
}