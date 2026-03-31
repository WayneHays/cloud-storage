package com.waynehays.cloudfilestorage.service.resource.deleter;

public interface ResourceDeleterApi {

    void delete(Long userId, String path);
}
