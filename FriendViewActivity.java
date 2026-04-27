package com.example.womensafety_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendViewActivity extends AppCompatActivity {
    private RecyclerView rvContacts;
    private ContactAdapter adapter;
    private List<ContactModel> contactList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_friend_view);

        ImageView btnCancel = findViewById(R.id.btn_cancel1);
        btnCancel.setOnClickListener(v -> finish());

        // Firebase Setup
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance("https://womensafety-project-63cfa-default-rtdb.firebaseio.com/")
                .getReference("Users").child(userId).child("EmergencyContacts");

        rvContacts = findViewById(R.id.rv_contacts);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));

        contactList = new ArrayList<>();
        adapter = new ContactAdapter(contactList, mDatabase);
        rvContacts.setAdapter(adapter);

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_contact);

        fabAdd.setOnClickListener(v -> {
            // FriendsActivity-ku pogum (Inga thaan contact save panrom)
            Intent intent = new Intent(FriendViewActivity.this, FriendsActivity.class);
            startActivity(intent);
        });

        fetchContacts();
    }

    private void fetchContacts() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ContactModel model = ds.getValue(ContactModel.class);
                    if (model != null) {
                        // Key-ah fetch panni model-kulla set panrom
                        model.setKey(ds.getKey());
                        contactList.add(model);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}