package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {


    private EditText name, phoneNumber, email, password, confirmPassword;
    private Button signup;
    private TextView alreadyHaveAccount;


    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);


        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");


        name = findViewById(R.id.signupFUllname);
        phoneNumber = findViewById(R.id.signUpPhoneNumber);
        email = findViewById(R.id.SignUpEmail);
        password = findViewById(R.id.signUpPassword);
        confirmPassword = findViewById(R.id.signUpConfirmPassword);
        signup = findViewById(R.id.buttonSignUp);
        alreadyHaveAccount = findViewById(R.id.signUploginredirect);

        //signup button click
        signup.setOnClickListener(view -> attemptSignUpWithFirebaseAuth());

        // redirection to login page
        alreadyHaveAccount.setOnClickListener(view -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Handles the entire sign-up process using Firebase Authentication.
     */
    private void attemptSignUpWithFirebaseAuth() {
        // 1. Get user input as strings
        String nameStr = name.getText().toString().trim();
        String phoneStr = phoneNumber.getText().toString().trim();
        String emailStr = email.getText().toString().trim();
        String passwordStr = password.getText().toString();
        String confirmPasswordStr = confirmPassword.getText().toString();

        //  Validate all the inputs
        if (!validateInputs(nameStr, phoneStr, emailStr, passwordStr, confirmPasswordStr)) {
            return; // Stop if validation fails
        }

        //  Using Firebase Auth to create the user account
        mAuth.createUserWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(this, authTask -> {
                    if (authTask.isSuccessful()) {
                        // SUCCESS: Firebase Authentication created the user, get the unique user ID (UID) for this new user.
                        String uid = mAuth.getCurrentUser().getUid();

                        // 4. Saving the *other* user information to the Realtime Database using the UID as the key.

                        saveAdditionalUserData(uid, nameStr, emailStr, phoneStr);

                    } else {

                        // Check if the reason is that the email is already in use.
                        if (authTask.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(SignupActivity.this, "An account with this email already exists.", Toast.LENGTH_LONG).show();
                        } else {
                            // Another error occurred (e.g., weak password, network issue).
                            Toast.makeText(SignupActivity.this, "Sign up failed: " + authTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Saves additional user data to the Realtime Database after successful auth creation.
     */
    private void saveAdditionalUserData(String uid, String name, String email, String phone) {
        // HelperClass constructor is now updated to not take a password.
        HelperClass helperClass = new HelperClass(name, email, phone);

        usersRef.child(uid).setValue(helperClass).addOnCompleteListener(dbTask -> {
            if (dbTask.isSuccessful()) {
                // All data saved successfully.
                Toast.makeText(SignupActivity.this, "Sign Up Successful!", Toast.LENGTH_SHORT).show();

                // Send user directly to the main activity, as they are now logged in.
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                // Clear the activity stack to prevent user from going back to login/signup pages
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

            } else {
                // Handle case where the database write fails.
                Toast.makeText(SignupActivity.this, "Failed to save user data. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputs(String name, String phone, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            this.password.setError("Password must be at least 6 characters");
            this.password.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            this.email.setError("Please enter a valid email address");
            this.email.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            this.confirmPassword.setError("Passwords do not match");
            this.confirmPassword.requestFocus();
            return false;
        }
        this.email.setError(null);
        this.confirmPassword.setError(null);
        this.password.setError(null);
        return true;
    }
}
