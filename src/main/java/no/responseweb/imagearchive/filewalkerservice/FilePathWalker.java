package no.responseweb.imagearchive.filewalkerservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "sfg.brewery", ignoreUnknownFields = false)
// @Component
public class FilePathWalker implements CommandLineRunner {

    private String host;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting FilePathWalker");
        String baseFileStore = "//storage10000/Share/Photo";
        testOfWalkService(baseFileStore);
        log.info("Done FilePathWalker");
    }
    private static void testOfWalkService(String basePath) throws IOException, InterruptedException {
        Set<String> fileList = new HashSet<>();
        Files.walkFileTree(Paths.get(basePath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (!Files.isDirectory(file)) {
                    fileList.add(file.getFileName().toString());
//                    log.info("File: {} ({})", file.getFileName(), file.toRealPath());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        log.info("Total files: {}", fileList.size());
    }


    public void setHost(String host) {
        this.host = host;
    }
}
