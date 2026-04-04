package com.waynehays.cloudfilestorage.repository;

import java.util.List;

public interface ResourceMetadataRepositoryCustom {

    void saveDirectoriesIfNotExist(List<Object[]> params);
}
