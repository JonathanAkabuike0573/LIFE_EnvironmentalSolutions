package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

public class FakeUserDataProvider implements UserDataProvider {
    private final boolean shouldSucceed;
    private final String nameToReturn;


    public FakeUserDataProvider(boolean shouldSucceed, String nameToReturn) {
        this.shouldSucceed = shouldSucceed;
        this.nameToReturn = nameToReturn;
    }

    @Override
    public void fetchUserData(UserDataCallback callback) {
        if (shouldSucceed) {
            callback.onDataReceived(nameToReturn);
        } else {
            callback.onError("Failure in retrieving the  user data.");
        }
    } }