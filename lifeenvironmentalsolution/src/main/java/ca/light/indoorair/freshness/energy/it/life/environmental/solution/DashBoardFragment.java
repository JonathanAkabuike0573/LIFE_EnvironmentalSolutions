package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.FeedBackPage;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.LightViewModel;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.SharedRoomViewModel;

public class DashBoardFragment extends Fragment {

    public Object dataProvider;
    // UI Elements
    private TextView userGreeting, temperatureText, comfortLevelText;
    private ImageView settingsIcon;
    private Spinner roomSpinner;


    private FirebaseAuth mAuth;
    private DatabaseReference roomsRef;
    private ValueEventListener roomsListener;


    private DatabaseReference selectedRoomRef;
    private ValueEventListener selectedRoomListener;

    private LightViewModel lightViewModel;
    private DatabaseReference airQualityRef;
    private ValueEventListener airQualityListener;


    private DatabaseReference lightRef;
    private ValueEventListener lightListener;

    private String currentSelectedRoom;

    // SharedPreferences & Adapter
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private MyAdapter adapter;
    private List<item> allItems;
    private List<item> visibleItems;
    private List<String> roomNames;
    private ArrayAdapter<String> roomAdapter;

    public DashBoardFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dash_board, container, false);


        allItems = new ArrayList<>();
        allItems.add(new item("Air Quality", "Good", R.drawable.air_qualityicon, true, false));
        allItems.add(new item("Smart Light", "Off", R.drawable.lightofficon, false, true));
        allItems.add(new item("Thermostat", "22°C", R.drawable.thermostaticon, true, false));
        allItems.add(new item("Air Conditioner", "Off", R.drawable.airconditionericon, false, true));
        allItems.add(new item("Presence Sensor", "Off", R.drawable.sensor_occupied, false, true));
        allItems.add(new item("Smart TV", "Off", R.drawable.tv_officon, false, true));

        visibleItems = new ArrayList<>();
        roomNames = new ArrayList<>();


        roomSpinner = view.findViewById(R.id.room_spinner);
        RecyclerView recycler = view.findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));


        adapter = new MyAdapter(getContext(), visibleItems, new MyAdapter.RoomSelectionProvider() {
            @Override
            public String getSelectedRoom() {
                if (roomSpinner != null && roomSpinner.getSelectedItem() != null) {
                    return roomSpinner.getSelectedItem().toString();
                }
                return "Main Office";
            }
        });
        recycler.setAdapter(adapter);


        roomAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roomNames);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roomSpinner.setAdapter(roomAdapter);


        FloatingActionButton fab = view.findViewById(R.id.fab_open_feedback);
        fab.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main, new FeedBackPage())
                    .addToBackStack(null)
                    .commit();
            Snackbar.make(v, "Opening feedback form...", Snackbar.LENGTH_SHORT).show();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lightViewModel = new ViewModelProvider(requireActivity()).get(LightViewModel.class);


        SharedRoomViewModel sharedRoomViewModel = new ViewModelProvider(requireActivity()).get(SharedRoomViewModel.class);
        sharedRoomViewModel.getCurrentRoom().observe(getViewLifecycleOwner(), roomName -> {
            if (roomName != null && !roomName.isEmpty()) {
                lightViewModel.init(roomName);
            }
        });

        initializeViews(view);
        mAuth = FirebaseAuth.getInstance();
        if (getContext() != null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        }

        settingsIcon.setOnClickListener(v -> showSettingsDialog());

        loadGreeting(new FirebaseUserDataProvider());
        initializeFirebase();
        setupPreferenceListener();
        updateVisibleItems();
    }

    private void initializeViews(View view) {
        userGreeting = view.findViewById(R.id.usergreeting);
        temperatureText = view.findViewById(R.id.temperature_text);
        comfortLevelText = view.findViewById(R.id.weather_description);
        settingsIcon = view.findViewById(R.id.settings_icon);
    }

    private void showSettingsDialog() {
        String[] itemNames = new String[allItems.size()];
        boolean[] checkedItems = new boolean[allItems.size()];

        for (int i = 0; i < allItems.size(); i++) {
            itemNames[i] = allItems.get(i).getName();
            checkedItems[i] = sharedPreferences.getBoolean("item_" + i, true);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Items to Display");
        builder.setMultiChoiceItems(itemNames, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for (int i = 0; i < checkedItems.length; i++) {
                editor.putBoolean("item_" + i, checkedItems[i]);
            }
            editor.apply();
            updateVisibleItems();
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateVisibleItems() {
        visibleItems.clear();
        for (int i = 0; i < allItems.size(); i++) {
            if (sharedPreferences.getBoolean("item_" + i, true)) {
                visibleItems.add(allItems.get(i));
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void initializeFirebase() {
        roomsRef = FirebaseDatabase.getInstance().getReference("rooms");
        roomsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                roomNames.clear();
                roomNames.add("Main Office");
                for (DataSnapshot roomSnapshot : dataSnapshot.getChildren()) {
                    roomNames.add(roomSnapshot.getKey());
                }
                roomAdapter.notifyDataSetChanged();

                // Restore last selected room
                if (!roomNames.isEmpty()) {
                    String lastSelectedRoom = sharedPreferences.getString("last_selected_room", roomNames.get(0));
                    int spinnerPosition = roomAdapter.getPosition(lastSelectedRoom);
                    if (spinnerPosition >= 0) {
                        roomSpinner.setSelection(spinnerPosition);
                    } else {
                        roomSpinner.setSelection(0);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error loading rooms: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        roomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRoom = (String) parent.getItemAtPosition(position);
                sharedPreferences.edit().putString("last_selected_room", selectedRoom).apply();

                SharedRoomViewModel sharedRoomViewModel = new ViewModelProvider(requireActivity()).get(SharedRoomViewModel.class);
                sharedRoomViewModel.setCurrentRoom(selectedRoom);

                attachRoomListener(selectedRoom);
                Toast.makeText(getContext(), "Loading data for: " + selectedRoom, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        roomsRef.addValueEventListener(roomsListener);
    }

    private void attachRoomListener(String roomName) {
        currentSelectedRoom = roomName;


        if (selectedRoomRef != null && selectedRoomListener != null) {
            selectedRoomRef.removeEventListener(selectedRoomListener);
        }
        if (airQualityRef != null && airQualityListener != null) {
            airQualityRef.removeEventListener(airQualityListener);
        }
        if (lightRef != null && lightListener != null) {
            lightRef.removeEventListener(lightListener);
        }


        if ("Main Office".equals(roomName)) {
            selectedRoomRef = FirebaseDatabase.getInstance().getReference("room_occupancy");
            airQualityRef = FirebaseDatabase.getInstance().getReference("sgp30_readings");
            lightRef = FirebaseDatabase.getInstance().getReference("sensorData").child("light");
        } else {
            selectedRoomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("room_occupancy");
            airQualityRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("air_quality_readings");
            lightRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("light");
        }


        selectedRoomListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    DataSnapshot lastReading = null;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        lastReading = snapshot;
                    }

                    if (lastReading != null) {
                        Double tempC = lastReading.child("current_temperature_c").getValue(Double.class);
                        Double tempF = lastReading.child("current_temperature_f").getValue(Double.class);
                        String comfortLevel = lastReading.child("comfort_level").getValue(String.class);

                        if (tempC != null || tempF != null) {
                            updateWeatherCard(tempC, tempF, comfortLevel);
                        } else {
                            updateWeatherCard(null, null, "No data");
                        }
                    } else {
                        updateWeatherCard(null, null, "No data");
                    }
                } else {
                    updateWeatherCard(null, null, "No data");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Temperature Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                updateWeatherCard(null, null, "Error");
            }
        };
        selectedRoomRef.addValueEventListener(selectedRoomListener);


        airQualityListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    DataSnapshot lastReading = null;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        lastReading = snapshot;
                    }

                    if (lastReading != null) {
                        String airQualityDescription = lastReading.child("co2_description").getValue(String.class);
                        updateAirQualityStatus(airQualityDescription);
                    } else {
                        updateAirQualityStatus("Good");
                    }
                } else {
                    updateAirQualityStatus("Good");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Air Quality Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                updateAirQualityStatus("Offline");
            }
        };
        airQualityRef.limitToLast(1).addValueEventListener(airQualityListener);


        lightListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Boolean powerOnValue = dataSnapshot.child("powerOn").getValue(Boolean.class);
                boolean isOn = powerOnValue != null && powerOnValue;
                updateSmartLightStatus(isOn);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        lightRef.addValueEventListener(lightListener);
    }


    private void updateAirQualityStatus(String airQualityDescription) {
        if (adapter != null && allItems != null) {
            for (item i : allItems) {
                if (i.getName().equals("Air Quality")) {
                    i.setStatus(airQualityDescription != null ? airQualityDescription : "Good");
                    for (item visibleItem : visibleItems) {
                        if (visibleItem.getName().equals("Air Quality")) {
                            visibleItem.setStatus(airQualityDescription != null ? airQualityDescription : "Good");
                            break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }


    private void updateSmartLightStatus(boolean isOn) {
        String status = isOn ? "On" : "Off";
        if (adapter != null && allItems != null) {
            for (item i : allItems) {
                if (i.getName().equals("Smart Light")) {
                    i.setStatus(status);
                    i.setDeviceOn(isOn); // Important: Syncs the toggle state

                    for (item visibleItem : visibleItems) {
                        if (visibleItem.getName().equals("Smart Light")) {
                            visibleItem.setStatus(status);
                            visibleItem.setDeviceOn(isOn);
                            break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    private void setupPreferenceListener() {
        preferenceChangeListener = (sharedPreferences, key) -> {

        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (roomsRef != null && roomsListener != null) {
            roomsRef.removeEventListener(roomsListener);
        }
        if (selectedRoomRef != null && selectedRoomListener != null) {
            selectedRoomRef.removeEventListener(selectedRoomListener);
        }
        if (airQualityRef != null && airQualityListener != null) {
            airQualityRef.removeEventListener(airQualityListener);
        }
        if (lightRef != null && lightListener != null) {
            lightRef.removeEventListener(lightListener);
        }
        if (sharedPreferences != null && preferenceChangeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    private void updateWeatherCard(Double tempC, Double tempF, String comfortLevel) {
        if (temperatureText == null || comfortLevelText == null) return;

        if (sharedPreferences == null && getContext() != null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        }

        String unit = sharedPreferences != null ? sharedPreferences.getString("units", "Metric (°C)") : "Metric (°C)";

        if (tempC == null && tempF == null) {
            temperatureText.setText("--°");
            comfortLevelText.setText(comfortLevel != null ? comfortLevel : "No data");
            return;
        }

        if (unit.equals("Imperial (°F)") && tempF != null) {
            temperatureText.setText(String.format(java.util.Locale.getDefault(), "%.0f°F", tempF));
        } else if (tempC != null) {
            temperatureText.setText(String.format(java.util.Locale.getDefault(), "%.0f°C", tempC));
        } else {

            if (tempC != null) {
                temperatureText.setText(String.format(java.util.Locale.getDefault(), "%.0f°C", tempC));
            } else if (tempF != null) {
                temperatureText.setText(String.format(java.util.Locale.getDefault(), "%.0f°F", tempF));
            } else {
                temperatureText.setText("--°");
            }
        }

        if (comfortLevel != null) {
            comfortLevelText.setText(comfortLevel.replace("_", " "));
        } else {
            comfortLevelText.setText("Comfort level unknown");
        }
    }

    protected void loadGreeting(UserDataProvider dataProvider) {
        dataProvider.fetchUserData(new UserDataProvider.UserDataCallback() {
            @Override
            public void onDataReceived(String userName) {
                if (userName != null && !userName.trim().isEmpty()) {
                    String firstName = userName.split(" ")[0];
                    setGreeting(firstName);
                } else {
                    setGreeting("User");
                }
            }

            @Override
            public void onError(String errorMessage) {
                setGreeting("User");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    protected Calendar getCalendarInstance() {
        return Calendar.getInstance();
    }

    String generateGreetingMessage(String name, Calendar calendar) {
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hourOfDay >= 0 && hourOfDay < 12) {
            greeting = "Good morning";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }
        return greeting + ", " + name;
    }

    protected void setGreeting(String name) {
        Calendar calendar = getCalendarInstance();
        String fullGreeting = generateGreetingMessage(name, calendar);
        if (userGreeting != null) {
            userGreeting.setText(fullGreeting);
        }
    }

    public void setDataProvider(FakeUserDataProvider provider) {
        this.dataProvider = provider;
    }
}
