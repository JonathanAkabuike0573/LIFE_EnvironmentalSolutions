package ca.light.indoorair.freshness.energy.it.life.environmental.solution;
public interface UserDataProvider {

    interface UserDataCallback {
        void onDataReceived(String userName);
        void onError(String errorMessage);
    }

    void fetchUserData(UserDataCallback callback);
}