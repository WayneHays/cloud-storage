package com.waynehays.cloudfilestorage.resource.service.searching;

import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;

import java.util.List;

public interface ResourceSearchServiceApi {

    List<ResourceDto> search(Long userId, String query);
}
