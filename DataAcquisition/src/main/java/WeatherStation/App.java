package WeatherStation;


import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class App 
{
	
	
	public static void sendToKafka(WeatherStationMock station , KafkaProducer<String,String> producer) {
		ProducerRecord<String, String> record = new ProducerRecord<>("station",
				station.getWeatherStatusMessage());
				producer.send(record);
	}
	
    public static void main( String[] args ) throws InterruptedException
    {
    	
    	Properties properties = new Properties();
    	properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
    	"127.0.0.1:9093");
    	properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
    	StringSerializer.class.getName());
    	properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
    	StringSerializer.class.getName());
    	
    	KafkaProducer<String,String> producer = new KafkaProducer<>(properties);
        
        // Call sendWeatherStatus for each station instance
        while (true) {
        	for(int i=1 ;i<=10 ;i++)
        	{
        		 WeatherStationMock station = new WeatherStationMock(i);
        		 station.sendWeatherStatus();
        		 sendToKafka(station , producer);
        	}
            
            try {
                Thread.sleep(1000);  // wait 1 second between sending messages
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
