package com.waynehays.cloudfilestorage.files.operation.directory;

import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;

import java.util.List;

public interface DirectoryServiceApi {

    List<ResourceDto> getContent(Long userId, String path);

    ResourceDto createDirectory(Long userId, String path);
}
