package no.responseweb.imagearchive.filewalkerservice;

import com.sun.nio.file.ExtendedWatchEventModifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
// @Component
public class FilePathWatcher implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        log.info("Starting FilePathWatcher");
        String baseFileStore = "//storage6000/Share/Test";
        testOfWatchService(baseFileStore);

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
