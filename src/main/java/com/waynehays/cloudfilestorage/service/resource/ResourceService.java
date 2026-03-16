package com.waynehays.cloudfilestorage.service.resource;

import com.waynehays.cloudfilestorage.dto.FileData;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.service.resource.deleter.ResourceDeleterApi;
import com.waynehays.cloudfilestorage.service.resource.downloader.ResourceDownloaderApi;
import com.waynehays.cloudfilestorage.service.resource.infoprovider.ResourceInfoProviderApi;
import com.waynehays.cloudfilestorage.service.resource.mover.ResourceMoverApi;
import com.waynehays.cloudfilestorage.service.resource.searcher.ResourceSearcherApi;
import com.waynehays.cloudfilestorage.service.resource.uploader.ResourceUploaderApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService implements ResourceServiceApi {
    private final ResourceDownloaderApi downloader;
    private final ResourceDeleterApi deleter;
    private final ResourceMoverApi mover;
    private final ResourceUploaderApi uploader;
    private final ResourceSearcherApi searcher;
    private final ResourceInfoProviderApi infoProvider;

    @Override
    public ResourceDto getInfo(Long userId, String path) {
        return infoProvider.getInfo(userId, path);
    }

    @Override
    public void delete(Long userId, String path) {
        deleter.delete(userId, path);
    }

    @Override
    public DownloadResult download(Long userId, String path) {
        return downloader.download(userId, path);
    }

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        return mover.move(userId, pathFrom, pathTo);
    }

    @Override
    public List<ResourceDto> search(Long userId, String query) {
        return searcher.search(userId, query);
    }

    @Override
    public List<ResourceDto> upload(Long userId, List<FileData> files) {
        return uploader.upload(userId, files);
    }
}
