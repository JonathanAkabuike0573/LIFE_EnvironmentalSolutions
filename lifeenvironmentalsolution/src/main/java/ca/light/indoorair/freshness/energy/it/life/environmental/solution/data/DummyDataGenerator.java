package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DummyDataGenerator {

    public static void sendDummyDataToFirebase(Context context) {
        DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("rooms");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        long currentMillis = System.currentTimeMillis();

        // --- Office Data - EXCELLENT Air Quality ---
        Map<String, Object> officeData = new HashMap<>();
        officeData.put("name", "Office");

        Map<String, Object> officeAirQuality = new HashMap<>();
        officeAirQuality.put("eCO2", 450);  // LOW value = EXCELLENT
        officeAirQuality.put("co2_description", "EXCELLENT"); // Clear description
        officeAirQuality.put("timestamp", timestamp);
        Map<String, Object> officeAirQualityReadings = new HashMap<>();
        officeAirQualityReadings.put(String.valueOf(currentMillis), officeAirQuality);
        officeData.put("air_quality_readings", officeAirQualityReadings);

        Map<String, Object> officeEnergy = new HashMap<>();
        officeEnergy.put("power_w", 75.5);
        officeEnergy.put("current_ma", 630.0);
        officeEnergy.put("vin_plus_v", 5.0);
        officeEnergy.put("timestamp", currentMillis);
        Map<String, Object> officeEnergyReadings = new HashMap<>();
        officeEnergyReadings.put(String.valueOf(currentMillis), officeEnergy);
        officeData.put("energy_readings", officeEnergyReadings);

        Map<String, Object> officeOccupancy = new HashMap<>();
        officeOccupancy.put("current_temperature_c", 24.5);
        officeOccupancy.put("current_temperature_f", 76.1);
        officeOccupancy.put("comfort_level", "Warm");
        Map<String, Object> officeOccupancyReadings = new HashMap<>();
        officeOccupancyReadings.put(String.valueOf(currentMillis), officeOccupancy);
        officeData.put("room_occupancy", officeOccupancyReadings);

        roomsRef.child("office").setValue(officeData);

        // --- Warehouse Data - VERY POOR Air Quality ---
        Map<String, Object> warehouseData = new HashMap<>();
        warehouseData.put("name", "Warehouse");
        long warehouseMillis = currentMillis + 1; // Ensure unique timestamp key

        Map<String, Object> warehouseAirQuality = new HashMap<>();
        warehouseAirQuality.put("eCO2", 2500); // HIGH value = VERY POOR
        warehouseAirQuality.put("co2_description", "VERY POOR"); // Clear description
        warehouseAirQuality.put("timestamp", timestamp);
        Map<String, Object> warehouseAirQualityReadings = new HashMap<>();
        warehouseAirQualityReadings.put(String.valueOf(warehouseMillis), warehouseAirQuality);
        warehouseData.put("air_quality_readings", warehouseAirQualityReadings);

        Map<String, Object> warehouseEnergy = new HashMap<>();
        warehouseEnergy.put("power_w", 450.0);
        warehouseEnergy.put("current_ma", 3750.0);
        warehouseEnergy.put("vin_plus_v", 5.0);
        warehouseEnergy.put("timestamp", warehouseMillis);
        Map<String, Object> warehouseEnergyReadings = new HashMap<>();
        warehouseEnergyReadings.put(String.valueOf(warehouseMillis), warehouseEnergy);
        warehouseData.put("energy_readings", warehouseEnergyReadings);

        Map<String, Object> warehouseOccupancy = new HashMap<>();
        warehouseOccupancy.put("current_temperature_c", 16.0);
        warehouseOccupancy.put("current_temperature_f", 60.8);
        warehouseOccupancy.put("comfort_level", "Cool");
        warehouseOccupancy.put("timestamp", timestamp);
        Map<String, Object> warehouseOccupancyReadings = new HashMap<>();
        warehouseOccupancyReadings.put(String.valueOf(warehouseMillis), warehouseOccupancy);
        warehouseData.put("room_occupancy", warehouseOccupancyReadings);

        roomsRef.child("warehouse").setValue(warehouseData);

        // --- Conference Room Data - GOOD Air Quality (Optional) ---
        Map<String, Object> conferenceData = new HashMap<>();
        conferenceData.put("name", "Conference Room");
        long conferenceMillis = currentMillis + 2;

        Map<String, Object> conferenceAirQuality = new HashMap<>();
        conferenceAirQuality.put("eCO2", 650); // MEDIUM value = GOOD
        conferenceAirQuality.put("co2_description", "GOOD");
        conferenceAirQuality.put("timestamp", timestamp);
        Map<String, Object> conferenceAirQualityReadings = new HashMap<>();
        conferenceAirQualityReadings.put(String.valueOf(conferenceMillis), conferenceAirQuality);
        conferenceData.put("air_quality_readings", conferenceAirQualityReadings);

        Map<String, Object> conferenceEnergy = new HashMap<>();
        conferenceEnergy.put("power_w", 120.0);
        conferenceEnergy.put("current_ma", 1000.0);
        conferenceEnergy.put("vin_plus_v", 5.0);
        conferenceEnergy.put("timestamp", conferenceMillis);
        Map<String, Object> conferenceEnergyReadings = new HashMap<>();
        conferenceEnergyReadings.put(String.valueOf(conferenceMillis), conferenceEnergy);
        conferenceData.put("energy_readings", conferenceEnergyReadings);

        Map<String, Object> conferenceOccupancy = new HashMap<>();
        conferenceOccupancy.put("current_temperature_c", 22.0);
        conferenceOccupancy.put("current_temperature_f", 71.6);
        conferenceOccupancy.put("comfort_level", "Comfortable");
        conferenceOccupancy.put("timestamp", timestamp);
        Map<String, Object> conferenceOccupancyReadings = new HashMap<>();
        conferenceOccupancyReadings.put(String.valueOf(conferenceMillis), conferenceOccupancy);
        conferenceData.put("room_occupancy", conferenceOccupancyReadings);

        roomsRef.child("conference").setValue(conferenceData)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Dummy data for Office, Warehouse, and Conference Room sent.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to send dummy data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}