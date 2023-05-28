package WeatherStation;

public class WeatherData {
    private int humidity;
    private int temperature;

    private int windSpeed;

    public WeatherData() {
        this.humidity = 0;
        this.temperature = 0;
        this.windSpeed = 0;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(int windSpeed) {
        this.windSpeed = windSpeed;
    }



}
