package com.waynehays.cloudfilestorage.repository.quota;

import java.util.List;

public interface StorageQuotaRepositoryCustom {

    void batchUpdateUsedSpace(List<Object[]> params);

    void batchDecreaseUsedSpace(List<Object[]> params);
}
