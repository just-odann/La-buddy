package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdminMessagesActivity extends AppCompatActivity {

    private RecyclerView rvInbox;
    private AdminInboxAdapter adapter;
    private ArrayList<InboxModel> inboxList;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_messages);

        rvInbox = findViewById(R.id.rvInbox);
        rvInbox.setLayoutManager(new LinearLayoutManager(this));

        inboxList = new ArrayList<>();

        // 🚨 UPDATE 1: THE CHAT LINK IS LIVE 🚨
        adapter = new AdminInboxAdapter(inboxList, user -> {
            Intent intent = new Intent(this, AdminChatActivity.class);
            intent.putExtra("CUSTOMER_UID", user.getUid());
            intent.putExtra("CUSTOMER_NAME", user.getName());

            // Kill the slide animation when opening the chat
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        rvInbox.setAdapter(adapter);

        setupNavigation();
        loadInboxUsers();
    }

    private void loadInboxUsers() {
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                inboxList.clear();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String uid = userSnapshot.getKey();

                    // Declare name
                    String name = userSnapshot.child("name").getValue(String.class);
                    if (name == null || name.isEmpty()) name = "New Customer";

                    // Declare lastMsg
                    String lastMsg = userSnapshot.child("lastMessage").getValue(String.class);
                    if (lastMsg == null || lastMsg.isEmpty()) lastMsg = "Tap to start a conversation";

                    // Pull numeric data
                    Long ts = userSnapshot.child("lastMessageTimestamp").getValue(Long.class);
                    long timestampLong = (ts != null) ? ts : 0;

                    Boolean unreadObj = userSnapshot.child("hasUnread").getValue(Boolean.class);
                    boolean isUnread = (unreadObj != null) ? unreadObj : false;

                    // Create model with all parameters
                    InboxModel inboxItem = new InboxModel(uid, name, lastMsg, formatTime(timestampLong), isUnread);
                    inboxItem.setSortTimestamp(timestampLong);

                    inboxList.add(inboxItem);
                }

                // Messenger Sort: Newest (largest number) at index 0
                java.util.Collections.sort(inboxList, (a, b) -> Long.compare(b.getSortTimestamp(), a.getSortTimestamp()));

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("DATABASE_ERROR", error.getMessage());
            }
        });
    }

    private String formatTime(long timestamp) {
        if (timestamp == 0) return "Just now";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }


    // 🚨 UPDATE 2: Kills the slide animation for the physical phone back button 🚨
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    // 🚨 UPDATE 3: THE NUCLEAR NAVIGATION FIX 🚨
    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        // Highlights the Messages icon!
        bottomNav.setSelectedItemId(R.id.nav_messages);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // If they click the button of the screen they are already on, do nothing.
            if (id == bottomNav.getSelectedItemId()) {
                return true;
            }

            Intent intent = null;
            if (id == R.id.nav_dashboard) {
                intent = new Intent(this, AdminDashboardActivity.class);
            } else if (id == R.id.nav_messages) {
                intent = new Intent(this, AdminMessagesActivity.class);
            } else if (id == R.id.nav_history) {
                intent = new Intent(this, AdminHistoryActivity.class);
            } else if (id == R.id.nav_finance) {
                intent = new Intent(this, AdminFinanceActivity.class);
            }

            if (intent != null) {
                // Strip all animations and clear old screens from memory
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                // Override the entry animation
                overridePendingTransition(0, 0);
                finish();

                // Override the exit animation
                overridePendingTransition(0, 0);
            }
            return true;
        });
    }
}