package com.waynehays.cloudfilestorage.service.resource.searcher;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

import java.util.List;

public interface ResourceSearcherApi {

    List<ResourceDto> search(Long userId, String query);
}
