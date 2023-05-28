package WeatherStation;


import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class App
{
	public static void sendToKafka(WeatherStationMock station, KafkaProducer<String, String> producer) {
		ProducerRecord<String, String> record = new ProducerRecord<>("station", station.getWeatherStatusMessage());
		producer.send(record);
	}


	public static void main(String[] args) {

		String bootstrapServers = System.getenv("KAFKA_SERVER");
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
				bootstrapServers);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class.getName());
		KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

		long stationId = Long.parseLong(System.getenv("STATION_ID"));


		while(true) {
			WeatherStationMock station = new WeatherStationMock(stationId);
			if(station.sendWeatherStatus(stationId))
				sendToKafka(station, producer);

			try {
				Thread.sleep(1000L);
			} catch (InterruptedException var6) {
				var6.printStackTrace();
			}
		}
	}
}