package ca.light.indoorair.freshness.energy.it.life.environmental.solution;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

    Context context;
    List<item> list;
    public MyAdapter(Context context, List<item> list) {
        this.context = context;
        this.list = list;
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
        holder.device_icon.setImageResource(currentItem.getImages());

        if (currentItem.isShowToggle()) {
            holder.device_toggle.setVisibility(View.VISIBLE);
        } else {
            holder.device_toggle.setVisibility(View.GONE);
        }

        // Special handling for Smart Light
        if ("Smart Light".equals(currentItem.getName())) {
            // Update status based on toggle state
            holder.device_status.setText(currentItem.isDeviceOn() ? "On" : "Off");
            holder.device_toggle.setOnCheckedChangeListener(null);
            holder.device_toggle.setChecked(currentItem.isDeviceOn());

            holder.device_toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentItem.setDeviceOn(isChecked);
                holder.device_status.setText(isChecked ? "On" : "Off");
                currentItem.setStatus(isChecked ? "On" : "Off");

                // Switch between lightonicon and lightofficon
                if (isChecked) {
                    holder.device_icon.setImageResource(R.drawable.lightonicon);
                } else {
                    holder.device_icon.setImageResource(R.drawable.lightofficon);
                }
            });
        } else {
            // For other items, just display the status
            holder.device_status.setText(currentItem.getStatus());
            holder.device_toggle.setOnCheckedChangeListener(null);
            holder.device_toggle.setChecked(currentItem.isDeviceOn());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}