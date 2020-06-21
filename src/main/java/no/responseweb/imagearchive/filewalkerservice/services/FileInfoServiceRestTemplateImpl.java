package no.responseweb.imagearchive.filewalkerservice.services;
//
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//import no.responseweb.imagearchive.filewalkerservice.services.model.FileItemDto;
//import no.responseweb.imagearchive.filewalkerservice.services.model.FilePathDto;
//import no.responseweb.imagearchive.filewalkerservice.services.model.FileStoreDto;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.web.client.RestTemplateBuilder;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Setter
//// @ConfigurationProperties(prefix = "response.fileinfoservice", ignoreUnknownFields = true)
//// @Component
//public class FileInfoServiceRestTemplateImpl implements FileInfoService {
//
//    private final RestTemplate restTemplate;
//
//    public FileInfoServiceRestTemplateImpl(RestTemplateBuilder restTemplateBuilder) {
//        this.restTemplate = restTemplateBuilder.build();
//    }
//
//    private String host;
//
//    @Override
//    public List<FileItemDto> getFilesAtPath(UUID filePathId) {
//
//        log.debug("Calling File Info Service");
//
//        ResponseEntity<List<FileItemDto>> responseEntity = restTemplate.exchange(
//                host + FileInfoServiceFeignImpl.FILES_IN_FOLDER_PATH
//                , HttpMethod.GET
//                , null
//                , new ParameterizedTypeReference<List<FileItemDto>>() {}
//                , (Object)filePathId
//        );
//
//        return new ArrayList<>(Objects.requireNonNull(responseEntity.getBody()));
//    }
//
//    @Override
//    public FilePathDto getFilePath(String path) {
//        ResponseEntity<FilePathDto> responseEntity = restTemplate.exchange(
//                host + FileInfoServiceFeignImpl.FOLDER_PATH
//                , HttpMethod.GET
//                , null
//                , new ParameterizedTypeReference<FilePathDto>() {}
//                , (Object) path
//        );
//        return responseEntity.getBody();
//    }
//
//    @Override
//    public FileStoreDto getFileStore(String nickname) {
//        restTemplate.getForObject(host + FileInfoServiceFeignImpl.STORE_PATH, FileStoreDto.class);
//
//
//
//
//        ResponseEntity<FileStoreDto> responseEntity = restTemplate.exchange(
//                host + FileInfoServiceFeignImpl.STORE_PATH
//                , HttpMethod.GET
//                , null
//                , new ParameterizedTypeReference<FileStoreDto>() {}
//                , (Object) nickname
//        );
//        return responseEntity.getBody();
//    }
//}
