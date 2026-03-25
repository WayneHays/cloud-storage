package com.waynehays.cloudfilestorage.service.resource;

import com.waynehays.cloudfilestorage.dto.ObjectData;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

import java.util.List;

public interface ResourceServiceApi {

    ResourceDto getInfo(Long userId, String path);

    void delete(Long userId, String path);

    DownloadResult download(Long userId, String path);

    ResourceDto move(Long userId, String pathFrom, String pathTo);

    List<ResourceDto> search(Long userId, String query);

    List<ResourceDto> upload(Long userId, List<ObjectData> files);
}
