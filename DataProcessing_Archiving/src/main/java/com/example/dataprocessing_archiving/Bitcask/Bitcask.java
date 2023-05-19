package com.example.dataprocessing_archiving.Bitcask;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Objects;

//record structure: key valueSize value
//record in hint files: key valueSize valuePos
//keyDir: key --> {fileName, valueSize, valuePos}
public class Bitcask {
    private final String workingDir = "/home/aya/Weather-Stations-Monitoring-main/DataProcessing_Archiving/src/main/java/com/example/Bitcask/";
    private final String directoryPath;
    private final long fileSizeThreshold = 1000L; //1 MB
    private boolean compactionBlocking;
    private final Hashtable<Long, String[]> keyDir;

    public Bitcask(String directoryName) throws Exception {
        this.directoryPath =  this.workingDir + directoryName;
        File dir = new File(directoryPath);
        if(!dir.exists())
        {
            boolean success = dir.mkdir();
            if(!success) throw new Exception("Error making a directory");
        }
        this.compactionBlocking = false;
        this.keyDir = new Hashtable<>();
    }
    public void put(long key, String value) throws IOException {
        if(compactionBlocking)
            throw new RuntimeException("Compaction is replacing files now, try again after few seconds.");
        String fileName = getActiveFile();
        String filePath = this.directoryPath + fileName;
        RandomAccessFile file = new RandomAccessFile(filePath, "rw");
        long offset = appendToFile(file, key, value, value.length(), 0, false);
        //update hashmap
        long valuePos = offset + 2 * Integer.BYTES;
        this.keyDir.put(key, new String[]{fileName, Integer.toString(value.length()), Long.toString(valuePos)});
    }
    public String get(long key) throws Exception {
        if(compactionBlocking)
            throw new RuntimeException("Compaction is replacing files now, try again after few seconds.");
        if(this.keyDir.containsKey(key)){
            String[] values = this.keyDir.get(key);
            String filePath = this.directoryPath + values[0];
            int valueSize = Integer.parseInt(values[1]);
            long valuePos = Long.parseLong(values[2]);
            RandomAccessFile file = new RandomAccessFile(filePath, "r");
            return readFromFile(file, valueSize, valuePos);
        }
        else
            throw new Exception("No such key in keyDir");
    }

    private String readFromFile(RandomAccessFile file, int valueSize, long valuePos) throws IOException {
        file.seek(valuePos);
        byte[] bytes = new byte[valueSize];
        int bytesRead = file.read(bytes);
        if (bytesRead < valueSize) {
            bytes = Arrays.copyOf(bytes, bytesRead);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String getActiveFile()
    {
        File directory = new File(this.directoryPath);
        File[] files = directory.listFiles((dir, name) -> new File(dir, name).isFile() && new File(dir, name).length() < fileSizeThreshold &&
                ! new File(dir, name).getName().contains("hint") && ! new File(dir, name).getName().contains("compacted"));
        if (files != null && files.length > 0) {
            return "/" + files[0].getName();
        } else {
            return createNewFile();
        }

    }
    private long appendToFile(RandomAccessFile file, long key, String value, int valueSize, long valuePos, boolean hintFiles) throws IOException {
        long offset = file.length();
        file.seek(offset);
        file.writeLong(key);
        file.writeInt(valueSize);
        if(hintFiles)
            file.writeLong(valuePos);
        else
            file.writeBytes(value);
        return offset;
    }
    private String createNewFile()
    {
        Instant instant = Instant.now();
        long timestamp = instant.toEpochMilli();
        return "/" + timestamp + ".bin";
    }
    public void compactFiles() throws IOException {
        if(compactionBlocking)
            throw new RuntimeException("Compaction is replacing files now, try again after few seconds.");
        Instant instant = Instant.now();
        long timestamp = instant.toEpochMilli();
        Hashtable<Long, String[]> newKeyDir = new Hashtable<>();
        String newDirectoryPath = this.workingDir + "compactTemp";
        File newDirectory = new File(newDirectoryPath);
        boolean success = newDirectory.mkdir();
        if(!success) throw new IOException();
        File currentDirectory = new File(this.directoryPath);
        String newFileName = createNewFile();
        RandomAccessFile newFile = new RandomAccessFile(newDirectoryPath + newFileName.substring(0, newFileName.lastIndexOf('.')) + "-compacted" + ".bin", "rw");
        String newHintPath = newDirectoryPath + newFileName.substring(0, newFileName.lastIndexOf('.')) + "-hint" + ".bin";
        RandomAccessFile newHint = new RandomAccessFile(newHintPath, "rw");
        File[] files = currentDirectory.listFiles();
        long offsetCurrent;
        if(files != null) {
            for (File f : files) {
                if(!f.getName().contains("hint") && Long.parseLong(f.getName().replace(".bin", "").replace("-compacted", "")) < timestamp) {
                    offsetCurrent = 0;
                    while (offsetCurrent < f.length()) {
                        try(RandomAccessFile currentFile = new RandomAccessFile(f.getAbsolutePath(), "r")) {
                            currentFile.seek(offsetCurrent);
                            long key = currentFile.readLong();
                            int valueSize = currentFile.readInt();
                            long valuePos = offsetCurrent + 2 * Integer.BYTES;
                            if (this.keyDir.containsKey(key)) {
                                String[] values = this.keyDir.get(key);
                                if (Objects.equals(values[0], "/" + f.getName()) && Integer.parseInt(values[1]) == valueSize
                                        && Long.parseLong(values[2]) == valuePos) {
                                    byte[] bytes = new byte[valueSize];
                                    int bytesRead = currentFile.read(bytes);
                                    if (bytesRead < valueSize) {
                                        bytes = Arrays.copyOf(bytes, bytesRead);
                                    }
                                    String val = new String(bytes, StandardCharsets.UTF_8);
                                    long offset = appendToFile(newFile, key, val, val.length(), 0, false);
                                    long newPosition = offset + 2 * Integer.BYTES;
                                    newKeyDir.put(key, new String[]{newFileName, Integer.toString(val.length()), Long.toString(newPosition)});
                                    appendToFile(newHint, key, "", val.length(), newPosition, true);
                                    if (newFile.length() >= fileSizeThreshold) {
                                        newFileName = createNewFile();
                                        newFile = new RandomAccessFile(newDirectoryPath + newFileName.substring(0, newFileName.lastIndexOf('.')) + "-compacted" + ".bin", "rw");
                                        newHintPath = newDirectoryPath + newFileName.substring(0, newFileName.lastIndexOf('.')) + "-hint" + ".bin";
                                        newHint = new RandomAccessFile(newHintPath, "rw");
                                    }
                                }
                            }
                            offsetCurrent += (2 * Integer.BYTES) + valueSize;
                        }
                    }
                }
            }
        }
        //move files to the current directory and remove the compact temp folder
        this.compactionBlocking = true;
        //move new values to the keyDir:
        for(long key: newKeyDir.keySet())
            this.keyDir.put(key, newKeyDir.get(key));
        if(files != null) {
            for (File file : files) {
                if (Long.parseLong(file.getName().replace(".bin", "").replace("-hint", "")
                        .replace("-compacted", "")) < timestamp) {
                    success = file.delete();
                    if (!success) throw new IOException("Error deleting a file");
                }
            }
        }
        File[] newFiles = newDirectory.listFiles();
        if(newFiles != null){
            for (File file : newFiles) {
                Path source = file.toPath();
                Path destination = Paths.get(currentDirectory.getAbsolutePath(), file.getName());
                Files.copy(source, destination);
            }
        }
        if(newFiles != null) {
            for (File file : newFiles) {
                success = file.delete();
                if (!success) throw new IOException("Error deleting a file");
            }
        }
        success = newDirectory.delete();
        if(!success) throw new IOException("Error deleting a file");
        this.compactionBlocking = false;
    }
    public void printHashTable()
    {
        for(long key : keyDir.keySet())
        {
            String[] values = keyDir.get(key);
            System.out.println("key " + key + "values: " + Arrays.toString(values));
        }
    }
    public void reconstructKeyDir() throws IOException {
        if(compactionBlocking)
            throw new RuntimeException("Compaction is replacing files now, try again after few seconds.");
        File currentDir = new File(this.directoryPath);
        File[] files = currentDir.listFiles();
        long offset;
        if(files != null) {
            for (File file : files) {
                if (file.getName().contains("hint")) {
                    offset = 0;
                    try(RandomAccessFile fileAccess = new RandomAccessFile(file, "r")) {
                        while (offset < file.length()) {
                            fileAccess.seek(offset);
                            long key = fileAccess.readInt();
                            int valueSize = fileAccess.readInt();
                            long valuePos = fileAccess.readLong();
                            keyDir.put(key, new String[]{"/" + file.getName().replace("-hint", ""), Integer.toString(valueSize), Long.toString(valuePos)});
                            offset += Long.BYTES + (2 * Integer.BYTES);
                        }
                    }
                }
            }
        }
    }
    public void emptyKeyDir()
    {
        this.keyDir.clear();
    }
    public void readRecordFile(String filePath) throws IOException {
        long offset = 0;
        try(RandomAccessFile file = new RandomAccessFile(this.directoryPath + filePath, "r")) {
            byte[] bytes;
            while (offset < file.length()) {
                file.seek(offset);
                long key = file.readInt();
                int valueSize = file.readInt();
                bytes = new byte[valueSize];
                file.read(bytes);
                System.out.println(key + " : " + valueSize + " , " + new String(bytes, StandardCharsets.UTF_8));
                offset += 2 * Integer.BYTES + valueSize;
            }
        }

    }
    public void readHintFile(String filePath) throws IOException {
        long offset = 0;
        try(RandomAccessFile file = new RandomAccessFile(this.directoryPath + filePath, "r")) {
            while (offset < file.length()) {
                file.seek(offset);
                long key = file.readLong();
                int valueSize = file.readInt();
                long valuePos = file.readLong();
                System.out.println(key + " : " + valueSize + " , " + valuePos);
                offset += 2 * Integer.BYTES + Long.BYTES;
            }
        }
    }
}