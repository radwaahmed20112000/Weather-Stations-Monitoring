package com.example.dataprocessing_archiving.BaseCentralStation;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.List;

public class ParquetManager implements Runnable {

    private final List<Status> statuses;
    private String path;
    private static ParquetWriter<GenericRecord> writer;

    // Define the schema for the Parquet file
    private static final Schema schema = SchemaBuilder.record("StationStatus")
            .fields()
            .name("battery_status").type().stringType().noDefault()
            .name("status_timestamp").type().longType().noDefault()
            .name("weather_humidity").type().intType().noDefault()
            .name("weather_temperature").type().intType().noDefault()
            .name("weather_wind_speed").type().intType().noDefault()
            .endRecord();


    public ParquetManager(List<Status> statuses) {
        this.statuses = statuses;
        this.path = "";
    }

    private String generatePath(Status parsedMessage) {
        Instant instant = Instant.ofEpochSecond(parsedMessage.getStatusTimestamp());
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        int year = dateTime.getYear();
        String month = Month.of(dateTime.getMonthValue()).toString();
        int week = dateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) % 4;

        return "Station" + parsedMessage.getStationID() + "/" + year + "/" + month + "/" + week + ".parquet";
    }

    private void updateWriter(String generatedPath) throws IOException {
        this.path = generatedPath;
        Utils.pathCheck(path);
        CompressionCodecName codec = CompressionCodecName.SNAPPY;
        if(writer != null) writer.close();
        writer = AvroParquetWriter.<GenericRecord>builder(new Path(path))
                .withSchema(schema)
                .withCompressionCodec(codec)
                .withDataModel(GenericData.get())
                .build();
    }

    public void run() {
        try {
            GenericRecord record = new GenericData.Record(schema);

            for (Status status : statuses) {

                String generatedPath = generatePath(status);
                if(generatedPath.compareTo(this.path) != 0) {
                    updateWriter(generatedPath);
                    System.out.println("hi");
                }
                record.put("battery_status", status.getBatteryStatus());
                record.put("status_timestamp", status.getStatusTimestamp());
                record.put("weather_humidity", status.getHumidity());
                record.put("weather_temperature", status.getTemperature());
                record.put("weather_wind_speed", status.getWindSpeed());

                // write data
                writer.write(record);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


