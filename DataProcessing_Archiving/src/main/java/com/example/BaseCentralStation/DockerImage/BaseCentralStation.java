package com.example.BaseCentralStation.DockerImage;

import com.example.Bitcask.Bitcask;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class BaseCentralStation {


    public static void main(String[] args) throws Exception {

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(Collections.singletonList("station"));
        ParquetManager parquetManager = new ParquetManager();

//        Bitcask bitcask = new Bitcask("test");
        List<Status> statuses = new ArrayList<>();

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                if (record.value().length() == 0) continue;
                System.out.println(record.value());
                Status status = Utils.parseMessage(record.value());
                statuses.add(status);
//                bitcask.put(status.getStationID(), record.value());
            }
            if(statuses.size() == 10000) {
                parquetManager.setStatuses(statuses);
                statuses = new ArrayList<>();
                Thread thread = new Thread(parquetManager);
                thread.start();
            }
        }

    }
}