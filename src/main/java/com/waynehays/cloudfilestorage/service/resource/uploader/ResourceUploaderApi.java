package com.waynehays.cloudfilestorage.service.resource.uploader;

import com.waynehays.cloudfilestorage.dto.FileData;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

import java.util.List;

public interface ResourceUploaderApi {

    List<ResourceDto> upload(Long userId, List<FileData> files);
}
