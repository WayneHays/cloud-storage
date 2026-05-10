package com.waynehays.cloudfilestorage.core.quota.service;

import com.waynehays.cloudfilestorage.core.quota.dto.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.core.quota.entity.StorageQuota;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaLimitException;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaNotFoundException;
import com.waynehays.cloudfilestorage.core.quota.factory.StorageQuotaFactory;
import com.waynehays.cloudfilestorage.core.quota.repository.StorageQuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StorageQuotaService implements StorageQuotaServiceApi, StorageQuotaBatchApi {
    private final StorageQuotaFactory factory;
    private final StorageQuotaRepository repository;

    @Override
    @Transactional
    public void createStorageQuota(Long userId, long storageLimit) {
        StorageQuota quota = factory.create(userId, storageLimit);
        repository.saveAndFlush(quota);

        log.info("Storage quota created: userId={}, limit={} bytes", userId, storageLimit);
    }

    @Override
    @Transactional
    public void reserveSpace(Long userId, long bytes) {
        log.info("Start reserve space: bytes={}", bytes);

        StorageQuota quota = repository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new QuotaNotFoundException(userId));
        long freeSpace = quota.getStorageLimit() - quota.getUsedSpace();

        if (bytes > freeSpace) {
            throw new QuotaLimitException(bytes, freeSpace);
        }

        long updatedUsedSpace = quota.getUsedSpace() + bytes;
        quota.setUsedSpace(updatedUsedSpace);
        long updatedFreeSpace = quota.getStorageLimit() - updatedUsedSpace;

        log.info("Reserved space: bytes={}, free space={}", bytes, updatedFreeSpace);
    }

    @Override
    @Transactional
    public void releaseSpace(Long userId, long bytes) {
        log.info("Start release space: bytes={}", bytes);

        StorageQuota quota = repository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new QuotaNotFoundException(userId));
        long updatedUsedSpace = Math.max(0, quota.getUsedSpace() - bytes);
        long freeSpace = quota.getStorageLimit() - updatedUsedSpace;
        quota.setUsedSpace(updatedUsedSpace);

        log.info("Released space: bytes={}, free space={}", bytes, freeSpace);
    }

    @Override
    public Page<Long> findAllUserIds(Pageable pageable) {
        return repository.findAllUserIds(pageable);
    }

    @Override
    @Transactional
    public void reconcileUsedSpace(List<Long> userIds) {
        repository.reconcileUsedSpace(userIds);
    }

    @Override
    @Transactional
    public void batchReleaseUsedSpace(List<SpaceReleaseDto> spaceToRelease) {
        Long[] userIds = spaceToRelease.stream()
                .map(SpaceReleaseDto::userId)
                .toArray(Long[]::new);
        Long[] bytesToRelease = spaceToRelease.stream()
                .map(SpaceReleaseDto::bytesToRelease)
                .toArray(Long[]::new);
        repository.batchReleaseUsedSpace(userIds, bytesToRelease);
    }
}
