package WeatherStation;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaProcessor {
    public static void main(String[] args) {
        // Set up Kafka consumer properties
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "raining-trigger-consumer");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create a Kafka consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);

        // Subscribe to the input topic
        consumer.subscribe(Collections.singleton("station"));

        // Set up Kafka producer properties
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "Kafka-Service:9092");
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Create a Kafka producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);

        // Start consuming and processing messages
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            System.out.println("records");
            for (ConsumerRecord<String, String> record : records) {
                String value = record.value();
                
                System.out.println(value);

                // Parse the humidity value from the message
                int humidity = parseHumidityFromMessage(value);
                System.out.println(humidity);
                // Check if humidity is higher than 70%
                if (humidity > 70) {
                    // Create the special message
                    String specialMessage = createSpecialMessage(value);

                    // Produce the special message to the output topic
                    ProducerRecord<String, String> specialRecord = new ProducerRecord<>("processor", specialMessage);
                    producer.send(specialRecord);
                }
            }
        }
    }

    private static int parseHumidityFromMessage(String jsonString) {


        // Find the index of the "humidity" field
        int humidityIndex = jsonString.indexOf("\"humidity\"");

        int humidity = 0 ;
        if (humidityIndex != -1) {
            // Find the start index of the humidity value
            int valueStartIndex = humidityIndex + "\"humidity\":".length();

            // Find the end index of the humidity value
            int valueEndIndex = jsonString.indexOf(",", valueStartIndex);
            if (valueEndIndex == -1) {
                valueEndIndex = jsonString.indexOf("}", valueStartIndex);
            }

            if (valueEndIndex != -1) {
                // Extract the humidity value as a string
                String humidityValue = jsonString.substring(valueStartIndex, valueEndIndex);
                try {
                    // Convert the humidity value to an integer
                     humidity = Integer.parseInt(humidityValue.trim());

                    System.out.println("Humidity: " + humidity);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid humidity value");
                }
            } else {
                System.out.println("Humidity value not found");
            }
        } 
        return humidity;
    }

    private static String createSpecialMessage(String message) {
        return "Special Message: " + message + "Humidity is greater than 70";
    }
}
