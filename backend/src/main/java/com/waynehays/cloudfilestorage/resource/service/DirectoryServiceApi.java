package com.waynehays.cloudfilestorage.resource.service;

import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;

import java.util.List;

public interface DirectoryServiceApi {

    List<ResourceDto> getContent(Long userId, String path);

    ResourceDto createDirectory(Long userId, String path);
}
