package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private TextView signup;
    private MaterialCheckBox rememberMeCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // If themes ever get mixed, uncomment the next line:
        // setTheme(R.style.Theme_LIFE_EnvironmentalSolution);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        bindViews();
        wireClicks();
    }

    private void bindViews() {
        usernameEditText   = findViewById(R.id.username);
        passwordEditText   = findViewById(R.id.password);
        loginButton        = findViewById(R.id.btn_login);
        signup             = findViewById(R.id.tv_signup);
        rememberMeCheckBox = findViewById(R.id.remember_me);

        if (usernameEditText == null || passwordEditText == null ||
                loginButton == null || signup == null || rememberMeCheckBox == null) {
            throw new IllegalStateException(
                    "login_page.xml is missing one or more required views: " +
                            "username, password, btn_login, tv_signup, remember_me");
        }
    }

    private void wireClicks() {
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
        Query checkUserDatabase = reference.orderByChild(getString(R.string.username)).equalTo(userUsername);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    usernameEditText.setError(null);

                    // If your user nodes are keyed by username, this is fine.
                    // If they are keyed by UID, you should iterate snapshot.getChildren().
                    String passwordFromDB = snapshot.child(userUsername).child("password").getValue(String.class);

                    if (passwordFromDB != null && passwordFromDB.equals(userPassword)) {
                        passwordEditText.setError(null);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        passwordEditText.setError(getString(R.string.invalid_credentials));
                        passwordEditText.requestFocus();
                    }
                } else {
                    usernameEditText.setError(getString(R.string.user_does_not_exist));
                    usernameEditText.requestFocus();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                // Optionally show a toast/log here
            }
        });
    }


}
