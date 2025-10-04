package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.os.Bundle;
import android.content.res.Configuration;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {


    EditText usernameEditText, passwordEditText;
    Button loginButton;
    TextView signup;
    CheckBox rememberMeCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.loginbutton);
        signup = findViewById(R.id.signup);
        rememberMeCheckBox = findViewById(R.id.checkBox);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateUsername() | !validatePassword()) {
                    return;
                } else {
                    checkUser();
                }
            }});





        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
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

    public void checkUser(){

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

            }
        });
    }





    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Re-set the layout when the orientation changes
        setContentView(R.layout.login_page);
    }


}
