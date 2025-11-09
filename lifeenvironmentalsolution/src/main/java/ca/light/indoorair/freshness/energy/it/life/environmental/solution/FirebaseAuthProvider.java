package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// This is our concrete implementation using Firebase.
public class FirebaseAuthProvider implements AuthProvider {

    private static final String TAG = "FirebaseAuthProvider";
    private final FirebaseAuth mAuth;
    private final GoogleSignInClient mGoogleSignInClient;
    private final Activity activity;

    public FirebaseAuthProvider(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        this.mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    @Override
    public void signInWithEmail(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        callback.onSuccess();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        callback.onFailure("Authentication failed. Please check your credentials.");
                    }
                });
    }

    @Override
    public void signInWithGoogle(ActivityResultLauncher<Intent> googleSignInLauncher) {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    @Override
    public void handleGoogleSignInResult(Intent data, AuthCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken(), callback);
            } else {
                callback.onFailure("Google account is null.");
            }
        } catch (ApiException e) {
            callback.onFailure("Google sign in failed: " + e.getMessage());
        }
    }

    private void firebaseAuthWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure("Firebase credential authentication failed.");
                    }
                });
    }

    @Override
    public void signOut(Runnable onComplete) {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(activity, task -> {
            onComplete.run();
        });
    }


    @Override
    public void signUpWithEmail(String email, String password, String name, String phone, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, authTask -> {
                    if (authTask.isSuccessful()) {
                        // Auth user created successfully. Now save the additional data.
                        String uid = mAuth.getCurrentUser().getUid();
                        DatabaseReference userNode = FirebaseDatabase.getInstance().getReference("users").child(uid);
                        HelperClass helperClass = new HelperClass(name, email, phone);

                        userNode.setValue(helperClass).addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                callback.onSuccess(); // Entire process was successful
                            } else {
                                // This is a rare but important case to handle
                                callback.onFailure("User created, but failed to save user data.");
                            }
                        });
                    } else {
                        // Auth creation failed (e.g., email already exists)
                        if (authTask.getException() instanceof FirebaseAuthUserCollisionException) {
                            callback.onFailure("An account with this email already exists.");
                        } else {
                            callback.onFailure(authTask.getException().getMessage());
                        }
                    }
                });
    }
}
