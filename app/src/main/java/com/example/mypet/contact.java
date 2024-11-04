package com.example.mypet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class contact extends AppCompatActivity {

    private ArrayList<Contact> contactList = new ArrayList<>();
    private GridLayout contactGrid;
    private TextView noContactText;
    private static final int MAX_CONTACTS = 8;

    // Firebase database reference
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Handle user not logged in case
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("contacts");

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Finish the current activity to go back
            }
        });

        contactGrid = findViewById(R.id.contact_grid);
        noContactText = findViewById(R.id.no_contact_text);
        Button addContactButton = findViewById(R.id.add_contact_button);

        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactList.size() < MAX_CONTACTS) {
                    showContactDialog(null, -1);
                } else {
                    Toast.makeText(contact.this, "You can only create up to 8 contacts", Toast.LENGTH_SHORT).show();
                }
            }
        });

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Contact contact = dataSnapshot.getValue(Contact.class);
                    contactList.add(contact);
                }
                updateContactGrid();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(contact.this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateContactGrid() {
        contactGrid.removeAllViews();
        if (contactList.isEmpty()) {
            noContactText.setVisibility(View.VISIBLE);
        } else {
            noContactText.setVisibility(View.GONE);
            for (int i = 0; i < contactList.size(); i++) {
                addContactCard(contactList.get(i), i);
            }
        }
    }

    private void addContactCard(final Contact contact, final int index) {
        CardView cardView = (CardView) LayoutInflater.from(this).inflate(R.layout.activity_contact_card, contactGrid, false);

        TextView nameTextView = cardView.findViewById(R.id.contact_name);
        TextView phoneTextView = cardView.findViewById(R.id.contact_phone);
        ImageButton editButton = cardView.findViewById(R.id.edit_contact_button);
        ImageButton deleteButton = cardView.findViewById(R.id.delete_contact_button);

        nameTextView.setText(contact.getName());
        phoneTextView.setText(contact.getPhone());

        phoneTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callContact(contact.getPhone());
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactDialog(contact, index);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                databaseReference.child(contact.getPhone()).removeValue();
                contactList.remove(index);
                updateContactGrid();
            }
        });

        contactGrid.addView(cardView);
    }

    private void showContactDialog(final Contact contact, final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.activity_dialog_add_contact, null);
        builder.setView(dialogView);

        final EditText nameEditText = dialogView.findViewById(R.id.edit_contact_name);
        final EditText phoneEditText = dialogView.findViewById(R.id.edit_contact_phone);
        Button saveButton = dialogView.findViewById(R.id.save_contact_button);

        if (contact != null) {
            nameEditText.setText(contact.getName());
            phoneEditText.setText(contact.getPhone());
        }

        final AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEditText.getText().toString();
                String phone = phoneEditText.getText().toString();

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(contact.this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                } else {
                    Contact newContact = new Contact(name, phone);
                    if (contact == null) {
                        databaseReference.child(phone).setValue(newContact);
                    } else {
                        databaseReference.child(contact.getPhone()).removeValue();
                        databaseReference.child(phone).setValue(newContact);
                    }
                    updateContactGrid();
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    private void callContact(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }

    public static class Contact {
        private String name;
        private String phone;

        public Contact() {}

        public Contact(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}
