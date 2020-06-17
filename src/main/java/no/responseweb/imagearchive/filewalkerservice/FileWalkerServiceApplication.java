package no.responseweb.imagearchive.filewalkerservice;

import com.sun.nio.file.ExtendedWatchEventModifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@SpringBootApplication
public class FileWalkerServiceApplication {

	public static void main(String[] args) throws IOException, InterruptedException {
		SpringApplication.run(FileWalkerServiceApplication.class, args);
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
