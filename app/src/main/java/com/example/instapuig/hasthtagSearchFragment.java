package com.example.instapuig;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class hasthtagSearchFragment extends homeFragment {

    Query getWallPostsQuery()
    {
        return FirebaseFirestore.getInstance().collection("posts").whereArrayContains("hashtags", appViewModel.getHashtagForSearch().hashtag);
    }

}
