package WeatherStation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class WeatherStationMock {
    private long stationId;
    private long sNo;
    private String batteryStatus;
    private long statusTimestamp;

    private WeatherData weatherData;
    private Random random;
    private String message = "" ;

    OpenMeteo openMeteo = new OpenMeteo();
    ChannelAdapter channelAdapter = new ChannelAdapter();
    public WeatherStationMock(long stationId) {
        this.stationId = stationId;
        this.sNo = 1;
        this.batteryStatus = "medium";
        this.statusTimestamp = System.currentTimeMillis() / 1000L;
        this.random = new Random();
    }

    public void sendWeatherStatus(int stationId) {
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
        JSONObject jsonData = openMeteo.getData(channelAdapter.timeStampToDate(statusTimestamp), stationId);

        try{
            weatherData = channelAdapter.adapt(jsonData);
        }
        catch (JSONException e){
            System.out.println("Error");
        }

        // Construct weather status message
        String weatherStatusMsg = "{"
                + "\"station_id\": " + stationId + ","
                + "\"s_no\": " + sNo + ","
                + "\"battery_status\": \"" + batteryStatus + "\","
                + "\"status_timestamp\": " + statusTimestamp + ","
                + "\"weather\": {"
                + "\"humidity\": " + weatherData.getHumidity() + ","
                + "\"temperature\": " + weatherData.getTemperature() + ","
                + "\"wind_speed\": " + weatherData.getTemperature()
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
