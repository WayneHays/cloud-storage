package com.waynehays.cloudfilestorage.service.resource.search;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

import java.util.List;

public interface ResourceSearchServiceApi {

    List<ResourceDto> search(Long userId, String query);
}
