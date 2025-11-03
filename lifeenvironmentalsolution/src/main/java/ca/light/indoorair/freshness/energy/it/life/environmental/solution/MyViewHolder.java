package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class MyViewHolder extends RecyclerView.ViewHolder {

    ImageView device_icon;
    TextView device_name;
    TextView device_status;
    SwitchMaterial device_toggle;

    public MyViewHolder(@NonNull View itemView) {
        super(itemView);
        device_icon = itemView.findViewById(R.id.device_icon);
        device_name = itemView.findViewById(R.id.device_name);
        device_status = itemView.findViewById(R.id.device_status);
        device_toggle = itemView.findViewById(R.id.device_toggle);
    }
}
