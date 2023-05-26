package com.example.dataprocessing_archiving.BaseCentralStation;

import org.json.JSONObject;

import java.io.File;

public class Utils {

    public static Status parseMessage(String message) {
        // Parse JSON object
        JSONObject jsonObject = new JSONObject(message);

        // Get values from JSON object
        long stationID = jsonObject.getLong("station_id");
        String batteryStatus = jsonObject.getString("battery_status");
        long timestamp = jsonObject.getLong("status_timestamp");
        JSONObject weatherObject = jsonObject.getJSONObject("weather");

        long sNo = jsonObject.getLong("s_no");
        int humidity = weatherObject.getInt("humidity");
        int temperature = weatherObject.getInt("temperature");
        int windSpeed = weatherObject.getInt("wind_speed");

        return new Status(stationID, sNo, batteryStatus, timestamp, humidity, temperature, windSpeed);
    }

    public static void pathCheck(String path) {
        File file = new File(path);

        // Get the parent directory
        File parentDir = file.getParentFile();

        // Create the parent directory and any missing ancestors
        if (!parentDir.exists()) {
            boolean success = parentDir.mkdirs();
            if (!success) {
                // Failed to create the directory
                throw new RuntimeException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }
    }
}
