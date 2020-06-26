package no.responseweb.imagearchive.filewalkerservice;

import com.sun.nio.file.ExtendedWatchEventModifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@SpringBootApplication
@EnableFeignClients
public class FileWalkerServiceApplication {

	public static final String FETCH_PATH = "api/v1/fetchFile/{fileItemId}";
	private static UUID appId;

	public static void main(String[] args) throws IOException, InterruptedException {
		appId = UUID.randomUUID();
		SpringApplication.run(FileWalkerServiceApplication.class, args);
	}

	public static UUID getAppId() {
		return appId;
	}

	private static void testOfWatchService(String basePath) throws IOException, InterruptedException {
		WatchService watchService = FileSystems.getDefault().newWatchService();
		Path path = Paths.get(basePath);
		path.register(watchService, new WatchEvent.Kind[] {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE}, ExtendedWatchEventModifier.FILE_TREE);
		WatchKey key;
		while ((key = watchService.take()) != null) {
			for (WatchEvent<?> event : key.pollEvents()) {
				log.debug("Event kind: {}, File affected: {}.", event.kind(), event.context());
			}
			key.reset();
		}
		watchService.close();
	}

}


