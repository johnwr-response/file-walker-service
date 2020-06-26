package no.responseweb.imagearchive.filewalkerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.responseweb.imagearchive.filewalkerservice.FileWalkerServiceApplication;
import no.responseweb.imagearchive.filewalkerservice.config.JmsConfig;
import no.responseweb.imagearchive.filewalkerservice.config.ResponseFilestoreProperties;
import no.responseweb.imagearchive.model.FileStoreDto;
import no.responseweb.imagearchive.model.WalkerStatusReportDto;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class MountPointCheckService {

    private final FileInfoService fileInfoService;
    private final JmsTemplate jmsTemplate;
    private final ResponseFilestoreProperties responseFilestoreProperties;

    @Scheduled(fixedDelay = 5000)
    public void checkAvailability() throws IOException {
        String runningOs = System.getProperty("os.name");
        log.debug("Running file-system availability check on walker running on {}", runningOs);
        if (responseFilestoreProperties.getOverrideWalkPath().startsWith("/")) {
            checkMountPoints(responseFilestoreProperties.getOverrideWalkPath());
        } else {
            checkUncPaths();
        }

    }
    private void checkUncPaths() {
        List<FileStoreDto> fileStores = fileInfoService.getFileStores();
        fileStores.forEach(fileStoreDto -> {
            log.debug("Checking {} for availability", fileStoreDto.getLocalBaseUri());
            Path folder = Paths.get(fileStoreDto.getLocalBaseUri());

            long size = 0;
            try {
                 size = Files.walk(folder)
                        .filter(p -> p.toFile().isFile())
                        .mapToLong(p -> p.toFile().length())
                        .sum();
            } catch (IOException e) {
                log.info("Exception {}", e.getMessage());
            }
            log.debug("Path marked {} for walking: {}", (size > 0 ? " eligible " : " in-eligible "), fileStoreDto.getLocalBaseUri());
            jmsTemplate.convertAndSend(JmsConfig.FILE_STORE_WALKER_STATUS_QUEUE, WalkerStatusReportDto.builder()
                    .walkerInstanceToken(FileWalkerServiceApplication.getAppId())
                    .fileStoreId(fileStoreDto.getId())
                    .ready((size > 0))
                    .build());

        });
    }

    private void checkMountPoints(String overrideWalkPath) throws IOException {
        Set<String> files = listFilesUsingFileWalk(overrideWalkPath);
        List<FileStoreDto> fileStores = fileInfoService.getFileStores();
        fileStores.forEach(f -> log.debug("Mount-point {} has content",f));
        fileStores.forEach(fileStoreDto -> {
            boolean eligible = false;
            if (files.contains(fileStoreDto.getMountPoint())) {
                log.debug("Report as eligible to walk {}", fileStoreDto.getLocalBaseUri());
                eligible = true;
            }
            jmsTemplate.convertAndSend(JmsConfig.FILE_STORE_WALKER_STATUS_QUEUE, WalkerStatusReportDto.builder()
                    .walkerInstanceToken(FileWalkerServiceApplication.getAppId())
                    .fileStoreId(fileStoreDto.getId())
                    .ready(eligible)
                    .build());
        });
    }

    private Set<String> listFilesUsingFileWalk(String dir) throws IOException {
        File fPath = new File(dir);
        Set<String> subDirsWithContent = new HashSet<>();
        Files.walkFileTree(fPath.toPath(), new HashSet<>(), 2, new SimpleFileVisitor<>() {
            final Path rootPath = fPath.toPath();
            final Set<String> fileList = new HashSet<>();
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                fileList.clear();
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                fileList.add(file.getFileName().toString());
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!fileList.isEmpty() && rootPath.compareTo(dir) < 0) {
                    subDirsWithContent.add(dir.getFileName().toString());
                }
                return super.postVisitDirectory(dir, exc);
            }
        });
        return subDirsWithContent;
    }

}
