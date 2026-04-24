package com.waynehays.cloudfilestorage.resource.service.uploading;

import com.waynehays.cloudfilestorage.resource.dto.internal.DirectoryRowDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.resource.mapper.BatchInsertMapper;
import com.waynehays.cloudfilestorage.resource.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.resource.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.shared.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
class CreateDirectoriesStep implements UploadStep {
    private final BatchInsertMapper batchInsertMapper;
    private final ResourceDtoMapper resourceDtoMapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void execute(UploadContext context) {
        Set<String> allDirectoryPaths = context.getResult().stream()
                .flatMap(r -> PathUtils.getAllAncestorDirectories(r.path()).stream())
                .collect(Collectors.toSet());

        if (allDirectoryPaths.isEmpty()) {
            return;
        }

        Set<String> missingPaths = metadataService.findMissingPaths(context.getUserId(), allDirectoryPaths);

        if (missingPaths.isEmpty()) {
            return;
        }

        List<DirectoryRowDto> directoriesToSave = batchInsertMapper.toDirectoryRows(missingPaths);
        metadataService.saveDirectories(context.getUserId(), directoriesToSave);
        missingPaths.forEach(context::addSavedToDbPath);

        List<ResourceDto> createdDirectories = resourceDtoMapper.directoriesFromPaths(missingPaths);
        context.addResult(createdDirectories);
    }
}
