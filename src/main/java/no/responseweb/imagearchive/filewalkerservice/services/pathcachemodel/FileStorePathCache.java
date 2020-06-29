package no.responseweb.imagearchive.filewalkerservice.services.pathcachemodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStorePathCache {
    private String fileStoreId;
    private String filePathId;
    private List<FileStorePathCacheItem> fileItems;
}
