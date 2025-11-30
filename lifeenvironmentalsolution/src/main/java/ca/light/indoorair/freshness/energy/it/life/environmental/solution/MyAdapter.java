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

import java.util.List;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.AirQualityFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.EnergyFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.LightFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.PresenceFragment;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

    Context context;
    List<item> list;
    RoomSelectionProvider roomSelectionProvider;

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

        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = null;

            // CRITICAL FIX: Get the CURRENT room selection at click time
            String selectedRoom = roomSelectionProvider.getSelectedRoom();

            // Debug: Show which room is being passed
            Toast.makeText(context, "Opening " + currentItem.getName() + " for: " + selectedRoom, Toast.LENGTH_SHORT).show();

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

        if (!currentItem.isShowToggle()) {
            holder.device_toggle.setVisibility(View.GONE);
            holder.device_icon.setImageResource(currentItem.getImages());
            return;
        }

        holder.device_toggle.setVisibility(View.VISIBLE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // --- Initial State Setup ---
        boolean isInitiallyOn;
        if ("Presence Sensor".equals(currentItem.getName())) {
            isInitiallyOn = prefs.getBoolean("presence_detection_enabled", true);
        } else {
            isInitiallyOn = currentItem.isDeviceOn();
        }

        // Update the model and UI without triggering the listener
        holder.device_toggle.setOnCheckedChangeListener(null);
        holder.device_toggle.setChecked(isInitiallyOn);
        currentItem.setDeviceOn(isInitiallyOn);

        String initialStatus;
        if ("Presence Sensor".equals(currentItem.getName())) {
            initialStatus = isInitiallyOn ? "On" : "Off";
        } else {
            initialStatus = isInitiallyOn ? "On" : "Off";
        }
        if(!"Air Quality".equals(currentItem.getName())) {
            holder.device_status.setText(initialStatus);
            currentItem.setStatus(initialStatus);
        }
        updateIcon(holder, currentItem, isInitiallyOn);

        // --- Listener Setup ---
        holder.device_toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status;
            if ("Presence Sensor".equals(currentItem.getName())) {
                status = isChecked ? "On" : "Off";
                // Save state for presence sensor
                prefs.edit().putBoolean("presence_detection_enabled", isChecked).apply();
            } else {
                status = isChecked ? "On" : "Off";
            }

            String message = currentItem.getName() + " is " + status;
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

            // Update model and UI
            currentItem.setDeviceOn(isChecked);
            currentItem.setStatus(status);
            holder.device_status.setText(status);
            updateIcon(holder, currentItem, isChecked);
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