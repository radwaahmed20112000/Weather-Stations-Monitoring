# Weather Stations Monitoring

This repository contains the code for a distributed weather monitoring system that uses IoT devices to collect weather data, and processes and archives it for analysis. The system architecture consists of three stages: data acquisition, data processing and archiving, and indexing.

## System Architecture

The system is composed of the following components:

- **Data Acquisition:** multiple weather stations that feed a queueing service (Kafka) with their readings.
- **Data Processing & Archiving:** The base central station is consuming the streamed data and archiving all data in the form of Parquet files.
- **Indexing:** two variants of index are maintained:
  - Key-value store (Bitcask) for the latest reading from each individual station.
  - ElasticSearch / Kibana that are running over the Parquet files.

## Implementation

1. **Weather Station Mock:** Each weather station should output a status message every 1 second to report its sampled weather status. The weather station should randomly change the battery status field and drop messages. See the `Weather Status Message` section below for more details.

2. **Set up Weather Station to connect to Kafka:** Use the produce API to send messages to the Kafka server. You should use the java programmatic API for this task.

3. **Implement Raining Triggers in Kafka Processors:** Use Kafka Processors to detect when it's raining. You should use Kafka Processor to detect if humidity is higher than 70%. The processor should output a special message to a specific topic.

4. **Implement Central Station:** Implement BitCask Riak to store an updated view of weather status. You should maintain a key value store of the station's statuses. To do this efficiently, you are going to implement the BitCask Riak LSM to maintain an updated store of each station status. You should also archive all weather statuses history for all stations by appending all weather statuses into parquet files. See the `Implement Archiving of all Weather Statuses` section below for more details.

5. **Set up Historical Weather Statuses Analysis:** Direct all weather statuses to ElasticSearch for indexing and querying by Kibana.

6. **Deploy using Kubernetes:** Deploy all the components of the system using Docker and Kubernetes.

7. **Profile Central Station using JFR:** Use Java Flight Recorder (JFR) to profile the central station and measure execution time, memory consumption, GC pauses and their durations, and I/O operations.

### Weather Status Message

The weather status message should be of the following schema:

```
{
    "station_id": 1, // Long
    "s_no": 1, // Long auto-incremental with each message per service
    "battery_status": "low", // String of (low, medium, high)
    "status_timestamp": 1681521224, // Long Unix timestamp
    "weather": {
        "humidity": 35, // Integer percentage
        "temperature": 100, // Integer in fahrenheit
        "wind_speed": 13, // Integer km/h
    }
}
```

### Implement Archiving of all Weather Statuses

To archive all weather statuses history for all stations, append all weather statuses into parquet files, and partition them by time and station ID. Write records in batches to the parquet to avoid blocking on IO frequently. Common batch size could be 10K records.

### Deploy using Kubernetes

To deploy all components of the system using Docker and Kubernetes, write Dockerfiles for the central server and weather stations, and a K8s YAML file to incorporate 10 services with images for the weather station, a service with an image for the central server, two services with Kafka and Zookeeper images, and shared storage for writing parquet files and BitCask Riak LSM.

### Profile Central Station using JFR

To profile the central station using JFR, run the system for one minute and report the top 10 classes with the highest total memory, GC pauses count, GC maximum pause duration, and a list of I/O operations.
