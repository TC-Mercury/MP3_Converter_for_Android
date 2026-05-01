package com.example.mercuryconverter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private Context context;
    private Cursor cursor;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onLinkSelected(String url);
    }
    public HistoryAdapter(Context context, Cursor cursor, OnItemClickListener listener) {
        this.context = context;
        this.cursor = cursor;
        this.listener = listener;
    }
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }
        String title = cursor.getString(cursor.getColumnIndexOrThrow("youtube_title"));
        String url = cursor.getString(cursor.getColumnIndexOrThrow("youtube_url"));
        String date = cursor.getString(cursor.getColumnIndexOrThrow("download_date"));

        holder.tvSongTitle.setText(title);
        holder.tvDownloadDate.setText(date);
        holder.itemView.setOnClickListener(v -> {
            if(listener != null){
                listener.onLinkSelected(url);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    public class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongTitle;
        TextView tvDownloadDate;
        Button btnDownload;
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvDownloadDate = itemView.findViewById(R.id.tvDownloadDate);
            btnDownload = itemView.findViewById(R.id.btnDownload);
        }
    }

    public void swapCursor(Cursor newCursor){
        if(cursor != null){
            cursor.close();
        }
        cursor = newCursor;
        if(newCursor != null){
            notifyDataSetChanged();
        }
    }
}
