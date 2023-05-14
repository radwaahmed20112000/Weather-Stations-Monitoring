package com.example.dataprocessing_archiving.Bitcask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class BitcaskApplication {
	private static Bitcask bitcask;
	private int i = 0;

	static {
		try {
			bitcask = new Bitcask("test");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(BitcaskApplication.class, args);
		bitcask.readRecordFile("/1683980420552-compacted.bin");
//		bitcask.readHintFile("/1683979058638-hint.bin");
//		bitcask.readRecordFile("/1683979058677.bin");
//		for(int i=0; i<1000; i++)
//		{
//			bitcask.put(i % 10, "hi i'm value " + i);
//		}
//		for(int i=0; i<100; i++)
//		{
//			bitcask.put(i % 5, "hi i'm value " + i);
//		}
//		bitcask.printHashMap();
//		System.out.println(bitcask.get(9));
//		bitcask.compactFiles();
//		bitcask.printHashMap();
//		bitcask.emptyKeyDir();
//		System.out.println("reconstructed: ");
//		bitcask.reconstructKeyDir();
//		bitcask.printHashMap();
	}
//	do compaction every one minutes
//	@Scheduled( initialDelay = 30 * 1000, fixedDelay = 30 * 1000)
//	public void compaction() throws IOException {
//		System.out.println("compaction is in process");
//		bitcask.compactFiles();
//		bitcask.printHashTable();
//		System.out.println("compaction is done");
//	}
//	@Scheduled(fixedDelay = 100)
//	public void putInDir() throws IOException {
//		bitcask.put(i % 10, "hi i'm value " + i);
//		i++;
//	}
}
