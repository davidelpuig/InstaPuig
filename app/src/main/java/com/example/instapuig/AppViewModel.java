package com.example.instapuig;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AppViewModel extends AndroidViewModel {
    public static class Media {
        public Uri uri;
        public String tipo;
        public Media(Uri uri, String tipo) {
            this.uri = uri;
            this.tipo = tipo;
        }
    }
    public MutableLiveData<Post> postSeleccionado = new MutableLiveData<>();
    public MutableLiveData<Media> mediaSeleccionado = new MutableLiveData<>();

    public MutableLiveData<UserProfile> currentUserProfile = new MutableLiveData<>();
    public AppViewModel(@NonNull Application application) {
        super(application);
    }
    public void setMediaSeleccionado(Uri uri, String type) {
        mediaSeleccionado.setValue(new Media(uri, type));
    }

    public void updateCurrentUserProfile(UserProfile profile)
    {
        currentUserProfile.postValue(profile);
        FirebaseFirestore.getInstance().collection("user_profiles").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).set(profile);
    }
    public void getCurrentUserProfile() {
        //UserProfile profile = new UserProfile();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseFirestore.getInstance().collection("user_profiles").document(user.getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        UserProfile profile;
                        if (task.isSuccessful()) {
                            // Document found in the offline cache
                            DocumentSnapshot document = task.getResult();
                            profile = document.toObject(UserProfile.class);

                            if(profile == null)
                            {
                                profile = new UserProfile();
                                profile.userId = user.getUid();

                                if (user.getDisplayName() == null) {
                                    profile.displayName = user.getEmail();
                                } else {
                                    profile.displayName = user.getDisplayName();
                                }

                                profile.eMail = user.getEmail();

                                if (user.getPhotoUrl() == null) {
                                    profile.photo = null;
                                } else {
                                    profile.photo = user.getPhotoUrl().toString();
                                }
                            }
                            //Log.d(TAG, "Cached document data: " + document.getData());
                        } else {
                            //Log.d(TAG, "Cached get failed: ", task.getException());
                            profile = null;
                        }
                        currentUserProfile.postValue(profile);
                    }
                });
    }

}