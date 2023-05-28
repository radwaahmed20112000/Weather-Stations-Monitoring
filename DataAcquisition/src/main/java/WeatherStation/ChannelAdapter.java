package WeatherStation;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class ChannelAdapter {

    public Date timeStampToDate(long timestamp){
        return new Date(timestamp * 1000L);
    }

    public WeatherData adapt(JSONObject response) throws JSONException {

        WeatherData weatherData = new WeatherData();
        weatherData.setHumidity(response.getInt("relativehumidity_2m"));
        weatherData.setTemperature((int) Math.round(response.getDouble("temperature_2m")));
        weatherData.setWindSpeed((int) Math.round(response.getDouble("windspeed_10m")));
        return weatherData;
    }

    public static void main(String[] args) {


            }
    }

