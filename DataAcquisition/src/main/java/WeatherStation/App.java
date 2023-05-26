package WeatherStation;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class App 
{
	public static void sendToKafka(WeatherStationMock station, KafkaProducer<String, String> producer) {
		ProducerRecord<String, String> record = new ProducerRecord<>("station", station.getWeatherStatusMessage());
		producer.send(record);
	}

	private static List<Boolean> generateRandomDrops() {
		List<Boolean> randoms = new ArrayList<>();
		int i;
		for(i = 0; i < 10; ++i)
			randoms.add(false);

		for(i = 10; i < 100; ++i)
			randoms.add(true);

		Collections.shuffle(randoms);
		return randoms;
	}

	public static void main(String[] args) {
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
				"localhost:9092");
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class.getName());
		KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
		int i = 0;

		long stationId = Long.parseLong(System.getenv("STATION_ID"));

		List<Boolean> randomDrops = generateRandomDrops();

		while(true) {
			if (randomDrops.get(i)) {
				WeatherStationMock station = new WeatherStationMock(stationId);
				station.sendWeatherStatus();
				sendToKafka(station, producer);
			}

			++i;
			if (i == 100) {
				i = 0;
				randomDrops = generateRandomDrops();
			}

			try {
				Thread.sleep(1000L);
			} catch (InterruptedException var6) {
				var6.printStackTrace();
			}
		}
	}
}
