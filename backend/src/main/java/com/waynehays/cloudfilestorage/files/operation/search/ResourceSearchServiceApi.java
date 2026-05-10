package com.waynehays.cloudfilestorage.files.operation.search;

import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;

import java.util.List;

public interface ResourceSearchServiceApi {

    List<ResourceResponse> search(Long userId, String query);
}
