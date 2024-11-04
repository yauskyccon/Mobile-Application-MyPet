package com.example.mypet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class PetOptionsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ItemClickListener itemClickListener;
    MainAdapter adapter;
    Button submitButton;

    // Firebase
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_options);

        recyclerView = findViewById(R.id.recycler_view);
        submitButton = findViewById(R.id.button);

        // Initialize Firebase Authentication and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("DOG ");
        arrayList.add("CAT ");

        itemClickListener = new ItemClickListener() {
            @Override
            public void onClick(String s) {

                recyclerView.post(new Runnable() {

                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                }); // <-- Added closing parenthesis here

                Toast.makeText(getApplicationContext(), "Selected: " + s, Toast.LENGTH_SHORT).show();
            }
        };

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MainAdapter(arrayList, itemClickListener);
        recyclerView.setAdapter(adapter);

        // Submit button click listener
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePetTypeToFirebase();
            }
        });
    }

    private void savePetTypeToFirebase() {

        String email = getIntent().getStringExtra("user_email");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String selectedPetType = adapter.getSelectedPetType();
            if (selectedPetType != null) {
                String userId = currentUser.getUid();
                // Save selected pet type to Firebase
                mDatabase.child("users").child(userId).child("petType").setValue(selectedPetType)
                        .addOnSuccessListener(aVoid -> {
                            // Navigate to MainMenuActivity
                            Intent intent = new Intent(PetOptionsActivity.this, mainmenu.class);
                            intent.putExtra("user_email", email);
                            startActivity(intent);
                            finish(); // Close this activity
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(PetOptionsActivity.this, "Failed to save pet type: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Please select a pet type.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // User is not authenticated, handle this case accordingly
            // For example, you can redirect the user to the login screen
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }
}
