package ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.DashboardRepository;

public class DashboardViewModel extends AndroidViewModel {

    private final DashboardRepository repository;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = new DashboardRepository();
    }

    public void init(String roomName) {
        repository.init(roomName);
    }


    public LiveData<Double> getTemperatureC() { return repository.getTemperatureC(); }
    public LiveData<Double> getTemperatureF() { return repository.getTemperatureF(); }
    public LiveData<String> getComfortLevel() { return repository.getComfortLevel(); }
    public LiveData<String> getAirQuality() { return repository.getAirQuality(); }
    public LiveData<Boolean> getSmartLightPower() { return repository.getSmartLightPower(); }

    public void toggleSmartLightPower(boolean newState) {
        repository.toggleSmartLightPower(newState);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.destroy();
    }
}
