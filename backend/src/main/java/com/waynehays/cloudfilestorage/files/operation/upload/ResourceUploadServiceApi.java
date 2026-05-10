package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.UploadObjectDto;

import java.util.List;

public interface ResourceUploadServiceApi {

    List<ResourceResponse> upload(Long userId, List<UploadObjectDto> files);
}
