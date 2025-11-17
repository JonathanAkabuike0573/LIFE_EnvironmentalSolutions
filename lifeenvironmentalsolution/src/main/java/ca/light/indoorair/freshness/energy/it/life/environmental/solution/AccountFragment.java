package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.app.Activity;
import android.content.Intent;
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
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
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

public class AccountFragment extends Fragment {

    private TextInputEditText fullNameEditText, emailEditText, phoneNumberEditText;
    private Button saveChangesButton;
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
                    // Here you would normally upload the image to Firebase Storage and save the URL in the database
                    Toast.makeText(getContext(), "Profile photo updated. Save changes to make it permanent.", Toast.LENGTH_LONG).show();
                    saveChangesButton.setEnabled(true);
                }
            }
    );


    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_management, container, false);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
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
        }

        changePhotoText.setOnClickListener(v -> openImagePicker());
        logo.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void initializeViews(View view) {
        fullNameEditText = view.findViewById(R.id.full_name_edit_text);
        emailEditText = view.findViewById(R.id.email_edit_text);
        phoneNumberEditText = view.findViewById(R.id.phone_number_edit_text);
        saveChangesButton = view.findViewById(R.id.save_changes_button);
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
            emailStatus.setText("Verified");
            emailStatus.setBackgroundResource(R.drawable.verified_background); // Create a green background
        } else {
            emailStatus.setText("Unverified");
            emailStatus.setBackgroundResource(R.drawable.unverified_background); // Create an orange background
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
}
