package com.example.BaseCentralStation.DockerImage;
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
import java.util.HashMap;
import java.util.List;

public class ParquetManager implements Runnable {

    private List<Status> statuses;
    private static final HashMap<Long, String> paths = new HashMap<>();
    private static final HashMap<Long, ParquetWriter<GenericRecord>> writers = new HashMap<>();

    // Define the schema for the Parquet file
    private static final Schema schema = SchemaBuilder.record("StationStatus")
            .fields()
            .name("battery_status").type().stringType().noDefault()
            .name("status_timestamp").type().longType().noDefault()
            .name("weather_humidity").type().intType().noDefault()
            .name("weather_temperature").type().intType().noDefault()
            .name("weather_wind_speed").type().intType().noDefault()
            .endRecord();


    private String generatePath(Status parsedMessage) {
        Instant instant = Instant.ofEpochSecond(parsedMessage.getStatusTimestamp());
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        int year = dateTime.getYear();
        String month = Month.of(dateTime.getMonthValue()).toString();
        int week = dateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) % 4;

        return "Station" + parsedMessage.getStationID() + "/" + year + "/" + month + "/" + week + ".parquet";
    }

    private void updateWriter(String generatedPath, long stationID) throws IOException {
        paths.put(stationID, generatedPath);
        Utils.pathCheck(generatedPath);
        CompressionCodecName codec = CompressionCodecName.SNAPPY;
        if(writers.get(stationID) != null) writers.get(stationID).close();
        writers.put(stationID, AvroParquetWriter.<GenericRecord>builder(new Path(generatedPath))
                .withSchema(schema)
                .withCompressionCodec(codec)
                .withDataModel(GenericData.get())
                .build());
    }

    public void run() {
        try {
            GenericRecord record = new GenericData.Record(schema);

            for (Status status : statuses) {

                long stationID = status.getStationID();
                String generatedPath = generatePath(status);
                if(generatedPath.compareTo(paths.get(stationID)) != 0)
                    updateWriter(generatedPath, stationID);

                record.put("battery_status", status.getBatteryStatus());
                record.put("status_timestamp", status.getStatusTimestamp());
                record.put("weather_humidity", status.getHumidity());
                record.put("weather_temperature", status.getTemperature());
                record.put("weather_wind_speed", status.getWindSpeed());

                // write data
                writers.get(stationID).write(record);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setStatuses(List<Status> statuses) {
        this.statuses = statuses;
    }
}


