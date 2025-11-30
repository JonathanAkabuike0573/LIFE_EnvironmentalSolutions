package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.AirQualityFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.EnergyFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.LightFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.PresenceFragment;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

    Context context;
    List<item> list;
    RoomSelectionProvider roomSelectionProvider;

    // Interface to get the current room from DashboardFragment
    public interface RoomSelectionProvider {
        String getSelectedRoom();
    }

    public MyAdapter(Context context, List<item> list, RoomSelectionProvider roomSelectionProvider) {
        this.context = context;
        this.list = list;
        this.roomSelectionProvider = roomSelectionProvider;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.dbitem_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        item currentItem = list.get(position);

        holder.device_name.setText(currentItem.getName());
        holder.device_status.setText(currentItem.getStatus());

        // --- 1. CLICK LISTENER (OPEN FRAGMENT) ---
        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = null;
            String selectedRoom = roomSelectionProvider.getSelectedRoom();

            // Pass the selected room to the new fragment
            Bundle args = new Bundle();
            args.putString("SELECTED_ROOM_KEY", selectedRoom);

            switch (currentItem.getName()) {
                case "Air Quality":
                    fragment = new AirQualityFragment();
                    break;
                case "Smart Light":
                    fragment = new LightFragment();
                    break;
                case "Presence Sensor":
                    fragment = new PresenceFragment();
                    break;
                case "Thermostat":
                    fragment = new EnergyFragment();
                    break;
            }

            if (fragment != null) {
                fragment.setArguments(args);
                ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        // --- 2. SETUP TOGGLE SWITCH ---
        if (!currentItem.isShowToggle()) {
            holder.device_toggle.setVisibility(View.GONE);
            holder.device_icon.setImageResource(currentItem.getImages());
            return;
        }

        holder.device_toggle.setVisibility(View.VISIBLE);

        // Remove previous listener to avoid triggering it during recycling
        holder.device_toggle.setOnCheckedChangeListener(null);

        // Set the current visual state
        holder.device_toggle.setChecked(currentItem.isDeviceOn());
        updateIcon(holder, currentItem, currentItem.isDeviceOn());

        // --- 3. TOGGLE CLICK LISTENER (THE FIX) ---
        holder.device_toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {

            // A. Immediate Local UI Update (Responsiveness)
            currentItem.setDeviceOn(isChecked);
            String status = isChecked ? "On" : "Off";
            currentItem.setStatus(status);
            holder.device_status.setText(status);
            updateIcon(holder, currentItem, isChecked);

            // B. Write to Firebase (Control Logic)
            if ("Smart Light".equals(currentItem.getName())) {
                // 1. Get Current Room
                String roomName = roomSelectionProvider.getSelectedRoom();

                // 2. Determine Firebase Path
                DatabaseReference lightRef;
                if ("Main Office".equals(roomName)) {
                    lightRef = FirebaseDatabase.getInstance().getReference("sensorData").child("light");
                } else {
                    lightRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("light");
                }

                // 3. Write to 'powerOn' node (Controls LightFragment & Simulation)
                lightRef.child("powerOn").setValue(isChecked)
                        .addOnFailureListener(e -> {
                            // Revert UI if write fails
                            Toast.makeText(context, "Failed to switch light", Toast.LENGTH_SHORT).show();
                            holder.device_toggle.setChecked(!isChecked);
                        });

            } else if ("Presence Sensor".equals(currentItem.getName())) {
                // Presence Sensor Logic (SharedPrefs)
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                prefs.edit().putBoolean("presence_detection_enabled", isChecked).apply();
                Toast.makeText(context, "Presence Sensor is " + status, Toast.LENGTH_SHORT).show();

            } else {

                Toast.makeText(context, currentItem.getName() + " is " + status, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateIcon(MyViewHolder holder, item currentItem, boolean isChecked) {
        String itemName = currentItem.getName();
        if (itemName.equals("Smart Light")) {
            holder.device_icon.setImageResource(isChecked ? R.drawable.lightonicon : R.drawable.lightofficon);
        } else if (itemName.equals("Smart TV")) {
            holder.device_icon.setImageResource(isChecked ? R.drawable.tv_onicon : R.drawable.tv_officon);
        } else if (itemName.equals("Presence Sensor")) {
            holder.device_icon.setImageResource(isChecked ? R.drawable.detection_on : R.drawable.detectionoff);
        } else {
            holder.device_icon.setImageResource(currentItem.getImages());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
