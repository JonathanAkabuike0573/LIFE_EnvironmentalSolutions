package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    EditText name, username, email, password;
    Button signup;
    FirebaseDatabase database;
    DatabaseReference myRef;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        name = findViewById(R.id.editTextName);
        username = findViewById(R.id.editTextUsername);
        email = findViewById(R.id.editTextEmail);
        password = findViewById(R.id.editTextPassword);
        signup = findViewById(R.id.buttonSignUp);

        signup.setOnClickListener(view -> {


            database = FirebaseDatabase.getInstance();
            myRef = database.getReference(getString(R.string.users));
            String name1 = name.getText().toString();
            String username1 = username.getText().toString();
            String email1 = email.getText().toString();
            String password1 = password.getText().toString();

            HelperClass helperClass = new HelperClass(name1, email1, username1, password1);
            myRef.child(username1).setValue(helperClass);

            Toast.makeText(SignupActivity.this, R.string.user_has_been_registered_successfully, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });


    }

}
