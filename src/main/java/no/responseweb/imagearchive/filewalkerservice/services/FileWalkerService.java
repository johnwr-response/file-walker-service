package no.responseweb.imagearchive.filewalkerservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.responseweb.imagearchive.filewalkerservice.FileWalkerServiceApplication;
import no.responseweb.imagearchive.filewalkerservice.config.JmsConfig;
import no.responseweb.imagearchive.filewalkerservice.config.ResponseFilestoreProperties;
import no.responseweb.imagearchive.filewalkerservice.services.pathcachemodel.FileStorePathCache;
import no.responseweb.imagearchive.filewalkerservice.services.pathcachemodel.FileStorePathCacheItem;
import no.responseweb.imagearchive.model.*;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.ZoneId.systemDefault;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileWalkerService {

    private static final String FOLDER_CACHE_FILE_NAME = ".fileStore";

    private final FileInfoService fileInfoService;
    private final JmsTemplate jmsTemplate;
    private final ResponseFilestoreProperties responseFilestoreProperties;

    // TODO: Reduce Cognitive Complexity
    @JmsListener(destination = JmsConfig.FILE_STORE_WALKER_JOB_TOPIC, containerFactory = "jmsTopicListenerContainerFactory")
    public void walkFileStore(WalkerJobDto walkerJobDto) throws IOException {
        log.info("WalkerJob: {}", walkerJobDto);
        if (walkerJobDto.getWalkerInstanceToken().compareTo(FileWalkerServiceApplication.getAppId())==0) {
            log.info("System: {}", System.getProperty("os.name"));
            ZoneOffset currentSystemZoneOffset = systemDefault().getRules().getOffset(Instant.now());
            log.info("Current System Zone offset: {}", currentSystemZoneOffset);
            FileStoreDto fileStoreDto = walkerJobDto.getFileStoreDto();
            log.info("Scheduled walk started on file-store: {}", fileStoreDto);
            Set<String> fileList = new HashSet<>();
            Map<Path,List<FileItemDto>> fileItemCacheMap = new HashMap<>();
            Map<Path,FileStorePathCache> folderItemsCacheMap = new HashMap<>();
            File fPath = new File(fileStoreDto.getLocalBaseUri());
            if (!responseFilestoreProperties.getOverrideWalkPath().isEmpty()) {
                fPath = new File(responseFilestoreProperties.getOverrideWalkPath() + File.separator + fileStoreDto.getMountPoint() + File.separator + fileStoreDto.getBaseFolder());
                log.info("Walk Path Overridden: {}", fPath.toString());
            }
            log.info("fPath is directory: {}", fPath.isDirectory());
            Path rootPath = fPath.toPath();
            Files.walkFileTree(fPath.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    log.info("Checking = {}", dir.toRealPath());

                    Path confFile = new File(dir+File.separator+FOLDER_CACHE_FILE_NAME).toPath();
//                    FileStorePathCache currentCachedConfigFile = readFileStoreConfigFile(confFile);
                    folderItemsCacheMap.put(dir,updateContentFile(readFileStoreConfigFile(confFile),fileStoreDto));

                    FilePathDto filePathDto;
                    if (rootPath.equals(dir)) {
                        filePathDto = fileInfoService.getRootFilePath(fileStoreDto.getId());
                    } else {
                        filePathDto = fileInfoService.getStoreRelativePath(fileStoreDto.getId(),rootPath.relativize(dir).toString());
                    }
//                    if (filePathDto.getId()==null) {
//                        filePathDto.setId(UUID.fromString(folderItemsCacheMap.get(dir).getFilePathId()));
//                    }
                    log.info("Current FilePath: {}", filePathDto);
                    folderItemsCacheMap.put(dir,updateContentFile(folderItemsCacheMap.get(dir),filePathDto));
                    logFileStoreConfigFile(folderItemsCacheMap.get(dir));

                    List<FileItemDto> filesAtPath = new ArrayList<>();
                    if (filePathDto!=null) {
                        filesAtPath = fileInfoService.getFilesAtPath(filePathDto.getId());
                    }
                    fileItemCacheMap.put(dir,filesAtPath);
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
                        List<FileItemDto> fileItems = fileItemCacheMap.get(dir).stream()
                                .filter(fileItem -> !fileItem.isLocallyVisited())
                                .collect(Collectors.toList());

                        fileItems.forEach(fileItem -> {
                            log.info("File is removed: {}", fileItem);
                            jmsTemplate.convertAndSend(JmsConfig.FILE_STORE_REQUEST_QUEUE, FileStoreRequestDto.builder()
                                    .fileStoreRequestType(FileStoreRequestTypeDto.DELETE)
                                    .walkerJobDto(walkerJobDto)
                                    .fileStore(fileStoreDto)
                                    .filePath(filePathDto)
                                    .fileItem(fileItem)
                                    .build());
                        });

                    }

                    fileItemCacheMap.remove(dir);

                    saveFileStoreConfigFile(dir,folderItemsCacheMap.get(dir));

                    return super.postVisitDirectory(dir, exc);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if ((!Files.isDirectory(file)) && (!file.getFileName().toString().equalsIgnoreCase(FOLDER_CACHE_FILE_NAME))) {

                        Path relativePath = rootPath.relativize(file.getParent());
                        FilePathDto filePathDto;
                        if (rootPath.equals(file.getParent())) {
                            filePathDto = fileInfoService.getRootFilePath(fileStoreDto.getId());
                        } else {
                            filePathDto = fileInfoService.getStoreRelativePath(fileStoreDto.getId(),relativePath.toString());
                        }
                        if (filePathDto == null) {
                            filePathDto = FilePathDto.builder()
//                                    .id(UUID.fromString(folderItemsCacheMap.get(file.getParent()).getFilePathId()))
                                    .fileStoreId(fileStoreDto.getId())
                                    .relativePath(relativePath.toString())
                                    .build();
                        }

                        BasicFileAttributes fileAttr = Files.getFileAttributeView(file, BasicFileAttributeView.class).readAttributes();

                        List<FileItemDto> fileItems = fileItemCacheMap.get(file.getParent());

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
                            fromStore = FileItemDto.builder()
                                    .fileStorePathId(filePathDto.getId())
                                    .filename(file.getFileName().toString())
                                    .createdDate(LocalDateTime.ofInstant(fileAttr.creationTime().toInstant().truncatedTo(ChronoUnit.SECONDS), currentSystemZoneOffset))
                                    .lastModifiedDate(LocalDateTime.ofInstant(fileAttr.lastModifiedTime().toInstant().truncatedTo(ChronoUnit.SECONDS), currentSystemZoneOffset))
                                    .size(fileAttr.size())
                                    .build();
                            jmsTemplate.convertAndSend(JmsConfig.FILE_STORE_REQUEST_QUEUE, FileStoreRequestDto.builder()
                                    .fileStoreRequestType(FileStoreRequestTypeDto.INSERT)
                                    .walkerJobDto(walkerJobDto)
                                    .fileStore(fileStoreDto)
                                    .filePath(filePathDto)
                                    .fileItem(fromStore)
                                    .build());
                        } else {
                            log.info("File found in store: {}", fromStore);
                            FileTime created = fileAttr.creationTime();
                            if (!LocalDateTime.ofInstant(created.toInstant(), currentSystemZoneOffset).truncatedTo(ChronoUnit.SECONDS).isEqual(fromStore.getCreatedDate())) {
                                log.info("File.created: {}, File.dto: {}", LocalDateTime.ofInstant(created.toInstant(), currentSystemZoneOffset).toInstant(currentSystemZoneOffset), fromStore.getCreatedDate().toInstant(currentSystemZoneOffset));
                                fromStore.setCreatedDate(LocalDateTime.ofInstant(created.toInstant(), currentSystemZoneOffset).truncatedTo(ChronoUnit.SECONDS));
                                fromStore.setLocallyChanged(true);
                            }
                            FileTime modified = fileAttr.lastModifiedTime();
                            if (!LocalDateTime.ofInstant(modified.toInstant(), currentSystemZoneOffset).truncatedTo(ChronoUnit.SECONDS).isEqual(fromStore.getLastModifiedDate())) {
                                log.info("File.modified: {}, File.dto: {}", LocalDateTime.ofInstant(modified.toInstant(), currentSystemZoneOffset).toString(), fromStore.getLastModifiedDate().toString());
                                fromStore.setLastModifiedDate(LocalDateTime.ofInstant(modified.toInstant(), currentSystemZoneOffset).truncatedTo(ChronoUnit.SECONDS));
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
                                        .walkerJobDto(walkerJobDto)
                                        .fileStore(fileStoreDto)
                                        .filePath(filePathDto)
                                        .fileItem(fromStore)
                                        .build());
                            }
                        }
                        // check
                        fileList.add(file.getFileName().toString());
                        // TODO: check if working for new files as well
                        folderItemsCacheMap.put(file.getParent(),updateContentFile(folderItemsCacheMap.get(file.getParent()),fromStore));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            log.info("Total files: {}", fileList.size());
        }

    }
    private void logFileStoreConfigFile(FileStorePathCache file) {
        if (file!=null) {
            if (file.getFileStoreId()!=null) {
                log.info("File Store: {}", file.getFileStoreId());
            }
            if (file.getFilePathId()!=null) {
                log.info("File Path: {}", file.getFilePathId());
            }
            if (file.getFileItems()!=null) {
                log.info("File Items: {}", file.getFileItems());
            }
        }
    }
    private FileStorePathCache updateContentFile(FileStorePathCache cacheFile, FileStoreDto fileStoreDto) {
        if (cacheFile == null) {
            cacheFile = FileStorePathCache.builder().build();
        }
        if (fileStoreDto != null && fileStoreDto.getId() != null) {
            cacheFile.setFileStoreId(fileStoreDto.getId().toString());
        }
        return cacheFile;
    }
    private FileStorePathCache updateContentFile(FileStorePathCache cacheFile, FilePathDto filePathDto) {
        if (cacheFile == null) {
            cacheFile = FileStorePathCache.builder().build();
        }
        if (filePathDto != null && filePathDto.getId() != null) {
            cacheFile.setFilePathId(filePathDto.getId().toString());
        }
        return cacheFile;
    }
    private FileStorePathCache updateContentFile(FileStorePathCache cacheFile, FileItemDto fileItemDto) {
        if (cacheFile == null) {
            cacheFile = FileStorePathCache.builder().build();
        }
        if (cacheFile.getFileItems() == null) {
            cacheFile.setFileItems(new ArrayList<>());
        }
        if (fileItemDto != null) {
            // TODO: get tags from DB
            List<FixedTagEntityDto> tags = null;
            cacheFile.getFileItems().add(
                    FileStorePathCacheItem.builder()
                            .fileItemId(fileItemDto.getId())
                            .fileName(fileItemDto.getFilename())
                            .fixedTagEntities(tags)
                            .build());
        }
        return cacheFile;
    }
    private FileStorePathCache readFileStoreConfigFile(Path file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return (Files.exists(file) && Files.size(file)>0 ) ? mapper.readValue(file.toFile(),FileStorePathCache.class) : null;
    }
    private void saveFileStoreConfigFile(Path parent, FileStorePathCache file) throws IOException {
        if (file.getFileStoreId()!=null) {
            Path confFile = new File(parent+File.separator+FOLDER_CACHE_FILE_NAME).toPath();
            if (!Files.exists(confFile)) {
                Files.createFile(confFile);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(confFile.toFile(), file);
            }
            Files.setAttribute(confFile, "dos:hidden", true);
        }
    }
}
