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
public class FilePathDto {
    private UUID id;
    private UUID fileStoreId;
    private String relativePath;

}
