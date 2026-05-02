package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.core.utils.PathUtils;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class CreateDirectoriesStep implements UploadStep {
    private final BatchInsertMapper batchInsertMapper;
    private final ResourceDtoMapper resourceDtoMapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void execute(Context context) {
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
        context.addToResult(createdDirectories);
    }
}
