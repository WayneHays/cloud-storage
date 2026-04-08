package com.waynehays.cloudfilestorage.repository.metadata;

import java.util.List;

public interface ResourceMetadataRepositoryCustom {

    void saveDirectories(List<Object[]> params);

    void saveFiles(List<Object[]> params);

    long markForDeletionAndSumSize(Long userId, String prefix);
}
