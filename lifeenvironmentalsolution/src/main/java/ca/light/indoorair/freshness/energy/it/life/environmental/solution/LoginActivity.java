package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.auth.AuthProvider;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.auth.FirebaseAuthProvider;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.validation.InputValidation;

// No longer importing com.google.firebase.auth.FirebaseAuthProvider
// No longer importing static androidx.core.app.PendingIntentCompat.getActivity
// testing out new branch

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "LoginPrefs";
    private static final String PREF_EMAIL = "email";

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton, googleSignInButton;
    private TextView signup;
    private CheckBox rememberMeCheckBox;

    private AuthProvider authProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            navigateToMainActivity();
            return;
        }

        setContentView(R.layout.login_page);
        bindViews();

        this.authProvider = new FirebaseAuthProvider(this);
        wireClickListeners();
        loadSavedCredentials();
    }

    private void bindViews() {
        emailEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.btn_login);
        signup = findViewById(R.id.tv_signup);
        rememberMeCheckBox = findViewById(R.id.remember_me);
        googleSignInButton = findViewById(R.id.btn_google);
    }

    private void wireClickListeners() {
        loginButton.setOnClickListener(v -> performEmailLogin());
        signup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));

        googleSignInButton.setOnClickListener(v -> {
            authProvider.signInWithGoogle(createAuthCallback());
        });
    }

    private void performEmailLogin() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";

        if (!validateInputs(email, password)) return;
        authProvider.signInWithEmail(email, password, createAuthCallback());
    }

    private AuthProvider.AuthCallback createAuthCallback() {
        return new AuthProvider.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(LoginActivity.this, R.string.login_successful , Toast.LENGTH_SHORT).show();
                if (rememberMeCheckBox.isChecked()) {
                    String emailToSave = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
                    if (!emailToSave.isEmpty()) {
                        saveCredentials(emailToSave);
                    }
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
        if (!InputValidation.isValidEmail(email)) {
            emailEditText.setError(getString(R.string.please_enter_a_valid_email));
            emailEditText.requestFocus();
            return false;
        }
        if (!InputValidation.isValidPassword(password)) {
            passwordEditText.setError(getString(R.string.password_cannot_be_empty));
            passwordEditText.requestFocus();
            return false;
        }
        return true;
    }

    private void loadSavedCredentials() {
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
