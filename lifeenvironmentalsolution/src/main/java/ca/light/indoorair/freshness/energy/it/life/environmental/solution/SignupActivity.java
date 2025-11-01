package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class SignupActivity extends AppCompatActivity {

    EditText name, phoneNumber, email, password, confirmPassword;
    Button signup;
    TextView alreadyHaveAccount;

    FirebaseDatabase database;
    DatabaseReference myRef;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        name = findViewById(R.id.signupFUllname);
        phoneNumber = findViewById(R.id.signUpPhoneNumber);
        email = findViewById(R.id.SignUpEmail);
        password = findViewById(R.id.signUpPassword);
        confirmPassword = findViewById(R.id.signUpConfirmPassword);
        signup = findViewById(R.id.buttonSignUp);
        alreadyHaveAccount = findViewById(R.id.signUploginredirect);

        signup.setOnClickListener(view -> {
            database = FirebaseDatabase.getInstance();
            myRef = database.getReference("users");

            // Use unique variable names to avoid shadowing
            String nameStr = name.getText().toString();
            String phoneStr = phoneNumber.getText().toString();
            String emailStr = email.getText().toString();
            String passwordStr = password.getText().toString();
            String confirmPasswordStr = confirmPassword.getText().toString();


            // --- Input Validation ---
            if (TextUtils.isEmpty(nameStr) || TextUtils.isEmpty(phoneStr) || TextUtils.isEmpty(emailStr) || TextUtils.isEmpty(passwordStr)) {
                Toast.makeText(SignupActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
                Toast.makeText(SignupActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!passwordStr.equals(confirmPasswordStr)) {
                Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if user with the same email already exists
            Query checkUser = myRef.orderByChild("email").equalTo(emailStr);

            checkUser.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // User with this email already exists
                        new AlertDialog.Builder(SignupActivity.this)
                                .setTitle("Email Already Registered")
                                .setIcon(R.drawable.logolife)
                                .setMessage("This email address is already in use. Please sign in.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    // User clicked OK, navigate to LoginActivity
                                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .show();
                    } else {
                        // Email is not registered, create new user
                        HelperClass helperClass = new HelperClass(nameStr, emailStr, phoneStr, passwordStr, confirmPasswordStr);
                        myRef.child(emailStr).setValue(helperClass);

                        Toast.makeText(SignupActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(SignupActivity.this, R.string.database_error_please_try_again_later, Toast.LENGTH_SHORT).show();
                }
            });
        });

        alreadyHaveAccount.setOnClickListener(view -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }


}
