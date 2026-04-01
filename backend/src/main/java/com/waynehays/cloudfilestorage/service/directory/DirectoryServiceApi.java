package com.waynehays.cloudfilestorage.service.directory;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

import java.util.List;

public interface DirectoryServiceApi {

    List<ResourceDto> getContent(Long userId, String path);

    ResourceDto createDirectory(Long userId, String path);
}
