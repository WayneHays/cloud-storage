package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CreateDirectoriesStep implements UploadStep {
    private final ResourceResponseMapper resourceResponseMapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void execute(Context context) {
        Set<String> allDirectoryPaths = context.getObjects().stream()
                .flatMap(o -> PathUtils.getAllAncestorDirectories(o.fullPath()).stream())
                .collect(Collectors.toSet());

        if (allDirectoryPaths.isEmpty()) {
            return;
        }

        List<ResourceMetadataDto> savedDirectories = metadataService.saveDirectories(context.getUserId(), allDirectoryPaths);
        allDirectoryPaths.forEach(context::addSavedToDbPath);

        List<ResourceResponse> createdDirectories = resourceResponseMapper.fromResourceMetadataDto(savedDirectories);
        context.addToResult(createdDirectories);
    }
}
