package no.responseweb.imagearchive.filewalkerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "response.filestore", ignoreUnknownFields = false)
public class FileWalkerService {

    private String walkPath;

    @Scheduled(fixedDelayString = "${response.scheduling.fixed-delay-time}", initialDelayString = "${response.scheduling.initial-delay-time}")
    public void walk() throws IOException {
        log.info("Scheduled walk started on path: {}", Paths.get(walkPath));
        Path pathToWalk = Paths.get(walkPath);
        Set<String> fileList = new HashSet<>();
        Set<String> currFolderFileList = new HashSet<>();
        File fPath = new File(walkPath);
        Files.walkFileTree(fPath.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                log.info("currPath = {}", dir.toRealPath());
                currFolderFileList.clear();
                // TODO: Reach out to file-info-service, get list of all files registered on this folder
                currFolderFileList.add(dir.toString());
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // TODO: Loop through currFolderFileList where not marked as locally visited and add remove-message on file-store-queue
                return super.postVisitDirectory(dir, exc);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file)) {
                    fileList.add(file.getFileName().toString());
                    // TODO: If present in currFolderFileList and equal to object. Mark file as locally visited
                    // TODO: If present in currFolderFileList and object changed. Mark file as locally visited and add change-message on file-store-queue
                    // TODO: If not present in currFolderFileList and equal to object. Add create-message on file-store-queue
                    log.info("Check File: {} ({}). Compare changes from currFolderFileList: {}", file.getFileName(), file.toRealPath(), currFolderFileList.toArray()[0]);
                    // check
                }
                return FileVisitResult.CONTINUE;
            }
        });
        log.info("Total files: {}", fileList.size());

    }

    public void setWalkPath(String walkPath) {
        this.walkPath = walkPath;
    }
}
