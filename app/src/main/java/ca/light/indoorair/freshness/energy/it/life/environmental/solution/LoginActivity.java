package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {


    EditText usernameEditText, passwordEditText;
    Button loginButton, googleSignInButton;
    TextView signup;
    CheckBox rememberMeCheckBox;

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private ActivityResultLauncher<IntentSenderRequest> oneTapLauncher;


    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.loginbutton);
        signup = findViewById(R.id.signup);
        googleSignInButton = findViewById(R.id.googlebutton);
        rememberMeCheckBox = findViewById(R.id.checkBox);

        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(getString(R.string.default_web_client_id))
                        // Show all accounts on the device.
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();


        oneTapLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                            String idToken = credential.getGoogleIdToken();
                            if (idToken != null) {
                                // Got an ID token from Google. Use it to authenticate
                                // with your backend.
                                Log.d(TAG, "Got ID token.");
                                Toast.makeText(this, "Google Sign In Successful.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } catch (ApiException e) {
                            Log.w(TAG, "Sign-in failed", e);
                            Toast.makeText(this, "Google Sign In Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        googleSignInButton.setOnClickListener(v -> oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        oneTapLauncher.launch(
                                new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build());
                    } catch (Exception e) {
                        Log.e(TAG, "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                    }
                })
                .addOnFailureListener(this, e -> {
                    // No Google Accounts found. Just continue presenting the signed-out UI.
                    Log.d(TAG, e.getLocalizedMessage());
                }));

        loginButton.setOnClickListener(v -> {
            if (!validateUsername() | !validatePassword()) {
                return;
            }
            checkUser();
        });


        signup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });


    }

    public Boolean validateUsername() {

        String val = usernameEditText.getText().toString();
        if (val.isEmpty()) {
            usernameEditText.setError(getString(R.string.username_cannot_be_empty));
            return false;
        } else {
            usernameEditText.setError(null);
            return true;
        }

    }

    public Boolean validatePassword() {
        String val = passwordEditText.getText().toString();
        if (val.isEmpty()) {
            passwordEditText.setError(getString(R.string.password_cannot_be_empty));
            return false;
        } else {
            passwordEditText.setError(null);
            return true;
        }

    }

    public void checkUser() {

        String userUsername = usernameEditText.getText().toString().trim();
        String userPassword = passwordEditText.getText().toString().trim();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("username").equalTo(userUsername);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {


                if (snapshot.exists()) {
                    usernameEditText.setError(null);
                    String passwordFromDB = snapshot.child(userUsername).child("password").getValue(String.class);

                    if (passwordFromDB != null && passwordFromDB.equals(userPassword)) {
                        passwordEditText.setError(null);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    } else {
                        passwordEditText.setError(getString(R.string.invalid_credentials));
                        passwordEditText.requestFocus();
                    }
                } else {
                    usernameEditText.setError(getString(R.string.user_does_not_exist));
                    usernameEditText.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }
}
