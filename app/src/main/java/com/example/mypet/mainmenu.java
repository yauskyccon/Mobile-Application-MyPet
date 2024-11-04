package com.example.mypet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class mainmenu extends AppCompatActivity {


    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmenu);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        CardView button = findViewById(R.id.profile_card);
        CardView logout = findViewById(R.id.logout_card);
        CardView feedingCard = findViewById(R.id.feeding_card);
        CardView contactCard = findViewById(R.id.contact_card);

        String email = getIntent().getStringExtra("user_email");
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            checkPetType(userId);
        } else {
            // Handle the case where there is no authenticated user
            Toast.makeText(this, "No authenticated user found", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mainmenu.this, PetProfileActivity.class);
                intent.putExtra("user_email", email);
                startActivity(intent);
            }
        });

        feedingCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mainmenu.this, feeding.class);
                startActivity(intent);
            }
        });

        contactCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mainmenu.this, contact.class);
                startActivity(intent);
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutUser();
            }
        });
    }

    private void checkPetType(String userId) {
        DatabaseReference userRef = mDatabase.child("users").child(userId);
        String email = getIntent().getStringExtra("user_email");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                } else {
                    // Handle the case where the user data does not exist in the database
                    Toast.makeText(mainmenu.this, "Welcome new user !!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(mainmenu.this, PetOptionsActivity.class);
                    intent.putExtra("user_email", email);
                    startActivity(intent);
                    finish(); // Close this activity
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
                Toast.makeText(mainmenu.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(mainmenu.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close this activity
    }
}
