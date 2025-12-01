package ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.DashboardRepository;

public class DashboardViewModel extends AndroidViewModel {

    private final DashboardRepository repository;
    private final SharedPreferences prefs;


    private final List<String> premiumDevices = Arrays.asList("Air Conditioner", "Thermostat", "Smart TV");

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = new DashboardRepository();
        prefs = PreferenceManager.getDefaultSharedPreferences(application);
    }

    public void init(String roomName) {
        repository.init(roomName);
    }


    public boolean isDeviceUnlocked(String deviceName) {

        if (!premiumDevices.contains(deviceName)) {
            return true;
        }


        boolean hasPaid = prefs.getBoolean("has_paid_subscription", false);
        Set<String> allowedDevices = prefs.getStringSet("allowed_devices", new HashSet<>());

        return hasPaid && allowedDevices.contains(deviceName);
    }

    public LiveData<Double> getTemperatureC() { return repository.getTemperatureC(); }
    public LiveData<Double> getTemperatureF() { return repository.getTemperatureF(); }
    public LiveData<String> getComfortLevel() { return repository.getComfortLevel(); }
    public LiveData<String> getAirQuality() { return repository.getAirQuality(); }
    public LiveData<Boolean> getSmartLightPower() { return repository.getSmartLightPower(); }

    public void toggleSmartLightPower(boolean newState) {

        if (isDeviceUnlocked("Smart Light")) {
            repository.toggleSmartLightPower(newState);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.destroy();
    }
}
