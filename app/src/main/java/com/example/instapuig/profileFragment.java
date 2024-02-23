package com.example.instapuig;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.UUID;


public class profileFragment extends Fragment {

    NavController navController;
    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;

    Button galleryImageButton, confirmChangesButton;
    public AppViewModel appViewModel;

    UserProfile profile = null;
    Uri mediaUri = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);


        photoImageView = view.findViewById(R.id.photoImageView);
        displayNameTextView = view.findViewById(R.id.displayNameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        galleryImageButton = view.findViewById(R.id.imagen_galeria);
        confirmChangesButton = view.findViewById(R.id.boton_guardar);

        appViewModel.getCurrentUserProfile();

        appViewModel.currentUserProfile.observe(getViewLifecycleOwner(), profile1 -> {

            profile = profile1;

            displayNameTextView.setText(profile.displayName);
            emailTextView.setText(profile1.eMail);

            if(profile1.photo != null)
                Glide.with(requireView()).load(profile1.photo).into(photoImageView);
            else
                photoImageView.setImageResource(R.drawable.ic_baseline_co_present_24);
        });

        galleryImageButton.setOnClickListener(view1 -> {
            seleccionarImagen();
        });

        appViewModel.mediaSeleccionado.observe(getViewLifecycleOwner(), media -> {

            if(media != null) {
                Glide.with(this).load(media.uri).into((ImageView) view.findViewById(R.id.photoImageView));
                mediaUri = media.uri;
            }
        });

        confirmChangesButton.setOnClickListener(view1 -> {

            profile.displayName = displayNameTextView.getText().toString();

            if (mediaUri != null)
            {
                pujaIguardarEnFirestore();
            }
            else
            {
                appViewModel.updateCurrentUserProfile(profile);
                //navController.navigate(R.id.homeFragment);
                Snackbar.make(requireView(), "Cambios guardados", Snackbar.LENGTH_LONG).show();
            }
        });

    }

    private void pujaIguardarEnFirestore() {
        FirebaseStorage.getInstance().getReference("profile_pictures/" +
                        UUID.randomUUID())
                .putFile(mediaUri)
                .continueWithTask(task ->
                        task.getResult().getStorage().getDownloadUrl())
                .addOnSuccessListener(url -> {
                        profile.photo = url.toString();
                        appViewModel.updateCurrentUserProfile(profile);
                        appViewModel.mediaSeleccionado.postValue(null);
                        //navController.navigate(R.id.homeFragment);
                        Snackbar.make(requireView(), "Cambios guardados", Snackbar.LENGTH_LONG).show();
                        mediaUri = null;
                     });
    }

    private final ActivityResultLauncher<String> galeria =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                appViewModel.setMediaSeleccionado(uri, "image");
            });

    private void seleccionarImagen() {
        //mediaTipo = "image";
        galeria.launch("image/*");
    }
}