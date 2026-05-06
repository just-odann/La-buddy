package com.example.la_buddy;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class AdminInboxAdapter extends RecyclerView.Adapter<AdminInboxAdapter.InboxViewHolder> {

    private ArrayList<InboxModel> inboxList;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(InboxModel user);
    }

    public AdminInboxAdapter(ArrayList<InboxModel> inboxList, OnChatClickListener listener) {
        this.inboxList = inboxList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InboxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inbox_user, parent, false);
        return new InboxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InboxViewHolder holder, int position) {
        InboxModel user = inboxList.get(position);

        holder.tvName.setText(user.getName());
        holder.tvLastMessage.setText(user.getLastMessage());
        holder.tvTime.setText(user.getTimestamp());

        // Highlight logic for unread messages (Messenger style)
        if (user.isUnread()) {
            holder.tvName.setTypeface(null, Typeface.BOLD);
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
            holder.tvLastMessage.setTextColor(Color.BLACK);
        } else {
            holder.tvName.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTextColor(Color.parseColor("#888888"));
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(user));
    }

    @Override
    public int getItemCount() {
        return inboxList.size();
    }

    public static class InboxViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMessage, tvTime;

        public InboxViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvInboxName);
            tvLastMessage = itemView.findViewById(R.id.tvInboxLastMessage);
            tvTime = itemView.findViewById(R.id.tvInboxTime);
        }
    }
}