package com.example.Bitcask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.io.IOException;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BitcaskApplication {
	private static Bitcask bitcask;
	private int i = 0;
	static {
		try {
			bitcask = new Bitcask("/home/aya/Weather-Stations-Monitoring/DataProcessing_Archiving/src/main/java/com/example/Bitcask/", "test");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(BitcaskApplication.class, args);
//		bitcask.reconstructKeyDir();
	}
	@Scheduled( initialDelay = 15 * 1000, fixedDelay = 30 * 1000)
	public void compaction() throws IOException {
		System.out.println("compaction is in process");
		bitcask.compactFiles();
		bitcask.printHashTable();
		System.out.println("compaction is done");
	}
//	@Scheduled(fixedDelay = 100)
//	public void putInDir() throws Exception {
//		bitcask.put(i % 10, "hi i'm value " + i);
//		i++;
//	}
	@Scheduled(initialDelay = 15 * 1000, fixedDelay = 100)
	public void getValue() throws Exception {
		System.out.println(bitcask.get(i % 10));
		i++;
	}
}
