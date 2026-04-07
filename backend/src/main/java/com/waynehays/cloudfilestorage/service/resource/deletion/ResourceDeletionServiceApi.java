package com.waynehays.cloudfilestorage.service.resource.deletion;

public interface ResourceDeletionServiceApi {

    void delete(Long userId, String path);
}
