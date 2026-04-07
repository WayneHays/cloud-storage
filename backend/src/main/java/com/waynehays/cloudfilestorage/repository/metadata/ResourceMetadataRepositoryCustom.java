package com.waynehays.cloudfilestorage.repository.metadata;

import java.util.List;

public interface ResourceMetadataRepositoryCustom {

    void saveDirectoriesIfNotExist(List<Object[]> params);
}
