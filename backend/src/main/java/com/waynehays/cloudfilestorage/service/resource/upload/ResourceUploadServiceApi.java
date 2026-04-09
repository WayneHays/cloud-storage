package com.waynehays.cloudfilestorage.service.resource.upload;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

import java.util.List;

public interface ResourceUploadServiceApi {

    List<ResourceDto> upload(Long userId, List<UploadObjectDto> files);
}
