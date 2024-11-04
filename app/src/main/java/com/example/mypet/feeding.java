package com.example.mypet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class feeding extends AppCompatActivity {

    private ArrayList<Feeding> feedingList = new ArrayList<>();
    private GridLayout feedingGrid;
    private TextView noFeedingText;
    private static final int MAX_FEEDINGS = 8;

    // Firebase database reference
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feeding);

        // Initialize Firebase Auth and get current user
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Initialize Firebase Database with user-specific node
            databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUser.getUid()).child("feedings");
        } else {
            Toast.makeText(feeding.this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Finish the current activity to go back
            }
        });

        Button voiceButton = findViewById(R.id.voice_button);
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start the feeding activity
                Intent intent = new Intent(feeding.this, voice.class);
                startActivity(intent);
            }
        });

        feedingGrid = findViewById(R.id.feeding_grid);
        noFeedingText = findViewById(R.id.no_feeding_text);
        Button addFeedingButton = findViewById(R.id.add_feeding_button);

        addFeedingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (feedingList.size() < MAX_FEEDINGS) {
                    showFeedingDialog(null, -1);
                } else {
                    Toast.makeText(feeding.this, "You can only create up to 8 schedules", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Fetch data from Firebase Database
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                feedingList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Feeding feeding = dataSnapshot.getValue(Feeding.class);
                    feedingList.add(feeding);
                }
                updateFeedingGrid();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(feeding.this, "Failed to load feedings", Toast.LENGTH_SHORT).show();
            }
        });

        updateFeedingGrid();
    }

    private void updateFeedingGrid() {
        feedingGrid.removeAllViews();
        if (feedingList.isEmpty()) {
            noFeedingText.setVisibility(View.VISIBLE);
        } else {
            noFeedingText.setVisibility(View.GONE);
            for (int i = 0; i < feedingList.size(); i++) {
                addFeedingCard(feedingList.get(i), i);
            }
        }
    }

    private void addFeedingCard(final Feeding feeding, final int index) {
        CardView cardView = (CardView) LayoutInflater.from(this).inflate(R.layout.activity_feeding_card, feedingGrid, false);

        TextView nameTextView = cardView.findViewById(R.id.feeding_name);
        TextView foodTextView = cardView.findViewById(R.id.feeding_food);
        TextView amountTextView = cardView.findViewById(R.id.feeding_amount);
        ImageButton editButton = cardView.findViewById(R.id.edit_feeding_button);
        ImageButton deleteButton = cardView.findViewById(R.id.delete_feeding_button);

        nameTextView.setText(feeding.getName());
        foodTextView.setText(feeding.getFood());
        amountTextView.setText(feeding.getAmount());

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFeedingDialog(feeding, index);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                databaseReference.child(feeding.getName()).removeValue();
                feedingList.remove(index);
                updateFeedingGrid();
            }
        });

        feedingGrid.addView(cardView);
    }

    private void showFeedingDialog(final Feeding feeding, final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.activity_dialog_add_feeding, null);
        builder.setView(dialogView);

        final EditText nameEditText = dialogView.findViewById(R.id.edit_feeding_name);
        final EditText foodEditText = dialogView.findViewById(R.id.edit_feeding_food);
        final EditText amountEditText = dialogView.findViewById(R.id.edit_feeding_amount);
        Button saveButton = dialogView.findViewById(R.id.save_feeding_button);

        if (feeding != null) {
            nameEditText.setText(feeding.getName());
            foodEditText.setText(feeding.getFood());
            amountEditText.setText(feeding.getAmount());
        }

        final AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEditText.getText().toString();
                String food = foodEditText.getText().toString();
                String amount = amountEditText.getText().toString();

                if (name.isEmpty() || food.isEmpty() || amount.isEmpty()) {
                    Toast.makeText(feeding.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    Feeding newFeeding = new Feeding(name, food, amount);
                    if (feeding == null) {
                        databaseReference.child(name).setValue(newFeeding);
                        feedingList.add(newFeeding);
                    } else {
                        databaseReference.child(feeding.getName()).removeValue();
                        databaseReference.child(name).setValue(newFeeding);
                        feedingList.set(index, newFeeding);
                    }
                    updateFeedingGrid();
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    public static class Feeding {
        private String name;
        private String food;
        private String amount;

        public Feeding() {}

        public Feeding(String name, String food, String amount) {
            this.name = name;
            this.food = food;
            this.amount = amount;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFood() {
            return food;
        }

        public void setFood(String food) {
            this.food = food;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }
    }
}
