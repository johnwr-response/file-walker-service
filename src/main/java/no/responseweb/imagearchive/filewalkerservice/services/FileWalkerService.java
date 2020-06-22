package no.responseweb.imagearchive.filewalkerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.responseweb.imagearchive.filewalkerservice.config.JmsConfig;
import no.responseweb.imagearchive.model.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "response.filestore", ignoreUnknownFields = false)
public class FileWalkerService {

    private String pathNickname;
    private String overrideWalkPath;
    private final FileInfoService fileInfoService;
    private final JmsTemplate jmsTemplate;

    // TODO: Reduce Cognitive Complexity
    @Scheduled(fixedDelayString = "${response.scheduling.fixed-delay-time}", initialDelayString = "${response.scheduling.initial-delay-time}")
    public void walk() throws IOException {
        log.info("System: {}", System.getProperty("os.name"));
        log.info("Scheduled walk started on path: {}", pathNickname);
        FileStoreDto fileStoreDto = fileInfoService.getFileStore(pathNickname);
        log.info("FileStore configured to walk: {}", fileStoreDto);
        Set<String> fileList = new HashSet<>();
        Map<Path,List<FileItemDto>> testmap = new HashMap<>();
        File fPath = new File(fileStoreDto.getLocalBaseUri());
        if (!overrideWalkPath.isEmpty()) {
            fPath = new File(overrideWalkPath + File.separator + fileStoreDto.getMountPoint() + File.separator + fileStoreDto.getBaseFolder());
            log.info("Walk Path Overridden: {}", fPath.toString());
        }
        log.info("fPath is directory: {}", fPath.isDirectory());
        Path rootPath = fPath.toPath();
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

                List<FileItemDto> filesAtPath = new ArrayList<>();
                if (filePathDto!=null) {
                    filesAtPath = fileInfoService.getFilesAtPath(filePathDto.getId());
                }
                testmap.put(dir,filesAtPath);
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

                if (filePathDto != null) {
                    List<FileItemDto> fileItems = testmap.get(dir).stream()
                            .filter(fileItem -> !fileItem.isLocallyVisited())
                            .collect(Collectors.toList());

                    fileItems.forEach(fileItem -> {
                        log.info("File is removed: {}", fileItem);
                        jmsTemplate.convertAndSend(JmsConfig.FILE_STORE_REQUEST_QUEUE, FileStoreRequestDto.builder()
                                .fileStoreRequestType(FileStoreRequestTypeDto.DELETE)
                                .fileStore(fileStoreDto)
                                .filePath(filePathDto)
                                .fileItem(fileItem)
                                .build());
                    });

                }

                testmap.remove(dir);
                return super.postVisitDirectory(dir, exc);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file)) {

                    Path relativePath = rootPath.relativize(file.getParent());
                    FilePathDto filePathDto;
                    if (rootPath.equals(file.getParent())) {
                        filePathDto = fileInfoService.getRootFilePath(fileStoreDto.getId());
                    } else {
                        filePathDto = fileInfoService.getStoreRelativePath(fileStoreDto.getId(),relativePath.toString());
                    }
                    if (filePathDto == null) {
                        filePathDto = FilePathDto.builder()
                                .fileStoreId(fileStoreDto.getId())
                                .relativePath(relativePath.toString())
                                .build();
                    }

                    BasicFileAttributes fileAttr = Files.getFileAttributeView(file, BasicFileAttributeView.class).readAttributes();

                    List<FileItemDto> fileItems = testmap.get(file.getParent());

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
                                .fileStore(fileStoreDto)
                                .filePath(filePathDto)
                                .fileItem(FileItemDto.builder()
                                        .fileStorePathId(filePathDto.getId())
                                        .filename(file.getFileName().toString())
                                        .createdDate(LocalDateTime.ofInstant(fileAttr.creationTime().toInstant(), ZoneOffset.UTC))
                                        .lastModifiedDate(LocalDateTime.ofInstant(fileAttr.lastModifiedTime().toInstant(), ZoneOffset.UTC))
                                        .size(fileAttr.size())
                                        .build())
                                .build());
                    } else {
                        log.info("File found in store: {}", fromStore);
                        FileTime created = fileAttr.creationTime();
                        if (created.toInstant().toEpochMilli() != fromStore.getCreatedDate().toInstant(ZoneOffset.UTC).toEpochMilli()) {
                            fromStore.setCreatedDate(LocalDateTime.ofInstant(created.toInstant(), ZoneOffset.UTC));
//                            log.info("File.created: {} File.dto: {}", created.toString(), fromStore.getCreatedDate().toString());
                            fromStore.setLocallyChanged(true);
                        }
                        FileTime modified = fileAttr.lastModifiedTime();
                        if (modified.toInstant().toEpochMilli() != fromStore.getLastModifiedDate().toInstant(ZoneOffset.UTC).toEpochMilli()) {
                            fromStore.setLastModifiedDate(LocalDateTime.ofInstant(modified.toInstant(), ZoneOffset.UTC));
//                            log.info("File.modified: {} toInstant {}, File.dto: {} toInstant {}", modified.toString(), modified.toInstant().toEpochMilli(), fromStore.getLastModifiedDate().toString(), fromStore.getLastModifiedDate().toInstant().toEpochMilli());
                            fromStore.setLocallyChanged(true);
                        }
                        long size = fileAttr.size();
                        if (fromStore.getSize()==null || size!=fromStore.getSize()) {
                            fromStore.setSize(size);
                            fromStore.setLocallyChanged(true);
                        }
                        if (fromStore.isLocallyChanged()) {
                            jmsTemplate.convertAndSend(JmsConfig.FILE_STORE_REQUEST_QUEUE, FileStoreRequestDto.builder()
                                    .fileStoreRequestType(FileStoreRequestTypeDto.UPDATE)
                                    .fileStore(fileStoreDto)
                                    .filePath(filePathDto)
                                    .fileItem(fromStore)
                                    .build());
                        }
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
