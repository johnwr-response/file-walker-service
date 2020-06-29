package no.responseweb.imagearchive.filewalkerservice.services.pathcachemodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.responseweb.imagearchive.model.FixedTagEntityDto;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStorePathCacheItem {
    private UUID fileItemId;
    private String fileName;
    private List<FixedTagEntityDto> fixedTagEntities;
}
