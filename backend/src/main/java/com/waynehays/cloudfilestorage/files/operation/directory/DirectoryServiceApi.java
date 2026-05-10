package com.waynehays.cloudfilestorage.files.operation.directory;

import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;

import java.util.List;

public interface DirectoryServiceApi {

    List<ResourceResponse> getContent(Long userId, String path);

    ResourceResponse createDirectory(Long userId, String path);
}
