package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Constants for SharedPreferences
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String PREF_EMAIL = "email";
    // We will no longer save the password
    // private static final String PREF_PASSWORD = "password";

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton, googleSignInButton;
    private TextView signup;
    private CheckBox rememberMeCheckBox;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::handleGoogleSignInResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        bindViews();

        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        configureGoogleSignIn();

        wireClicks();
        loadCredentials();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and navigate to MainActivity.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            navigateToMainActivity();
        }
    }

    private void configureGoogleSignIn() {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure Google Sign-In. Check google-services.json and R.string.default_web_client_id.", e);
            Toast.makeText(this, "Google Sign-In is not available.", Toast.LENGTH_LONG).show();
            if (googleSignInButton != null) {
                googleSignInButton.setVisibility(View.GONE);
            }
        }
    }


    private void handleGoogleSignInResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(this, "Google Sign-In Successful.", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindViews() {
        emailEditText   = findViewById(R.id.username);
        passwordEditText   = findViewById(R.id.password);
        loginButton        = findViewById(R.id.btn_login);
        signup             = findViewById(R.id.tv_signup);
        rememberMeCheckBox = findViewById(R.id.remember_me);
        googleSignInButton = findViewById(R.id.btn_google);

        if (emailEditText == null || passwordEditText == null ||
                loginButton == null || signup == null || rememberMeCheckBox == null ||
                googleSignInButton == null) {
            // Updated the error message to be more robust
            throw new IllegalStateException("A required view is missing in login_page.xml. Please check all IDs.");
        }
    }

    private void wireClicks() {
        loginButton.setOnClickListener(v -> loginUser());

        signup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        googleSignInButton.setOnClickListener(v -> {
            if (mGoogleSignInClient == null) {
                Toast.makeText(this, "Google Sign-In is not configured.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    /**
     * Securely logs in the user using Firebase Authentication.
     */
    private void loginUser() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";

        // --- Input Validation ---
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password cannot be empty");
            passwordEditText.requestFocus();
            return;
        }

        // --- Firebase Authentication Sign-In ---
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithEmail:success");
                        Toast.makeText(LoginActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();

                        // Handle "Remember Me"
                        if (rememberMeCheckBox.isChecked()) {
                            saveCredentials(email);
                        } else {
                            clearCredentials();
                        }

                        navigateToMainActivity();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed. Please check your credentials.",
                                Toast.LENGTH_LONG).show();
                    }
                });
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
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(PREF_EMAIL, email);
        // DO NOT save the password
        editor.apply();
    }

    private void clearCredentials() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.remove(PREF_EMAIL);
        editor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Clear the back stack so the user cannot go back to the login screen
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
