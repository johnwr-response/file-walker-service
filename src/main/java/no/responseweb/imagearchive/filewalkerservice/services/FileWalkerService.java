package no.responseweb.imagearchive.filewalkerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.responseweb.imagearchive.filewalkerservice.config.JmsConfig;
import no.responseweb.imagearchive.filewalkerservice.services.model.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "response.filestore", ignoreUnknownFields = false)
public class FileWalkerService {

    private String pathNickname;
    private final FileInfoService fileInfoService;
    private final JmsTemplate jmsTemplate;

    @Scheduled(fixedDelayString = "${response.scheduling.fixed-delay-time}", initialDelayString = "${response.scheduling.initial-delay-time}")
    public void walk() throws IOException {
        log.info("Scheduled walk started on path: {}", pathNickname);
        FileStoreDto fileStoreDto = fileInfoService.getFileStore(pathNickname);
        Path rootPath = Paths.get(fileStoreDto.getBaseUri());
        log.info("FileStore configured to walk: {}", fileStoreDto);
        Set<String> fileList = new HashSet<>();
        Map<UUID,List<FileItemDto>> testmap = new HashMap<>();
        File fPath = new File(fileStoreDto.getBaseUri());
        Files.walkFileTree(fPath.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                log.info("Checking = {}", dir.toRealPath());

                FilePathDto filePathDto;
                if (rootPath.equals(dir)) {
                    filePathDto = fileInfoService.getRootFilePath(fileStoreDto.getId());
                } else {
                    filePathDto = fileInfoService.getStoreRelativePath(fileStoreDto.getId(),rootPath.relativize(dir).toString());
                }
                log.info("Current FilePath: {}", filePathDto);

                if (filePathDto!=null) {
                    List<FileItemDto> filesAtPath = fileInfoService.getFilesAtPath(filePathDto.getId());
                    testmap.put(filePathDto.getId(),filesAtPath);
                }
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                log.info("Leaving = {}", dir.toRealPath());
                FilePathDto filePathDto;
                if (rootPath.equals(dir)) {
                    filePathDto = fileInfoService.getRootFilePath(fileStoreDto.getId());
                } else {
                    filePathDto = fileInfoService.getStoreRelativePath(fileStoreDto.getId(),rootPath.relativize(dir).toString());
                }
                log.info("Leaving this filePath: {}", filePathDto);

                List<FileItemDto> fileItems = testmap.get(filePathDto.getId()).stream()
                        .filter(fileItem -> !fileItem.isLocallyVisited())
                        .collect(Collectors.toList());

                fileItems.forEach(fileItem -> {
                    log.info("File is removed: {}", fileItem);
                    jmsTemplate.convertAndSend(JmsConfig.FILE_STORE_REQUEST_QUEUE, FileStoreRequestDto.builder()
                            .fileStoreRequestType(FileStoreRequestTypeDto.DELETE)
                            .fileItem(fileItem)
                            .build());
                });

                testmap.remove(filePathDto.getId());
                return super.postVisitDirectory(dir, exc);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file)) {

                    FilePathDto filePathDto;
                    if (rootPath.equals(file.getParent())) {
                        filePathDto = fileInfoService.getRootFilePath(fileStoreDto.getId());
                    } else {
                        filePathDto = fileInfoService.getStoreRelativePath(fileStoreDto.getId(),rootPath.relativize(file.getParent()).toString());
                    }

                    List<FileItemDto> fileItems = testmap.get(filePathDto.getId());
                    FileItemDto fromStore = fileItems.stream()
                            .filter(fileItem -> file.getFileName().toString().equals(fileItem.getFilename()))
                            .findAny()
                            .map(fileItem -> {
                                fileItem.setLocallyVisited(true);
                                return fileItem;
                            })
                            .orElse(null);
                    if (fromStore==null) {
                        log.info("New file: {}", file.getFileName());
                        jmsTemplate.convertAndSend(JmsConfig.FILE_STORE_REQUEST_QUEUE, FileStoreRequestDto.builder()
                                .fileStoreRequestType(FileStoreRequestTypeDto.INSERT)
                                .fileItem(FileItemDto.builder()
                                        .fileStorePathId(filePathDto.getId())
                                        .filename(file.getFileName().toString())
                                        .build())
                                .build());
                    } else {
                        log.info("File found in store: {}", fromStore);
                        // TODO: Detect changes and add change-message on file-store-queue
                    }
                    // check
                    fileList.add(file.getFileName().toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        log.info("Total files: {}", fileList.size());

    }

}
