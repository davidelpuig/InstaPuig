package com.example.instapuig;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class homeFragment extends Fragment {

    NavController navController;
    public AppViewModel appViewModel;

    UserProfile userProfile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.newPostFragment);
            }
        });

        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);

        Query query = FirebaseFirestore.getInstance().collection("posts").orderBy("time", Query.Direction.DESCENDING).limit(50);

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class)
                .setLifecycleOwner(this)
                .build();

        PostsAdapter adapter = new PostsAdapter(options);
        adapter.setFragment(this);
        postsRecyclerView.setAdapter(adapter);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        appViewModel.currentUserProfile.observe(getViewLifecycleOwner(), profile -> {
            userProfile = profile;
        });
    }

    class PostsAdapter extends FirestoreRecyclerAdapter<Post, PostsAdapter.PostViewHolder> {
        public PostsAdapter(@NonNull FirestoreRecyclerOptions<Post> options) {super(options);}

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @Override
        protected void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull final Post post) {
            if(post.authorPhotoUrl == null)
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            else
                Glide.with(getContext()).load(post.authorPhotoUrl).circleCrop().into(holder.authorPhotoImageView);
            holder.authorTextView.setText(post.author);
            holder.contentTextView.setText(post.content);

            // Original author on shared posts
            if(post.originalAuthor != null)
            {
                holder.originalAuthorInfo.setVisibility(View.VISIBLE);
                holder.originalAuthorTextView.setText(post.originalAuthor);
                if(post.originalAuthorPhotoUrl == null)
                    holder.originalAuthorPhotoImageView.setImageResource(R.drawable.user);
                else
                    Glide.with(getContext()).load(post.originalAuthorPhotoUrl).circleCrop().into(holder.originalAuthorPhotoImageView);
            }
            else
            {
                holder.originalAuthorInfo.setVisibility(View.GONE);
            }

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(post.time);

            holder.timeTextView.setText( formatter.format(calendar.getTime()));

            // Gestion de likes
            final String postKey = getSnapshots().getSnapshot(position).getId();
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if(post.likes.containsKey(uid))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(post.likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                FirebaseFirestore.getInstance().collection("posts")
                        .document(postKey)
                        .update("likes."+uid, post.likes.containsKey(uid) ?
                                FieldValue.delete() : true);
            });

            // Delete
            if (post.uid.equals(uid))
            {
                holder.deleteImageView.setVisibility(View.VISIBLE);
                holder.deleteImageView.setOnClickListener(view -> {
                    FirebaseFirestore.getInstance().collection("posts").document(post.postId).delete();
                });
            }
            else
            {
                holder.deleteImageView.setVisibility(View.GONE);
            }

            // Miniatura de media
            if (post.mediaUrl != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.mediaType)) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.mediaUrl).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }

            // Recycler view de comentarios
            RecyclerView commentRecyclerView = holder.commentsRecyclerView;

            Query query = FirebaseFirestore.getInstance().collection("posts").document(post.postId).collection("comments").orderBy("time", Query.Direction.DESCENDING).limit(50);

            FirestoreRecyclerOptions<Comment> options = new FirestoreRecyclerOptions.Builder<Comment>()
                    .setQuery(query, Comment.class)
                    .setLifecycleOwner(fragment)
                    .build();

            CommentsAdapter adapter = new CommentsAdapter(options);
            adapter.postKey = post.postId;
            commentRecyclerView.setAdapter(adapter);

            // Show / hide all comments
            if(holder.displayAllComments)
            {
                commentRecyclerView.setVisibility(View.VISIBLE);
                holder.showAllCommentstextView.setText("Ocultar los comentarios");
            }
            else
            {
                commentRecyclerView.setVisibility(View.GONE);
                holder.showAllCommentstextView.setText("Mostrar todos los comentarios");
            }

            holder.showAllCommentstextView.setOnClickListener(view -> {
                if(holder.displayAllComments)
                {
                    holder.displayAllComments = false;
                    commentRecyclerView.setVisibility(View.GONE);
                    holder.showAllCommentstextView.setText("Mostrar todos los comentarios");
                }
                else
                {
                    holder.displayAllComments = true;
                    commentRecyclerView.setVisibility(View.VISIBLE);
                    holder.showAllCommentstextView.setText("Ocultar los comentarios");
                }
            });

            // Share button
            holder.shareImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    //UserProfile profile = appViewModel.getCurrentUserProfile();

                    Post p = new Post(user.getUid(), userProfile.displayName+" ha compartido un post:", userProfile.photo, post.content,
                                    System.currentTimeMillis(), post.mediaUrl, post.mediaType);

                    p.originalAuthor = post.author;
                    p.originalAuthorPhotoUrl = post.authorPhotoUrl;

                    FirebaseFirestore.getInstance().collection("posts")
                            .add(p)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    documentReference.update("postId", documentReference.getId());
                                }
                            });
                }
            });

            //Comment button
            holder.commentTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.newCommentFragment);
                }
            });
        }

        Fragment fragment;
        public void setFragment(Fragment f)
        {
            fragment = f;
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView authorPhotoImageView, originalAuthorPhotoImageView, likeImageView, mediaImageView, deleteImageView, shareImageView;
            TextView authorTextView, originalAuthorTextView, contentTextView, numLikesTextView, timeTextView, showAllCommentstextView;
            TextView commentTextView;
            RecyclerView commentsRecyclerView;

            LinearLayout originalAuthorInfo;

            boolean displayAllComments;

            PostViewHolder(@NonNull View itemView) {
                super(itemView);

                displayAllComments = false;

                authorPhotoImageView = itemView.findViewById(R.id.photoImageView);
                timeTextView = itemView.findViewById(R.id.timeTextView);
                authorTextView = itemView.findViewById(R.id.authorTextView);
                contentTextView = itemView.findViewById(R.id.contentTextView);
                likeImageView = itemView.findViewById(R.id.likeImageView);
                numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
                mediaImageView = itemView.findViewById(R.id.mediaImage);
                commentsRecyclerView = itemView.findViewById(R.id.commentsRecyclerView);
                commentTextView = itemView.findViewById(R.id.commentTextView);
                deleteImageView = itemView.findViewById(R.id.deleteImageView);
                shareImageView = itemView.findViewById(R.id.forwardImageView);
                showAllCommentstextView = itemView.findViewById(R.id.showAllCommentsTextView);
                originalAuthorInfo = itemView.findViewById(R.id.originalAuthorInfo);
                originalAuthorPhotoImageView = itemView.findViewById(R.id.originalAuthorPhotoImageView);
                originalAuthorTextView = itemView.findViewById(R.id.originalAuthorTextView);

            }
        }
    }

    class CommentsAdapter extends FirestoreRecyclerAdapter<Comment, CommentsAdapter.CommentViewHolder> {
        public CommentsAdapter(@NonNull FirestoreRecyclerOptions<Comment> options) {super(options);}

        @NonNull
        @Override
        public CommentsAdapter.CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CommentsAdapter.CommentViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_comment, parent, false));
        }

        @Override
        protected void onBindViewHolder(@NonNull CommentsAdapter.CommentViewHolder holder, int position, @NonNull final Comment comment) {
                    if(comment.authorPhotoUrl == null)
                        holder.authorPhotoImageView.setImageResource(R.drawable.user);
                    else
                        Glide.with(getContext()).load(comment.authorPhotoUrl).circleCrop().into(holder.authorPhotoImageView);
                    holder.authorTextView.setText(comment.author);
                    holder.contentTextView.setText(comment.content);

                    SimpleDateFormat formatter = new SimpleDateFormat("  dd/MM/yyyy HH:mm");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(comment.time);

                    holder.timeTextView.setText( formatter.format(calendar.getTime()));

                    // Gestion de likes
                    final String commentKey = getSnapshots().getSnapshot(position).getId();
                    final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    if(comment.likes.containsKey(uid))
                        holder.likeImageView.setImageResource(R.drawable.like_on);
                    else
                        holder.likeImageView.setImageResource(R.drawable.like_off);
                    holder.numLikesTextView.setText(String.valueOf(comment.likes.size()));
                    holder.likeImageView.setOnClickListener(view -> {
                        FirebaseFirestore.getInstance().collection("posts")
                                .document(postKey)
                                .collection("comments")
                                .document(commentKey)
                                .update("likes."+uid, comment.likes.containsKey(uid) ?
                                        FieldValue.delete() : true);
                    });

                    // Delete
                    if (comment.uid.equals(uid))
                    {
                        holder.deleteImageView.setVisibility(View.VISIBLE);
                        holder.deleteImageView.setOnClickListener(view -> {
                            FirebaseFirestore.getInstance().collection("posts").document(postKey).collection("comments").document(comment.commentId).delete();
                        });
                    }
                    else
                    {
                        holder.deleteImageView.setVisibility(View.GONE);
                    }
/*
                    // Miniatura de media
                    if (post.mediaUrl != null) {
                        holder.mediaImageView.setVisibility(View.VISIBLE);
                        if ("audio".equals(post.mediaType)) {
                            Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                        } else {
                            Glide.with(requireView()).load(post.mediaUrl).centerCrop().into(holder.mediaImageView);
                        }
                        holder.mediaImageView.setOnClickListener(view -> {
                            appViewModel.postSeleccionado.setValue(post);
                            navController.navigate(R.id.mediaFragment);
                        });
                    } else {
                        holder.mediaImageView.setVisibility(View.GONE);
                    }*/
        }

        public String postKey;

        class CommentViewHolder extends RecyclerView.ViewHolder {
            ImageView authorPhotoImageView, likeImageView, deleteImageView /*,mediaImageView*/;
            TextView authorTextView, contentTextView, numLikesTextView, timeTextView;

            CommentViewHolder(@NonNull View itemView) {
                super(itemView);

                authorPhotoImageView = itemView.findViewById(R.id.photoImageView);
                timeTextView = itemView.findViewById(R.id.timeTextView);
                authorTextView = itemView.findViewById(R.id.authorTextView);
                contentTextView = itemView.findViewById(R.id.contentTextView);
                likeImageView = itemView.findViewById(R.id.likeImageView);
                numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
                //mediaImageView = itemView.findViewById(R.id.mediaImage);
                deleteImageView = itemView.findViewById(R.id.deleteImageView);
            }
        }
    }
}