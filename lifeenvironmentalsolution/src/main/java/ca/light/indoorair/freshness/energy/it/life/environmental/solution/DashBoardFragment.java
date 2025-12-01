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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.FeedBackPage;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.DashboardViewModel;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.SharedRoomViewModel;

public class DashBoardFragment extends Fragment {

    public Object dataProvider;

    // UI Elements
    private TextView userGreeting, temperatureText, comfortLevelText;
    private ImageView settingsIcon;
    private Spinner roomSpinner;


    private DashboardViewModel dashboardViewModel;
    private SharedRoomViewModel sharedRoomViewModel;

    // SharedPreferences & Adapter
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private MyAdapter adapter;
    private List<item> allItems;
    private List<item> visibleItems;
    private List<String> roomNames;
    private ArrayAdapter<String> roomAdapter;

    public DashBoardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dash_board, container, false);

        // Setup data lists
        allItems = new ArrayList<>();
        allItems.add(new item("Air Quality", "Good", R.drawable.air_qualityicon, true, false));
        allItems.add(new item("Smart Light", "Off", R.drawable.lightofficon, false, true));
        allItems.add(new item("Thermostat", "22°C", R.drawable.thermostaticon, true, false));
        allItems.add(new item("Air Conditioner", "Off", R.drawable.airconditionericon, false, true));
        allItems.add(new item("Presence Sensor", "Off", R.drawable.sensor_occupied, false, true));
        allItems.add(new item("Smart TV", "Off", R.drawable.tv_officon, false, true));

        visibleItems = new ArrayList<>();
        roomNames = new ArrayList<>();

        // Setup RecyclerView
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

        // Setup room spinner
        roomAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roomNames);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roomSpinner.setAdapter(roomAdapter);

        // Setup FAB
        FloatingActionButton fab = view.findViewById(R.id.fab_open_feedback);
        fab.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main, new FeedBackPage())
                    .addToBackStack(null)
                    .commit();
            Snackbar.make(v, R.string.opening_feedback_form, Snackbar.LENGTH_SHORT).show();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());


        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        sharedRoomViewModel = new ViewModelProvider(requireActivity()).get(SharedRoomViewModel.class);

        initializeViews(view);
        setupObservers();
        setupRoomSpinner();
        setupPreferenceListener();
        updateVisibleItems();

        // Load greeting
        loadGreeting(new FirebaseUserDataProvider());
    }


    private void initializeViews(View view) {
        userGreeting = view.findViewById(R.id.usergreeting);
        temperatureText = view.findViewById(R.id.temperature_text);
        comfortLevelText = view.findViewById(R.id.weather_description);
        settingsIcon = view.findViewById(R.id.settings_icon);


        settingsIcon.setOnClickListener(v -> showSettingsDialog());
    }

    private void setupObservers() {

        dashboardViewModel.getTemperatureC().observe(getViewLifecycleOwner(), this::updateTemperatureDisplay);
        dashboardViewModel.getComfortLevel().observe(getViewLifecycleOwner(), this::updateComfortDisplay);


        dashboardViewModel.getAirQuality().observe(getViewLifecycleOwner(), this::updateAirQualityItem);


        dashboardViewModel.getSmartLightPower().observe(getViewLifecycleOwner(), this::updateSmartLightStatus);

        // Sync with room changes
        sharedRoomViewModel.getCurrentRoom().observe(getViewLifecycleOwner(), roomName -> {
            if (roomName != null && !roomName.isEmpty()) {
                dashboardViewModel.init(roomName);
            }
        });
    }

    private void setupRoomSpinner() {

        DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("rooms");
        roomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                roomNames.clear();
                roomNames.add("Main Office");
                for (DataSnapshot roomSnapshot : dataSnapshot.getChildren()) {
                    roomNames.add(roomSnapshot.getKey());
                }
                roomAdapter.notifyDataSetChanged();


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
                Toast.makeText(getContext(), R.string.error_loading_rooms, Toast.LENGTH_SHORT).show();
            }
        });

        // Room selection
        roomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRoom = (String) parent.getItemAtPosition(position);
                sharedPreferences.edit().putString("last_selected_room", selectedRoom).apply();
                sharedRoomViewModel.setCurrentRoom(selectedRoom);
                Toast.makeText(getContext(), getString(R.string.loading_data_for) + selectedRoom, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


    }

    private void updateTemperatureDisplay(Double tempC) {
        Double tempF = dashboardViewModel.getTemperatureF().getValue();
        String comfort = dashboardViewModel.getComfortLevel().getValue();
        updateWeatherCard(tempC, tempF, comfort);
    }

    private void updateComfortDisplay(String comfortLevel) {
        if (comfortLevelText != null) {
            comfortLevelText.setText(comfortLevel != null ? comfortLevel.replace("_", " ") : "Comfort level unknown");
        }
    }

    private void updateAirQualityItem(String airQualityDescription) {
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

    private void updateSmartLightStatus(Boolean isOn) {
        if (isOn == null) return;
        String status = isOn ? "On" : "Off";
        if (adapter != null && allItems != null) {
            for (item i : allItems) {
                if (i.getName().equals("Smart Light")) {
                    i.setStatus(status);
                    i.setDeviceOn(isOn);
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

    private void setupPreferenceListener() {
        preferenceChangeListener = (sharedPreferences, key) -> {};
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }


    protected void loadGreeting(UserDataProvider dataProvider) {
        dataProvider.fetchUserData(new UserDataProvider.UserDataCallback() {
            @Override
            public void onDataReceived(String userName) {

                if (!isAdded() || getContext() == null) {
                    return;
                }

                if (userName != null && !userName.trim().isEmpty()) {
                    String firstName = userName.split(" ")[0];
                    setGreeting(firstName);
                } else {
                    setGreeting("User");
                }
            }

            @Override
            public void onError(String errorMessage) {

                if (!isAdded() || getContext() == null) {
                    return;
                }

                setGreeting("User");
                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
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
            greeting = getString(R.string.good_morning);
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            greeting = getString(R.string.good_afternoon);
        } else {
            greeting = getString(R.string.good_evening);
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

    @Override
    public void onStop() {
        super.onStop();
        if (sharedPreferences != null && preferenceChangeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }
}
