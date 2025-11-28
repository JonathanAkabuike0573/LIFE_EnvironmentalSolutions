package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class AccountFragment extends Fragment {

    private TextInputEditText fullNameEditText, emailEditText, phoneNumberEditText;
    private Button saveChangesButton, changePasswordButton;
    private TextView emailStatus;
    private ImageView logo;
    private TextView changePhotoText;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    logo.setImageURI(imageUri);
                    Toast.makeText(getContext(), "Profile photo updated. Save changes to make it permanent.", Toast.LENGTH_LONG).show();
                    saveChangesButton.setEnabled(true);
                }
            }
    );

    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        logo.setImageBitmap((android.graphics.Bitmap) extras.get("data"));
                        Toast.makeText(getContext(), "Profile photo updated. Save changes to make it permanent.", Toast.LENGTH_LONG).show();
                        saveChangesButton.setEnabled(true);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(getContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
            }
    );

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            loadUserProfile();
            setupTextWatchers();
            saveChangesButton.setOnClickListener(v -> saveUserProfile());
            changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        }

        changePhotoText.setOnClickListener(v -> showPhotoSourceDialog());
        logo.setOnClickListener(v -> showPhotoSourceDialog());
    }

    private void showPhotoSourceDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Change Profile Photo")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureLauncher.launch(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void initializeViews(View view) {
        fullNameEditText = view.findViewById(R.id.full_name_edit_text);
        emailEditText = view.findViewById(R.id.email_edit_text);
        phoneNumberEditText = view.findViewById(R.id.phone_number_edit_text);
        saveChangesButton = view.findViewById(R.id.save_changes_button);
        changePasswordButton = view.findViewById(R.id.change_password_button);
        emailStatus = view.findViewById(R.id.email_status);
        logo = view.findViewById(R.id.logo);
        changePhotoText = view.findViewById(R.id.change_photo_text);
    }

    private void loadUserProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);

                    fullNameEditText.setText(name);
                    emailEditText.setText(email);
                    phoneNumberEditText.setText(phone);

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        updateEmailVerificationStatus(user.isEmailVerified());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmailVerificationStatus(boolean isVerified) {
        if (isVerified) {
            emailStatus.setText(R.string.verified);
            emailStatus.setBackgroundResource(R.drawable.verified_background);
        } else {
            emailStatus.setText(R.string.unverified);
            emailStatus.setBackgroundResource(R.drawable.unverified_background);
        }
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveChangesButton.setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        fullNameEditText.addTextChangedListener(textWatcher);
        emailEditText.addTextChangedListener(textWatcher);
        phoneNumberEditText.addTextChangedListener(textWatcher);
    }

    private void saveUserProfile() {
        String name = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneNumberEditText.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone);

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    saveChangesButton.setEnabled(false);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Password");

        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, (ViewGroup) getView(), false);
        final EditText newPasswordEditText = viewInflated.findViewById(R.id.new_password_edit_text);
        final EditText confirmPasswordEditText = viewInflated.findViewById(R.id.confirm_password_edit_text);

        builder.setView(viewInflated);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (newPassword.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(newPassword);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void changePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
