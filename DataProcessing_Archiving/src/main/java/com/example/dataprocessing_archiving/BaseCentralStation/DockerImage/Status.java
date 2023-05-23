package com.example.dataprocessing_archiving.BaseCentralStation.DockerImage;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

public class Status {

    private final long stationID;
    private final String batteryStatus;
    private final long statusTimestamp;
    private final int humidity;
    private final int temperature;
    private final int windSpeed;

    public Status(long stationID, String batteryStatus, long statusTimestamp, int humidity,
                          int temperature, int windSpeed) {
        this.stationID = stationID;
        this.batteryStatus = batteryStatus;
        this.statusTimestamp = statusTimestamp;
        this.humidity = humidity;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
    }

    public long getStationID() {
        return stationID;
    }

    public String getBatteryStatus() {
        return batteryStatus;
    }

    public long getStatusTimestamp() {
        return statusTimestamp;
    }

    public int getHumidity() {
        return humidity;
    }

    public int getTemperature() {
        return temperature;
    }

    public int getWindSpeed() {
        return windSpeed;
    }
}