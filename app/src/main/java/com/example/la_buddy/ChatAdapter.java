package com.example.la_buddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messageList;
    private String currentUserId;

    public ChatAdapter(List<ChatMessage> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        // 🚨 Make sure this uses getSender(), not getSenderId()
        String sender = messageList.get(position).getSender();

        if (sender != null && sender.equals(currentUserId)) {
            return 1; // Sent (Right)
        } else {
            return 2; // Received (Left)
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        try {
            if (viewType == 1) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_sent, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_received, parent, false);
            }
        } catch (Exception e) {
            // Backup: If the XML is missing, use a simple text view to prevent a crash
            view = new TextView(parent.getContext());
        }
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        if (holder.tvMessage != null) {
            holder.tvMessage.setText(message.getText());
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            // This is the most likely spot for the crash!
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}