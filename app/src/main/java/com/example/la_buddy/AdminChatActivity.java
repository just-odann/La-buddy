package com.example.la_buddy;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdminChatActivity extends AppCompatActivity {

    private String customerUid;
    private String customerName;
    private RecyclerView rvChatMessages;
    private EditText etMessageInput;
    private Button btnSendMsg, btnBack;
    private TextView tvChatCustomerName;

    private DatabaseReference chatRef;
    private ArrayList<ChatMessage> messageList;
    private ChatAdapter chatAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat);

        customerUid = getIntent().getStringExtra("CUSTOMER_UID");
        customerName = getIntent().getStringExtra("CUSTOMER_NAME");

        if (customerUid == null || customerUid.isEmpty()) {
            finish();
            return;
        }

        // 🚨 FIX 1: MARK AS READ ON OPEN 🚨
        // As soon as the Admin opens this chat, we set hasUnread to false.
        FirebaseDatabase.getInstance().getReference("Users")
                .child(customerUid)
                .child("hasUnread").setValue(false);

        // DEBUG: Check if we are actually getting a UID or just a name
        Log.d("CHAT_DEBUG", "Connected to UID: " + customerUid);

        if (customerUid == null || customerUid.isEmpty()) {
            Toast.makeText(this, "Error: Customer ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvChatCustomerName = findViewById(R.id.tvChatCustomerName);
        rvChatMessages = findViewById(R.id.rvChatMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMsg = findViewById(R.id.btnSendMsg);
        btnBack = findViewById(R.id.btnBack);

        tvChatCustomerName.setText(customerName != null ? customerName : "Customer Chat");

        // 2. Setup RecyclerView
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        rvChatMessages.setAdapter(chatAdapter);

        // 3. SYNCED PATH: Matches HomeActivity exactly
        chatRef = FirebaseDatabase.getInstance().getReference("Orders")
                .child(customerUid)
                .child("messages");

        loadMessages();

        btnSendMsg.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });
    }

    private void loadMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                Log.d("CHAT_DEBUG", "Messages found: " + snapshot.getChildrenCount());

                for (DataSnapshot snap : snapshot.getChildren()) {
                    ChatMessage msg = snap.getValue(ChatMessage.class);
                    if (msg != null) {
                        messageList.add(msg);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                if (messageList.size() > 0) {
                    rvChatMessages.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CHAT_DEBUG", "Database Error: " + error.getMessage());
            }
        });
    }

    private void sendMessage() {
        String msgText = etMessageInput.getText().toString().trim();
        if (msgText.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        ChatMessage newMessage = new ChatMessage(msgText, "admin", currentTime);

        // 1. Save to the actual chat conversation
        chatRef.push().setValue(newMessage).addOnSuccessListener(aVoid -> {
            // 2. Update the "Users" node to trigger sorting in the Admin Inbox
            DatabaseReference userUpdateRef = FirebaseDatabase.getInstance().getReference("Users").child(customerUid);

            java.util.HashMap<String, Object> update = new java.util.HashMap<>();
            update.put("lastMessage", "Admin: " + msgText);
            update.put("lastMessageTimestamp", currentTime); // 🚨 This moves it to the top!
            update.put("hasUnread", false);                  // Admin reply means it's read

            userUpdateRef.updateChildren(update);

            // 3. Update legacy field (Optional, for your C# dashboard/Table compatibility)
            FirebaseDatabase.getInstance().getReference("Orders")
                    .child(customerUid)
                    .child("adminMessage").setValue(msgText);

            etMessageInput.setText("");
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    // ====================================================================
    // ALL-IN-ONE CLASSES
    // ====================================================================

    public static class ChatMessage {
        public String sender;
        public String text;
        public long timestamp;

        public ChatMessage() {}

        public ChatMessage(String text, String sender, long timestamp) {
            this.text = text;
            this.sender = sender;
            this.timestamp = timestamp;
        }
    }

    public static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private ArrayList<ChatMessage> list;

        public ChatAdapter(ArrayList<ChatMessage> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 1. Create the container layout
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(16, 8, 16, 8);

            // 2. Create the message bubble (TextView)
            TextView textView = new TextView(parent.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textView.setLayoutParams(params);
            textView.setPadding(35, 20, 35, 20); // Comfortable padding inside bubble
            textView.setTextSize(16f);
            textView.setMaxWidth(750); // Keep bubbles from touching the other side

            layout.addView(textView);
            return new ChatViewHolder(layout, textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage msg = list.get(position);
            holder.textView.setText(msg.text);

            // Create rounded background
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(40); // Rounded "Messenger" style

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.textView.getLayoutParams();

            // 🚨 COLOR & ALIGNMENT LOGIC 🚨
            if ("admin".equals(msg.sender)) {
                // ADMIN: Right side, Blue background, White text
                ((LinearLayout) holder.itemView).setGravity(Gravity.END);
                gd.setColor(Color.parseColor("#0084FF")); // Deep Messenger Blue
                holder.textView.setTextColor(Color.WHITE);
                params.gravity = Gravity.END;
            } else {
                // CUSTOMER: Left side, Gray background, Black text
                ((LinearLayout) holder.itemView).setGravity(Gravity.START);
                gd.setColor(Color.parseColor("#E4E6EB")); // Visible light gray
                holder.textView.setTextColor(Color.BLACK);
                params.gravity = Gravity.START;
            }

            holder.textView.setLayoutParams(params);
            holder.textView.setBackground(gd);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public static class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            public ChatViewHolder(@NonNull View itemView, TextView textView) {
                super(itemView);
                this.textView = textView;
            }
        }
    }
}