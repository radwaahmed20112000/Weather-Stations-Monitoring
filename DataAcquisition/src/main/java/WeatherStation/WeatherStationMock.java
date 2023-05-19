package WeatherStation;

import java.util.Random;

public class WeatherStationMock {
    private long stationId;
    private long sNo;
    private String batteryStatus;
    private long statusTimestamp;
    private int humidity;
    private int temperature;
    private int windSpeed;
    private Random random;
    private String message = "" ;

    public WeatherStationMock(long stationId) {
        this.stationId = stationId;
        this.sNo = 1;
        this.batteryStatus = "medium";
        this.statusTimestamp = System.currentTimeMillis() / 1000L;
        this.humidity = 0;
        this.temperature = 0;
        this.windSpeed = 0;
        this.random = new Random();
    }

    public void sendWeatherStatus() {
        // Randomly change battery status
        int batteryStatusChance = random.nextInt(100);
        if (batteryStatusChance < 30) {
            batteryStatus = "low";
        } else if (batteryStatusChance < 70) {
            batteryStatus = "medium";
        } else {
            batteryStatus = "high";
        }

        // Randomly drop messages on a 10% rate
        int dropChance = random.nextInt(100);
        if (dropChance < 10) {
            return;
        }

        // Generate weather data
        humidity = random.nextInt(100);
        temperature = random.nextInt(100) + 50; // temperature between 50-149 degrees Fahrenheit
        windSpeed = random.nextInt(50);

        // Construct weather status message
        String weatherStatusMsg = "{"
                + "\"station_id\": " + stationId + ","
                + "\"s_no\": " + sNo + ","
                + "\"battery_status\": \"" + batteryStatus + "\","
                + "\"status_timestamp\": " + statusTimestamp + ","
                + "\"weather\": {"
                + "\"humidity\": " + humidity + ","
                + "\"temperature\": " + temperature + ","
                + "\"wind_speed\": " + windSpeed
                + "}"
                + "}";
        
        this.message = weatherStatusMsg ;
        // Send weather status message to Kafka
        //sendToKafka(weatherStatusMsg);

        // Increment sNo for the next message
        sNo++;
    }

    public String getWeatherStatusMessage() {
        // Implement logic to send message to Kafka queueing service
        //System.out.println("Sending message to Kafka: " + this.message);
        return this.message ;
    }
    
}
