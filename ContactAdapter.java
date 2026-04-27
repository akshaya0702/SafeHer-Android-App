package com.example.womensafety_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DatabaseReference;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private final List<ContactModel> contactList;
    private final DatabaseReference mDatabase;

    public ContactAdapter(List<ContactModel> contactList, DatabaseReference mDatabase) {
        this.contactList = contactList;
        this.mDatabase = mDatabase;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Corrected inflate logic
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactModel contact = contactList.get(position);
        holder.tvName.setText(contact.getName());

        // Drawing-la pottu irukkira 'X' button (Delete logic)
        holder.btnDelete.setOnClickListener(v -> {
            if (contact.getKey() != null) {
                mDatabase.child(contact.getKey()).removeValue()
                        .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "Contact Deleted", Toast.LENGTH_SHORT).show());            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView btnDelete;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_contact_name_item);
            btnDelete = itemView.findViewById(R.id.btn_delete_contact);
        }
    }
}