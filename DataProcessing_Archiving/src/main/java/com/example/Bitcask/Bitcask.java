package com.example.Bitcask;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Objects;
import java.util.concurrent.locks.*;


// record structure: key valueSize timestamp value
// record in hint files: key valueSize timestamp valuePos
// keyDir: key --> {fileName, valueSize, timestamp,  valuePos}
public class Bitcask {
    private final String workingDir;
    private final String directoryPath;
    private final long fileSizeThreshold = 1000000L; // 1 MB
    private final Hashtable<Long, String[]> keyDir;
    private final ReentrantReadWriteLock compactionLock;

    public Bitcask(String workingDir, String directoryName) throws Exception {
        this.workingDir = workingDir;
        this.directoryPath =  this.workingDir + directoryName;
        this.compactionLock = new ReentrantReadWriteLock();
        File dir = new File(directoryPath);
        if(!dir.exists())
        {
            boolean success = dir.mkdirs();
            if(!success) throw new Exception("Error making a directory");
        }
        this.keyDir = new Hashtable<>();
    }
    public void put(long key, String value) throws IOException {
        compactionLock.writeLock().lock();
        try {
            String fileName = getActiveFile() +".bin";
            String filePath = this.directoryPath + fileName;
            RandomAccessFile file = new RandomAccessFile(filePath, "rw");
            long timestamp = Instant.now().toEpochMilli();
            long offset = appendToFile(file, key, value, value.length(), timestamp, 0, false);
            //update hashmap
            long valuePos = offset + Integer.BYTES + 2 * Long.BYTES;
            this.keyDir.put(key, new String[]{fileName, Integer.toString(value.length()), Long.toString(timestamp), Long.toString(valuePos)});
        } finally{
            compactionLock.writeLock().unlock();
        }
    }
    public String get(long key) throws Exception {
        compactionLock.readLock().lock();
        long valuePos;
        int valueSize;
        RandomAccessFile file;
        String value;
        try {
            if (this.keyDir.containsKey(key)) {
                String[] values = this.keyDir.get(key);
                String filePath = this.directoryPath + values[0];
                valueSize = Integer.parseInt(values[1]);
                valuePos = Long.parseLong(values[3]);
                file = new RandomAccessFile(filePath, "r");
                value = readFromFile(file, valueSize, valuePos);
            } else
                throw new Exception("No such key in keyDir");
        } finally{
            compactionLock.readLock().unlock();
        }
        return value;
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
            return "/" + files[0].getName().replace(".bin", "");
        } else {
            return createNewFile();
        }

    }
    private long appendToFile(RandomAccessFile file, long key, String value, int valueSize, long timestamp, long valuePos, boolean hintFiles) throws IOException {
        long offset = file.length();
        file.seek(offset);
        file.writeLong(key);
        file.writeInt(valueSize);
        file.writeLong(timestamp);
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
        return "/" + timestamp;
    }
    public void compactFiles() throws IOException {
        long timestamp = Instant.now().toEpochMilli();
        Hashtable<Long, String[]> newKeyDir = new Hashtable<>();
        String newDirectoryPath = this.workingDir + "compactTemp";
        File newDirectory = new File(newDirectoryPath);
        boolean success = newDirectory.mkdir();
        if(!success) throw new IOException("Couldn't create compact directory");
        File currentDirectory = new File(this.directoryPath);
        String createdFileName = createNewFile();
        String newFileName = createdFileName + "-compacted" + ".bin";
        RandomAccessFile newFile = new RandomAccessFile(newDirectoryPath + newFileName , "rw");
        String newHintPath = newDirectoryPath + createdFileName + "-hint" + ".bin";
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
                            long readTimestamp = currentFile.readLong();
                            long valuePos = offsetCurrent + Integer.BYTES + 2 * Long.BYTES;
                            if (this.keyDir.containsKey(key)) {
                                String[] values = this.keyDir.get(key);
                                if (Objects.equals(values[0], "/" + f.getName()) && Integer.parseInt(values[1]) == valueSize
                                        && Long.parseLong(values[2]) == readTimestamp && Long.parseLong(values[3]) == valuePos ) {
                                    byte[] bytes = new byte[valueSize];
                                    int bytesRead = currentFile.read(bytes);
                                    if (bytesRead < valueSize) {
                                        bytes = Arrays.copyOf(bytes, bytesRead);
                                    }
                                    String val = new String(bytes, StandardCharsets.UTF_8);
                                    long timestampCompact = Instant.now().toEpochMilli();
                                    long offset = appendToFile(newFile, key, val, val.length(), timestampCompact, 0, false);
                                    long newPosition = offset + Integer.BYTES + 2 * Long.BYTES;
                                    newKeyDir.put(key, new String[]{newFileName, Integer.toString(val.length()), Long.toString(timestampCompact), Long.toString(newPosition)});
                                    appendToFile(newHint, key, "", val.length(), timestampCompact, newPosition, true);
                                    if (newFile.length() >= fileSizeThreshold) {
                                        createdFileName = createNewFile();
                                        newFileName = createdFileName + "-compacted" + ".bin";
                                        newFile = new RandomAccessFile(newDirectoryPath + newFileName , "rw");
                                        newHintPath = newDirectoryPath + createdFileName + "-hint" + ".bin";
                                        newHint = new RandomAccessFile(newHintPath, "rw");
                                    }
                                }
                            }
                            offsetCurrent += (Integer.BYTES + 2 * Long.BYTES) + valueSize;
                        }
                    }
                }
            }
        }
        compactionLock.writeLock().lock();
        try {
            //move files to the current directory and remove the compact temp folder
            //move new values to the keyDir:
            for (long key : newKeyDir.keySet())
            {
                if(this.keyDir.contains(key))
                {
//                    long timestampOfNew = Long.parseLong(newKeyDir.get(key)[2]);
                    long timestampOfOld = Long.parseLong(this.keyDir.get(key)[2]);
                    if(timestampOfOld < timestamp)
                        this.keyDir.put(key, newKeyDir.get(key));
                }
                else
                    this.keyDir.put(key, newKeyDir.get(key));
            }
            if (files != null) {
                for (File file : files) {
                    if (Long.parseLong(file.getName().replace(".bin", "").replace("-hint", "")
                            .replace("-compacted", "")) < timestamp) {
                        success = file.delete();
                        if (!success) throw new IOException("Error deleting a file");
                    }
                }
            }
            File[] newFiles = newDirectory.listFiles();
            if (newFiles != null) {
                for (File file : newFiles) {
                    Path source = file.toPath();
                    Path destination = Paths.get(currentDirectory.getAbsolutePath(), file.getName());
                    Files.copy(source, destination);
                }
            }
            if (newFiles != null) {
                for (File file : newFiles) {
                    success = file.delete();
                    if (!success) throw new IOException("Error deleting a file");
                }
            }
            success = newDirectory.delete();
            if (!success) throw new IOException("Error deleting a file");
        } finally {
//            System.out.println("replace ended");
            compactionLock.writeLock().unlock();
        }
    }
    public void printHashTable()
    {
        for(long key : keyDir.keySet())
        {
            String[] values = keyDir.get(key);
//            System.out.println("key " + key + "values: " + Arrays.toString(values));
        }
    }
    public void reconstructKeyDir() throws IOException {
        compactionLock.writeLock().lock();
        this.keyDir.clear();
        try {
            File currentDir = new File(this.directoryPath);
            File[] files = currentDir.listFiles();
            long offset;
            if (files != null) {
                for (File file : files) {
                    if (!file.getName().contains("compacted")) {
                        offset = 0;
                        try (RandomAccessFile fileAccess = new RandomAccessFile(file, "r")) {
                            while (offset < file.length()) {
                                fileAccess.seek(offset);
                                long key = fileAccess.readLong();
                                int valueSize = fileAccess.readInt();
                                long timestamp = fileAccess.readLong();
                                if (file.getName().contains("hint"))
                                {
                                    long valuePos = fileAccess.readLong();
                                    keyDir.put(key, new String[]{"/" + file.getName().replace("-hint", "-compacted"), Integer.toString(valueSize), Long.toString(timestamp), Long.toString(valuePos)});
                                    offset += 3 * Long.BYTES + Integer.BYTES;
                                }
                                else
                                {
                                    keyDir.put(key, new String[]{"/" + file.getName(), Integer.toString(valueSize), Long.toString(timestamp), Long.toString(offset + 2 * Long.BYTES + Integer.BYTES)});
                                    offset += 2 * Long.BYTES + Integer.BYTES + valueSize;
                                }
                            }
                        }
                    }
                }
            }
        } finally{
            compactionLock.writeLock().unlock();
        }
    }
    //helper functions
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
                long key = file.readLong();
                int valueSize = file.readInt();
                long timestamp = file.readLong();
                bytes = new byte[valueSize];
                file.read(bytes);
//                System.out.println(key + " : " + valueSize + " " + timestamp + " , " +  new String(bytes, StandardCharsets.UTF_8));
                offset += Integer.BYTES + 2 * Long.BYTES + valueSize;
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
                long timestamp = file.readLong();
                long valuePos = file.readLong();
//                System.out.println(key + " : " + valueSize + " " +  timestamp + " , " + valuePos);
                offset += Integer.BYTES + 3 * Long.BYTES;
            }
        }
    }
}