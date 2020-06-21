package no.responseweb.imagearchive.filewalkerservice.services;

import no.responseweb.imagearchive.filewalkerservice.services.model.FileItemDto;
import no.responseweb.imagearchive.filewalkerservice.services.model.FilePathDto;
import no.responseweb.imagearchive.filewalkerservice.services.model.FileStoreDto;

import java.util.List;
import java.util.UUID;

public interface FileInfoService {
    FileStoreDto getFileStore(String nickname);
    FilePathDto getRootFilePath(UUID fileStoreId);
    FilePathDto getStoreRelativePath(UUID fileStoreId, String relativePath);
    List<FileItemDto> getFilesAtPath(UUID filePathId);
}
