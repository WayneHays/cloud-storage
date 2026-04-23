package com.waynehays.cloudfilestorage.resource.service.uploading;

import com.waynehays.cloudfilestorage.resource.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;

import java.util.List;

public interface ResourceUploadServiceApi {

    List<ResourceDto> upload(Long userId, List<UploadObjectDto> files);
}
