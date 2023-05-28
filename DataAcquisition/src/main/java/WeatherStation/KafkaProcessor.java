package WeatherStation;


import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Properties;

public class KafkaProcessor {
    public static void main(String[] args) {
    	 // Set up Kafka Streams configuration
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Build the Kafka Streams topology
        StreamsBuilder builder = new StreamsBuilder();

        // Read from the input topic
        KStream<String, String> inputTopic = builder.stream("station");

        // Process the stream
        inputTopic
                .filter((key, value) -> {
                    // Parse the humidity value from the message
                	System.out.println(value) ;
                    int humidity = parseHumidityFromMessage(value);
                    // Check if humidity is higher than 70%
                    return humidity > 70;
                })
                .mapValues(value -> createSpecialMessage(value))
                .to("processor", Produced.with(Serdes.String(), Serdes.String()));

        // Create and start the Kafka Streams application
        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        // Add shutdown hook to gracefully close the streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
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
