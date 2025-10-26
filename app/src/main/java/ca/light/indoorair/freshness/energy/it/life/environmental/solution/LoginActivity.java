package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    Button loginButton, googleSignInButton;
    private TextView signup;
    private MaterialCheckBox rememberMeCheckBox;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::handleGoogleSignInResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // If themes ever get mixed, uncomment the next line:
        // setTheme(R.style.Theme_LIFE_EnvironmentalSolution);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        bindViews();

        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure Google Sign-In. Check google-services.json and R.string.default_web_client_id.", e);
            Toast.makeText(this, "Google Sign-In is not available.", Toast.LENGTH_LONG).show();
            googleSignInButton.setVisibility(View.GONE);
        }

        wireClicks();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            navigateToMainActivity();
        }
    }

    private void handleGoogleSignInResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
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
                        navigateToMainActivity();
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication Failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindViews() {
        usernameEditText   = findViewById(R.id.username);
        passwordEditText   = findViewById(R.id.password);
        loginButton        = findViewById(R.id.btn_login);
        signup             = findViewById(R.id.tv_signup);
        rememberMeCheckBox = findViewById(R.id.remember_me);
        googleSignInButton = findViewById(R.id.btn_google);


        if (usernameEditText == null || passwordEditText == null ||
                loginButton == null || signup == null || rememberMeCheckBox == null ||
                googleSignInButton == null) {
            throw new IllegalStateException(
                    "login_page.xml is missing one or more required views: " +
                            "username, password, btn_login, tv_signup, remember_me, or googlebutton");
        }
    }

    private void wireClicks() {
        loginButton.setOnClickListener(v -> {
            if (!validateUsername()) {
                return;
            }
            if (!validatePassword()) {
                return;
            }
            checkUser();
        });

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

    private boolean validateUsername() {
        String val = usernameEditText.getText() == null ? "" : usernameEditText.getText().toString();
        if (val.isEmpty()) {
            usernameEditText.setError(getString(R.string.username_cannot_be_empty));
            return false;
        } else {
            usernameEditText.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String val = passwordEditText.getText() == null ? "" : passwordEditText.getText().toString();
        if (val.isEmpty()) {
            passwordEditText.setError(getString(R.string.password_cannot_be_empty));
            return false;
        } else {
            passwordEditText.setError(null);
            return true;
        }
    }

    private void checkUser() {
        String userUsername = String.valueOf(usernameEditText.getText()).trim();
        String userPassword = String.valueOf(passwordEditText.getText()).trim();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("username").equalTo(userUsername);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Loop through the results, even if we only expect one.
                    // This is safer than assuming the username is the key.
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String passwordFromDB = userSnapshot.child("password").getValue(String.class);

                        if (passwordFromDB != null && passwordFromDB.equals(userPassword)) {
                            usernameEditText.setError(null);
                            passwordEditText.setError(null);
                            navigateToMainActivity();
                            return; // Exit after finding the user
                        }
                    }
                    // If the loop completes, the password was incorrect.
                    passwordEditText.setError(getString(R.string.invalid_credentials));
                    passwordEditText.requestFocus();
                } else {
                    usernameEditText.setError(getString(R.string.user_does_not_exist));
                    usernameEditText.requestFocus();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "checkUser:onCancelled", error.toException());
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
