package com.example.mypet;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class PetProfileActivity extends AppCompatActivity {

    private EditText petNameEditText;
    private EditText petTypeEditText;
    private EditText petAgeEditText;
    private Button buttonSave;
    private Button buttonBack;
    private ImageView petImageView;
    private Button buttonCamera;
    private Button buttonDeleteAccount;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextView textViewEmail;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;

    private Uri imageUri;
    private StorageReference mStorageRef;
    private boolean imageExistsInDatabase = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.petprofile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        buttonBack = findViewById(R.id.button);
        buttonCamera = findViewById(R.id.button2);
        petNameEditText = findViewById(R.id.petName);
        petTypeEditText = findViewById(R.id.petType);
        petAgeEditText = findViewById(R.id.petAge);
        buttonSave = findViewById(R.id.button1);
        buttonDeleteAccount = findViewById(R.id.button3);
        textViewEmail = findViewById(R.id.textView1);
        petImageView = findViewById(R.id.imageView);

        String email = getIntent().getStringExtra("user_email");
        textViewEmail.setText("" + email);

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PetProfileActivity.this, mainmenu.class);
                intent.putExtra("user_email", email);
                startActivity(intent);
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePetDetails(email);
            }
        });

        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if the app has permission to access the camera
                if (ContextCompat.checkSelfPermission(PetProfileActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request camera permission if not granted
                    ActivityCompat.requestPermissions(PetProfileActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                } else {
                    // If permission is granted, open the camera
                    openCamera();
                }
            }
        });

        petImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        buttonDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteAccountDialog();
            }
        });

        // Retrieve and display pet details if available
        retrieveAndDisplayPetDetails();
    }

    private void savePetDetails(String userEmail) {
        String petName = petNameEditText.getText().toString().trim();
        String petType = petTypeEditText.getText().toString().trim();
        String petAge = petAgeEditText.getText().toString().trim();

        if (petName.isEmpty() || petType.isEmpty() || petAge.isEmpty()) {
            Toast.makeText(this, "Pet name, type, and age are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null && !imageExistsInDatabase) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = mDatabase.child("users").child(userId);
            userRef.child("petName").setValue(petName);
            userRef.child("petType").setValue(petType);
            userRef.child("petAge").setValue(petAge);

            if (imageUri != null) {
                // Upload image to Firebase Storage
                StorageReference fileReference = mStorageRef.child("pets/" + userId + "/profile.jpg");
                fileReference.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            // Image uploaded successfully, get download URL
                            fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Save image URL to database
                                String imageUrl = uri.toString();
                                userRef.child("petImage").setValue(imageUrl);
                                Toast.makeText(PetProfileActivity.this, "Pet details saved successfully", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .addOnFailureListener(e -> {
                            // Error uploading image
                            Toast.makeText(PetProfileActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(PetProfileActivity.this, "Pet details saved successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void retrieveAndDisplayPetDetails() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = mDatabase.child("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String petName = dataSnapshot.child("petName").getValue(String.class);
                        String petType = dataSnapshot.child("petType").getValue(String.class);
                        String petAge = dataSnapshot.child("petAge").getValue(String.class);
                        String petImageUrl = dataSnapshot.child("petImage").getValue(String.class);
                        if (petName != null) {
                            petNameEditText.setText(petName);
                        }
                        if (petType != null) {
                            petTypeEditText.setText(petType);
                        }
                        if (petAge != null) {
                            petAgeEditText.setText(petAge);
                        }
                        if (petImageUrl != null) {
                            imageExistsInDatabase = true;
                            Picasso.get().load(petImageUrl).into(petImageView);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                petImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {
            // Get the captured image
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            // Convert bitmap to URI
            imageUri = getImageUri(imageBitmap);

            // Set image to ImageView
            petImageView.setImageBitmap(imageBitmap);
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAccount();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(PetProfileActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(PetProfileActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                Toast.makeText(PetProfileActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}
