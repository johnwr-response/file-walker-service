package no.responseweb.imagearchive.filewalkerservice.services.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStoreDto {
    private UUID id;
    private String baseUri;
    private String nickname;

}
