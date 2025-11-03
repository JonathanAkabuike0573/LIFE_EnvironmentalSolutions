package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// This is our concrete implementation that uses Firebase to get user data.
public class FirebaseUserDataProvider implements UserDataProvider {

    private final FirebaseAuth mAuth;
    private final DatabaseReference usersRef;

    public FirebaseUserDataProvider() {
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @Override
    public void fetchUserData(UserDataCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("No user is logged in.");
            return;
        }

        String uid = currentUser.getUid();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    HelperClass userProfile = snapshot.getValue(HelperClass.class);
                    if (userProfile != null) {
                        // Success: Pass the user's name back through the callback.
                        callback.onDataReceived(userProfile.getName());
                    } else {
                        callback.onError("User profile data is malformed.");
                    }
                } else {
                    callback.onError("User profile not found in database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failure: Pass the error message back through the callback.
                callback.onError(error.getMessage());
            }
        });
    }
}
