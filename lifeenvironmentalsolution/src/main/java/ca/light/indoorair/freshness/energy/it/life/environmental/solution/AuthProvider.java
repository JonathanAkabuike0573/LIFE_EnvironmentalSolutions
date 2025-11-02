package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;

// This is the contract for any class that can handle user authentication.
public interface AuthProvider {

    // A callback interface to handle the asynchronous results of authentication.
    interface AuthCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    // Method for standard email and password login.
    void signInWithEmail(String email, String password, AuthCallback callback);

    // Method to initiate the Google sign-in flow.
    void signInWithGoogle(ActivityResultLauncher<Intent> googleSignInLauncher);

    // Method to handle the result from the Google sign-in activity.
    void handleGoogleSignInResult(Intent data, AuthCallback callback);
        void signUpWithEmail(String email, String password, String name, String phone, AuthCallback callback);

    // Method to sign the user out.
    void signOut(Runnable onComplete);
}
