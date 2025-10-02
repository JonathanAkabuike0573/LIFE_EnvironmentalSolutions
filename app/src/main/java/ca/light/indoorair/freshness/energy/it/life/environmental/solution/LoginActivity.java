package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.os.Bundle;
import android.content.res.Configuration;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText usernameEditText, passwordEditText;
    Button loginButton;
    TextView registerTextView;
    CheckBox rememberMeCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.loginbutton);
        registerTextView = findViewById(R.id.signup);
        rememberMeCheckBox = findViewById(R.id.checkBox);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Re-set the layout when the orientation changes
        setContentView(R.layout.login_page);
    }
}
