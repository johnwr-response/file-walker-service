package no.responseweb.imagearchive.filewalkerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.responseweb.imagearchive.model.FileItemDto;
import no.responseweb.imagearchive.model.FilePathDto;
import no.responseweb.imagearchive.model.FileStoreDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
// @Profile({"feign-test-do-not-use"})
public class FileInfoServiceFeignImpl implements FileInfoService {
    public static final String FILES_IN_FOLDER_PATH = "/api/v1/filesinfolder/{filePathId}";
    public static final String STORE_ROOT_PATH = "/api/v1/filePath/{fileStoreId}/file-store-root-folder";
    public static final String STORE_RELATIVE_PATH = "api/v1/filePath/{fileStoreId}/{relativePath}";
    public static final String STORE_PATH = "/api/v1/fileStoreNickname/{fileStoreNickname}";
    private final FileInfoServiceFeignClient fileInfoServiceFeignClient;

    @Override
    public FileStoreDto getFileStore(String nickname) {
        ResponseEntity<FileStoreDto> responseEntity = fileInfoServiceFeignClient.getFileStore(nickname);
        return responseEntity.getBody();
    }

    @Override
    public FilePathDto getRootFilePath(UUID fileStoreId) {
        ResponseEntity<FilePathDto> responseEntity = fileInfoServiceFeignClient.getFileStoreRootFilePath(fileStoreId);
        return responseEntity.getBody();
    }

    @Override
    public FilePathDto getStoreRelativePath(UUID fileStoreId, String relativePath) {
        ResponseEntity<FilePathDto> responseEntity = fileInfoServiceFeignClient.getFileStoreRelativeFilePath(fileStoreId,relativePath);
        return responseEntity.getBody();
    }

    @Override
    public List<FileItemDto> getFilesAtPath(UUID filePathId) {
        ResponseEntity<List<FileItemDto>> responseEntity = fileInfoServiceFeignClient.getFilesInFolder(filePathId);
        return responseEntity.getBody();
    }



}
