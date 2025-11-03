package ca.light.indoorair.freshness.energy.it.life.environmental.solution;


public interface UserDataProvider {
    // callback interface to handle the asynchronous nature of fetching data.
    interface UserDataCallback {
        void onDataReceived(String userName);
        void onError(String errorMessage);
    }

    // This is the method that our fragment will call.
    // It takes a callback to notify the caller when data is ready.
    void fetchUserData(UserDataCallback callback);
}
