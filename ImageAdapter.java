package com.virtualcamera.app;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    public interface OnImageSelectedListener {
        void onImageSelected(ImageItem item, int position);
        void onImageDelete(ImageItem item, int position);
    }

    private final Context context;
    private final List<ImageItem> items;
    private final OnImageSelectedListener listener;
    private int selectedPosition = -1;

    public ImageAdapter(Context context, List<ImageItem> items, OnImageSelectedListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int old = selectedPosition;
        selectedPosition = position;
        if (old != -1) notifyItemChanged(old);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageItem item = items.get(position);
        boolean isSelected = position == selectedPosition;

        // Load image with Glide
        Glide.with(context)
                .load(Uri.parse(item.getUri()))
                .apply(new RequestOptions()
                        .transforms(new CenterCrop(), new RoundedCorners(16)))
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(holder.ivImage);

        holder.tvName.setText(item.getName());

        // Selection highlight
        holder.cardView.setStrokeWidth(isSelected ? 6 : 0);
        holder.cardView.setStrokeColor(isSelected ?
                context.getColor(R.color.primary) : 0);

        holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.cardView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                listener.onImageSelected(item, pos);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                listener.onImageDelete(item, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivImage;
        ImageView ivCheck;
        TextView tvName;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivImage = itemView.findViewById(R.id.ivImage);
            ivCheck = itemView.findViewById(R.id.ivCheck);
            tvName = itemView.findViewById(R.id.tvName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
