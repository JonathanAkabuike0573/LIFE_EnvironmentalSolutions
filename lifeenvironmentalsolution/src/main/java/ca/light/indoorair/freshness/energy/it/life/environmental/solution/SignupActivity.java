package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.auth.AuthProvider;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.auth.FirebaseAuthProvider;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.validation.InputValidation;

public class SignupActivity extends AppCompatActivity {

    private EditText name, phoneNumber, email, password, confirmPassword;
    private Button signup;
    private TextView alreadyHaveAccount;

    // The Activity only knows about the interface, not the implementation.
    private AuthProvider authProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);


        this.authProvider = new FirebaseAuthProvider(this);

        bindViews();
        wireClicks();
    }

    private void attemptSignUp() {
        String nameStr = name.getText().toString().trim();
        String phoneStr = phoneNumber.getText().toString().trim();
        String emailStr = email.getText().toString().trim();
        String passwordStr = password.getText().toString();
        String confirmPasswordStr = confirmPassword.getText().toString();

        if (!validateInputs(nameStr, phoneStr, emailStr, passwordStr, confirmPasswordStr)) {
            return; // Stop if validation fails
        }

        // Delegate the entire signup process to the provider
        authProvider.signUpWithEmail(emailStr, passwordStr, nameStr, phoneStr, new AuthProvider.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(SignupActivity.this, R.string.sign_up_successful, Toast.LENGTH_SHORT).show();
                // Send user to main activity, as they are now signed up and logged in.
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    //Helper methods for binding views, wiring clicks, and validation
    private void bindViews() {
        name = findViewById(R.id.signupFUllname);
        phoneNumber = findViewById(R.id.signUpPhoneNumber);
        email = findViewById(R.id.SignUpEmail);
        password = findViewById(R.id.signUpPassword);
        confirmPassword = findViewById(R.id.signUpConfirmPassword);
        signup = findViewById(R.id.buttonSignUp);
        alreadyHaveAccount = findViewById(R.id.signUploginredirect);
    }

    private void wireClicks() {
        signup.setOnClickListener(view -> attemptSignUp());
        alreadyHaveAccount.setOnClickListener(view -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private boolean validateInputs(String name, String phone, String email, String password, String confirmPassword) {
        if (!InputValidation.isValidName(name)) {
            this.name.setError(getString(R.string.please_enter_a_valid_name));
            return false;
        }
        if (!InputValidation.isValidPhone(phone)) {
            this.phoneNumber.setError(getString(R.string.please_enter_a_valid_phone_number));
            return false;
        }
        if (!InputValidation.isValidEmail(email)) {
            this.email.setError(getString(R.string.please_enter_a_valid_email_address));
            return false;
        }
        if (!InputValidation.isValidPassword(password)) {
            this.password.setError(getString(R.string.password_must_be_at_least_6_characters));
            return false;
        }
        if (!password.equals(confirmPassword)) {
            this.confirmPassword.setError(getString(R.string.passwords_do_not_match));
            return false;
        }
        return true;
    }
}
