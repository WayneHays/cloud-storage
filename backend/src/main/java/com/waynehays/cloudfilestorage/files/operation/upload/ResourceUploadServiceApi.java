package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;

import java.util.List;

public interface ResourceUploadServiceApi {

    List<ResourceDto> upload(Long userId, List<UploadObjectDto> files);
}
