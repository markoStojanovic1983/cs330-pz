package com.metropolitan.postchat;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.metropolitan.postchat.models.Post;
/**
 * Created by mare on 8/10/17.
 */
public class PostViewHolder extends RecyclerView.ViewHolder {

    //Definisanje UI pogleda
    public TextView titleView;
    public TextView authorView;
    public ImageView likeView;
    public TextView numStarsView;
    public TextView bodyView;

    /*Metod koji kreira ViewHolder radi prikaza unutar RecyclerView-a
     */
    public PostViewHolder(View itemView) {
        super(itemView);
        // Inicijalizacije UI pogleda
        titleView = (TextView) itemView.findViewById(R.id.post_title);
        authorView = (TextView) itemView.findViewById(R.id.post_author);
        likeView = (ImageView) itemView.findViewById(R.id.star);
        numStarsView = (TextView) itemView.findViewById(R.id.post_num_stars);
        bodyView = (TextView) itemView.findViewById(R.id.post_body);

    }
    /*Metod zaduzen za prikaz posta nakon promene stanja like dugmeta
     */
    public void bindToPost(Post post, View.OnClickListener starClickListener) {
        titleView.setText(post.title);
        authorView.setText(post.author);
        numStarsView.setText(String.valueOf(post.likeCount));
        bodyView.setText(post.body);
        likeView.setOnClickListener(starClickListener);
    }
    /*Metod zaduzen za brisanje posta
     */
    public void deletePost(Post post, View.OnLongClickListener deleteClickListener){
        titleView.setText(post.title);
        authorView.setText(post.author);
        numStarsView.setText(String.valueOf(post.likeCount));
        bodyView.setText(post.body);
        authorView.setOnLongClickListener(deleteClickListener);

}}
