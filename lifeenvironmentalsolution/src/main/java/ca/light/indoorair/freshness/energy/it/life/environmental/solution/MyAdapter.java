package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        updateIcon(holder, currentItem, currentItem.isDeviceOn());


        holder.itemView.setOnClickListener(v -> {


            List<String> premiumDevices = Arrays.asList("Air Conditioner", "Thermostat", "Smart TV");


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean hasPaid = prefs.getBoolean("has_paid_subscription", false);
            Set<String> allowedDevices = prefs.getStringSet("allowed_devices", new HashSet<>());


            if (premiumDevices.contains(currentItem.getName())) {
                if (!hasPaid || !allowedDevices.contains(currentItem.getName())) {
                    Toast.makeText(context, "Locked: Upgrade plan to access " + currentItem.getName(), Toast.LENGTH_SHORT).show();
                    return;
                }
            }


            String selectedRoom = roomSelectionProvider.getSelectedRoom();
            if (context instanceof MainActivity) {
                ((MainActivity) context).navigateToDashboardItem(currentItem.getName(), selectedRoom);
            }
        });

        if (!currentItem.isShowToggle()) {
            holder.device_toggle.setVisibility(View.GONE);
            holder.device_icon.setImageResource(currentItem.getImages());
            return;
        }

        holder.device_toggle.setVisibility(View.VISIBLE);
        holder.device_toggle.setOnCheckedChangeListener(null);
        holder.device_toggle.setChecked(currentItem.isDeviceOn());

        holder.device_toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean hasPaid = prefs.getBoolean("has_paid_subscription", false);
            Set<String> allowedDevices = prefs.getStringSet("allowed_devices", new HashSet<>());

            if (Arrays.asList("Air Conditioner", "Smart TV").contains(currentItem.getName())) {
                if (!hasPaid || !allowedDevices.contains(currentItem.getName())) {
                    buttonView.setChecked(false);
                    Toast.makeText(context, "Locked", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if ("Smart Light".equals(currentItem.getName())) {
                String roomName = roomSelectionProvider.getSelectedRoom();
                DatabaseReference lightRef;
                if ("Main Office".equals(roomName)) {
                    lightRef = FirebaseDatabase.getInstance().getReference("sensorData").child("light");
                } else {
                    lightRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("light");
                }
                lightRef.child("powerOn").setValue(isChecked);

            } else if ("Presence Sensor".equals(currentItem.getName())) {
                currentItem.setDeviceOn(isChecked);
                String status = isChecked ? "On" : "Off";
                currentItem.setStatus(status);
                holder.device_status.setText(status);
                updateIcon(holder, currentItem, isChecked);

                SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
                p.edit().putBoolean("presence_detection_enabled", isChecked).apply();

            } else {
                currentItem.setDeviceOn(isChecked);
                String status = isChecked ? "On" : "Off";
                currentItem.setStatus(status);
                holder.device_status.setText(status);
                updateIcon(holder, currentItem, isChecked);
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
