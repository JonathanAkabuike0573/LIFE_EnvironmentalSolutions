package ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.LightRepository;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class LightViewModel extends AndroidViewModel {

    private String currentRoom;
    private final LightRepository lightRepository;
    private final SharedPreferences sharedPreferences;

    // LiveData
    private final MutableLiveData<Integer> _lux = new MutableLiveData<>(0);
    public final LiveData<Integer> lux = _lux;

    private final MutableLiveData<String> _lightLevelText = new MutableLiveData<>("Dim");
    public final LiveData<String> lightLevelText = _lightLevelText;

    private final MutableLiveData<String> _lastUpdatedTime = new MutableLiveData<>("--:--");
    public final LiveData<String> lastUpdatedTime = _lastUpdatedTime;

    private final MutableLiveData<Integer> _statusColor = new MutableLiveData<>(R.drawable.circle_indicator_yellow);
    public final LiveData<Integer> statusColor = _statusColor;

    private final MutableLiveData<Integer> _cardGlowColor = new MutableLiveData<>(R.color.card_glow_neutral);
    public final LiveData<Integer> cardGlowColor = _cardGlowColor;

    private final MutableLiveData<String> _brightness = new MutableLiveData<>("Neutral");
    public final LiveData<String> brightness = _brightness;

    private final MutableLiveData<Boolean> _autoBrightness = new MutableLiveData<>(true);
    public final LiveData<Boolean> autoBrightness = _autoBrightness;

    // SLIDER SYNC
    private final MutableLiveData<Float> _sliderPosition = new MutableLiveData<>(50f);
    public final LiveData<Float> sliderPosition = _sliderPosition;

    // Thresholds
    private static final int LUX_DIM_THRESHOLD = 200;
    private static final int LUX_NORMAL_THRESHOLD = 1000;

    public LightViewModel(@NonNull Application application) {
        super(application);
        lightRepository = new LightRepository();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
    }

    public void init(String roomName) {
        this.currentRoom = roomName;
        startListening();
        loadSettings();
    }

    private void startListening() {
        lightRepository.startListening(currentRoom, new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer luxValue = dataSnapshot.child("lux").getValue(Integer.class);
                    String brightnessValue = dataSnapshot.child("brightness").getValue(String.class);
                    Boolean autoBrightnessValue = dataSnapshot.child("autoBrightness").getValue(Boolean.class);

                    if (luxValue != null) {
                        _lux.setValue(luxValue);
                        updateLightLevelUI(luxValue);
                    }

                    if (brightnessValue != null) {
                        _brightness.setValue(brightnessValue);
                        _cardGlowColor.setValue(getCardGlowColor(brightnessValue));
                    }

                    if (autoBrightnessValue != null) {
                        _autoBrightness.setValue(autoBrightnessValue);
                        sharedPreferences.edit()
                                .putBoolean("auto_brightness_enabled", autoBrightnessValue)
                                .apply();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void setBrightness(String brightness) {
        lightRepository.setBrightness(brightness);
    }

    public void setAutoBrightness(boolean enabled) {
        lightRepository.setAutoBrightness(enabled);
    }

    public void setSliderBrightness(int sliderValue) {
        lightRepository.setSliderBrightness(sliderValue);
        _sliderPosition.setValue((float) sliderValue);  // SYNC SLIDER UI
    }

    // PERFECT PRESETS
    public void setPreset(String preset) {
        switch (preset.toLowerCase()) {
            case "focus":
                setBrightness("Neutral");
                setSliderBrightness(80);      // Slider → 80%
                setAutoBrightness(false);
                break;
            case "relax":
                setBrightness("Cool");
                setSliderBrightness(40);      // Slider → 40%
                setAutoBrightness(false);
                break;
        }
    }

    private void updateLightLevelUI(int lux) {
        String levelText;
        int colorRes;

        if (lux < LUX_DIM_THRESHOLD) {
            levelText = "Dim";
            colorRes = R.drawable.circle_indicator_yellow;
        } else if (lux < LUX_NORMAL_THRESHOLD) {
            levelText = "Normal";
            colorRes = R.drawable.circle_indicator_green;
        } else {
            levelText = "Bright";
            colorRes = R.drawable.circle_indicator_red;
        }

        _lightLevelText.setValue(levelText);
        _statusColor.setValue(colorRes);
        _lastUpdatedTime.setValue(new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date()));
    }

    private int getCardGlowColor(String brightness) {
        switch (brightness != null ? brightness.toLowerCase() : "neutral") {
            case "warm": return R.color.card_glow_warm;
            case "cool": return R.color.card_glow_cool;
            default: return R.color.card_glow_neutral;
        }
    }

    private void loadSettings() {
        boolean autoEnabled = sharedPreferences.getBoolean("auto_brightness_enabled", true);
        _autoBrightness.setValue(autoEnabled);
        _sliderPosition.setValue(50f);  // Default slider position
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        lightRepository.stopListening();
    }
}
