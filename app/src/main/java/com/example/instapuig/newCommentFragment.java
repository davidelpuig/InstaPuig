package com.example.instapuig;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;


public class newCommentFragment extends Fragment {

    Button publishButton;
    EditText postConentEditText;

    NavController navController;

    public AppViewModel appViewModel;

    UserProfile userProfile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_comment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        publishButton = view.findViewById(R.id.publishButton);
        postConentEditText = view.findViewById(R.id.postContentEditText);

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publicar();
            }
        });

        navController = Navigation.findNavController(view);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        appViewModel.currentUserProfile.observe(getViewLifecycleOwner(), profile -> {
            userProfile = profile;
        });
    }

    void publicar()
    {
        String postContent = postConentEditText.getText().toString();
        if(TextUtils.isEmpty(postContent)){
            postConentEditText.setError("Required");
            return;
        }
        publishButton.setEnabled(false);

        guardarEnFirestore(postContent, null);
    }

    private void guardarEnFirestore(String postContent, String mediaUrl) {
        //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //UserProfile profile = appViewModel.getCurrentUserProfile();
        Post post = appViewModel.postSeleccionado.getValue();
        Comment comment = new Comment(userProfile.userId, userProfile.displayName,
                userProfile.photo,
                postContent,
                Calendar.getInstance().getTimeInMillis());
        FirebaseFirestore.getInstance().collection("posts").document(post.postId).collection("comments")
                .add(comment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        documentReference.update("commentId", documentReference.getId());
                        navController.popBackStack();
                    }
                });
    }
}