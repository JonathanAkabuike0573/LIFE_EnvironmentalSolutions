package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    // Constants for SharedPreferences
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String PREF_EMAIL = "email";

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton, googleSignInButton;
    private TextView signup;
    private CheckBox rememberMeCheckBox;

    // The Activity only knows about the interface, not the implementation.
    private AuthProvider authProvider;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Delegate the result handling to the provider
                    authProvider.handleGoogleSignInResult(result.getData(), createAuthCallback());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        // Check if user is already signed in.
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            navigateToMainActivity();
            return; // Important: return to prevent rest of onCreate from running
        }

        bindViews();

        // METHOD INJECTION: We create the concrete provider and "inject" it here.
        // The rest of the class will only interact with the `authProvider` interface.
        this.authProvider = new FirebaseAuthProvider(this);

        wireClicks();
        loadCredentials();
    }

    private void bindViews() {
        emailEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.btn_login);
        signup = findViewById(R.id.tv_signup);
        rememberMeCheckBox = findViewById(R.id.remember_me);
        googleSignInButton = findViewById(R.id.btn_google);
    }

    private void wireClicks() {
        loginButton.setOnClickListener(v -> loginUser());

        signup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        googleSignInButton.setOnClickListener(v -> {
            // Delegate the Google Sign-In action to the provider.
            authProvider.signInWithGoogle(googleSignInLauncher);
        });
    }

    private void loginUser() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";

        if (!validateInputs(email, password)) {
            return; // Stop if validation fails
        }

        // Delegate the login action to the provider.
        authProvider.signInWithEmail(email, password, createAuthCallback());
    }

    // A helper method to create a reusable callback for handling auth results.
    private AuthProvider.AuthCallback createAuthCallback() {
        return new AuthProvider.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(LoginActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                if (rememberMeCheckBox.isChecked()) {
                    saveCredentials(emailEditText.getText().toString().trim());
                } else {
                    clearCredentials();
                }
                navigateToMainActivity();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        };
    }

    private boolean validateInputs(String email, String password) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password cannot be empty");
            passwordEditText.requestFocus();
            return false;
        }
        return true;
    }

    private void loadCredentials() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedEmail = prefs.getString(PREF_EMAIL, null);
        if (savedEmail != null) {
            emailEditText.setText(savedEmail);
            rememberMeCheckBox.setChecked(true);
        }
    }

    private void saveCredentials(String email) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_EMAIL, email).apply();
    }

    private void clearCredentials() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove(PREF_EMAIL).apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
