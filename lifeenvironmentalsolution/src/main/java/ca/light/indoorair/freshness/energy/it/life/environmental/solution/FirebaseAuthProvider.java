package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import androidx.credentials.CredentialManager;
import com.google.firebase.database.FirebaseDatabase;

/**
 * A concrete implementation of the {@link AuthProvider} interface that uses Firebase
 * Authentication and the Firebase Realtime Database with modern Google Sign-In.
 */
public class FirebaseAuthProvider implements AuthProvider {

    private static final String TAG = "FirebaseAuthProvider";

    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference databaseReference;
    private final Context context; // Should be an Activity context
    private final CredentialManager credentialManager;

    public FirebaseAuthProvider(Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.databaseReference = FirebaseDatabase.getInstance().getReference("users");
        this.context = context;
        this.credentialManager = CredentialManager.create(context);
    }

    @Override
    public void signInWithEmail(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Additional check to ensure user profile exists in Realtime Database
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            databaseReference.child(firebaseUser.getUid()).get().addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful() && dbTask.getResult().exists()) {
                                    callback.onSuccess();
                                } else {
                                    // Sign out the user if their DB entry is missing
                                    firebaseAuth.signOut();
                                    callback.onFailure("User profile not found. Please sign up again.");
                                }
                            });
                        } else {
                            callback.onFailure("Authentication succeeded but user is null.");
                        }
                    } else {
                        callback.onFailure("Authentication failed: " + task.getException().getMessage());
                    }
                });
    }

    @Override
    public void signUpWithEmail(String email, String password, String name, String phone, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            HelperClass newUser = new HelperClass(name, email, phone);
                            databaseReference.child(firebaseUser.getUid()).setValue(newUser)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            callback.onSuccess();
                                        } else {
                                            callback.onFailure("User created, but failed to save user data.");
                                        }
                                    });
                        }
                    } else {
                        if (authTask.getException() instanceof FirebaseAuthUserCollisionException) {
                            callback.onFailure("An account with this email already exists.");
                        } else {
                            callback.onFailure(authTask.getException().getMessage());
                        }
                    }
                });
    }

    @Override
    public void signInWithGoogle(AuthCallback callback) {
        // Ensure context is an Activity
        if (!(context instanceof Activity)) {
            callback.onFailure("Context must be an Activity for Google Sign-In");
            return;
        }

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();


        credentialManager.getCredentialAsync(
                (Activity) context,
                request,
                null,
                context.getMainExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof GoogleIdTokenCredential) {
                            GoogleIdTokenCredential googleIdTokenCredential = (GoogleIdTokenCredential) credential;
                            String idToken = googleIdTokenCredential.getIdToken();
                            firebaseAuthWithGoogle(idToken, callback);
                        } else {
                            callback.onFailure("Unsupported credential type received.");
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        callback.onFailure("Google Sign-In failed: " + e.getMessage());
                        Log.e(TAG, "Google Sign-In GetCredentialException", e);
                    }
                }
        );
    }

    private void firebaseAuthWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(authResultTask -> {
                    if (authResultTask.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            // If it's a new user, save their details to the database.
                            if (authResultTask.getResult().getAdditionalUserInfo().isNewUser()) {
                                String username = user.getDisplayName();
                                String email = user.getEmail();
                                HelperClass newUser = new HelperClass(username, email, "");
                                databaseReference.child(user.getUid()).setValue(newUser);
                            }
                            callback.onSuccess(); // Final success signal
                        }
                    } else {
                        callback.onFailure("Firebase authentication failed: " + authResultTask.getException().getMessage());
                    }
                });
    }

    @Override
    public void signOut(Runnable onComplete) {
        firebaseAuth.signOut();

        onComplete.run();
    }
}
