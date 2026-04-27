package com.waynehays.cloudfilestorage.files.operation.search;

import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;

import java.util.List;

public interface ResourceSearchServiceApi {

    List<ResourceDto> search(Long userId, String query);
}
