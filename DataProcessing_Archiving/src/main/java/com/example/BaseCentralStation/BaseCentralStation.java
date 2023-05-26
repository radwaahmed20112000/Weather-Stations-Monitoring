package com.example.BaseCentralStation;


import com.example.Bitcask.Bitcask;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BaseCentralStation {
	private static final Bitcask bitcask;
	static {
		try {
			bitcask = new Bitcask("/data/", "RiakLSM");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception {

		SpringApplication.run(BaseCentralStation.class, args);

		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
				"localhost:9092");
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
		properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

		consumer.subscribe(Collections.singletonList("station"));
		ParquetManager parquetManager = new ParquetManager();

		List<Status> statuses = new ArrayList<>();

		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
			for (ConsumerRecord<String, String> record : records) {
				if (record.value().length() == 0) continue;
				Status status = Utils.parseMessage(record.value());
				statuses.add(status);
				bitcask.put(status.getStationID(), record.value());
			}
			if(statuses.size() == 100) {
				parquetManager.setStatuses(statuses);
				statuses = new ArrayList<>();
				Thread thread = new Thread(parquetManager);
				thread.start();
			}
		}
	}
	@Scheduled( initialDelay = 15 * 1000, fixedDelay = 30 * 1000)
	public void compaction() throws IOException {
		bitcask.compactFiles();
	}

	@Scheduled(initialDelay = 15 * 1000, fixedDelay = 5000)
	public void getStatuses() throws Exception {
		for (int i = 1; i < 11; i++)
			System.out.println("Station " + i + ": " + bitcask.get(i));
	}

	@Scheduled(initialDelay = 300 * 1000, fixedDelay = 300 * 1000)
	public void getValue() throws Exception {
		for (int i = 1; i < 11; i++)
			System.out.println("Station " + i + ": " + bitcask.get(i));
	}
}
