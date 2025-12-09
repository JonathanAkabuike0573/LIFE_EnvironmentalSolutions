package ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.AirQualityRepository;

public class SharedRoomViewModel extends ViewModel {
    private MutableLiveData<String> currentRoom = new MutableLiveData<>("Main Office");
    private AirQualityRepository airQualityRepository;

    public void setAirQualityRepository(AirQualityRepository repository) {
        this.airQualityRepository = repository;
    }

    public LiveData<String> getCurrentRoom() { return currentRoom; }

    public void setCurrentRoom(String room) {
        currentRoom.setValue(room);

        // Update global monitoring to track the new room
        if (airQualityRepository != null) {
            airQualityRepository.updateGlobalMonitoredRoom(room);
        }
    }
}