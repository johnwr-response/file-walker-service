package no.responseweb.imagearchive.filewalkerservice.services;

import no.responseweb.imagearchive.filewalkerservice.config.FeignClientConfig;
import no.responseweb.imagearchive.filewalkerservice.services.model.FileItemDto;
import no.responseweb.imagearchive.filewalkerservice.services.model.FilePathDto;
import no.responseweb.imagearchive.filewalkerservice.services.model.FileStoreDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.UUID;

//@FeignClient(name = "file-info-service", fallback = FileInfoServiceFeignClientFailover.class, configuration = FeignClientConfig.class)
@FeignClient(value = "file-info-service", url = "http://localhost:8071", configuration = FeignClientConfig.class)
public interface FileInfoServiceFeignClient {
    @RequestMapping(method = RequestMethod.GET, value = FileInfoServiceFeignImpl.FILES_IN_FOLDER_PATH)
    ResponseEntity<List<FileItemDto>> getFilesInFolder(@PathVariable UUID filePathId);

    @RequestMapping(method = RequestMethod.GET, value = FileInfoServiceFeignImpl.STORE_ROOT_PATH)
    ResponseEntity<FilePathDto> getFileStoreRootFilePath(@PathVariable UUID fileStoreId);

    @RequestMapping(method = RequestMethod.GET, value = FileInfoServiceFeignImpl.STORE_RELATIVE_PATH)
    ResponseEntity<FilePathDto> getFileStoreRelativeFilePath(@PathVariable UUID fileStoreId, @PathVariable String relativePath);

    @RequestMapping(method = RequestMethod.GET, value = FileInfoServiceFeignImpl.STORE_PATH)
    ResponseEntity<FileStoreDto> getFileStore(@PathVariable String fileStoreNickname);
}
