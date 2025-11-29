package ca.light.indoorair.freshness.energy.it.life.environmental.solution.auth;

/**
 * Defines the contract for authentication operations, abstracting the underlying implementation.
 */
public interface AuthProvider {

    interface AuthCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    void signInWithEmail(String email, String password, AuthCallback callback);

    void signUpWithEmail(String email, String password, String name, String phone, AuthCallback callback);

    /**
     * Initiates the Google Sign-In flow.
     * The provider will handle the entire asynchronous flow, including any necessary UI,
     * and will report the final result via the provided callback.
     * @param callback The callback to invoke with the final success or failure result.
     */
    void signInWithGoogle(AuthCallback callback);

    void signOut(Runnable onComplete);
}
