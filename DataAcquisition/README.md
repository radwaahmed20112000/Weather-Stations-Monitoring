# Data Acquisition


Step 1: Get Kafka

	Download the latest Kafka release and extract it:

	$ tar -xzf kafka_2.13-3.4.0.tgz
	$ cd kafka_2.13-3.4.0
	
Step 2: Start the Kafka environment

	Start the ZooKeeper service
	$ bin/zookeeper-server-start.sh config/zookeeper.properties
	
	Start the Kafka broker service
	$ bin/kafka-server-start.sh config/server.properties
	
	
You can use this to confirm this API works on your machine.
	./bin/kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic station --from-beginning
	
	
You can use this to see detected raining messages 
	./bin/kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic processor --from-beginning
	

