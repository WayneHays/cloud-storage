package com.waynehays.cloudfilestorage.service.quota;

import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.entity.StorageQuota;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.exception.StorageQuotaNotFoundException;
import com.waynehays.cloudfilestorage.repository.quota.StorageQuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageQuotaService implements StorageQuotaServiceApi {
    private final StorageQuotaRepository repository;

    @Override
    public Page<Long> findAllUserIds(int page, int limit) {
        return repository.findAllUserIds(PageRequest.of(page, limit));
    }

    @Override
    @Transactional
    public void createStorageQuota(Long userId, long storageLimit) {
        StorageQuota quota = new StorageQuota();
        quota.setUserId(userId);
        quota.setUsedSpace(0);
        quota.setStorageLimit(storageLimit);
        repository.saveAndFlush(quota);

        log.info("Storage quota created for user={}, storage limit={}", userId, storageLimit);
    }

    @Override
    @Transactional
    public void reserveSpace(Long userId, long bytes) {
        log.info("Start reserve space: userId={}, bytes={}", userId, bytes);

        StorageQuota quota = repository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new StorageQuotaNotFoundException("Quota not found for user", userId));
        long freeSpace = quota.getStorageLimit() - quota.getUsedSpace();

        if (bytes > freeSpace) {
            throw new ResourceStorageLimitException("Not enough storage space", bytes, freeSpace);
        }

        long updatedUsedSpace = quota.getUsedSpace() + bytes;
        quota.setUsedSpace(updatedUsedSpace);
        long updatedFreeSpace = quota.getStorageLimit() - updatedUsedSpace;

        log.info("Reserved space for user={}, bytes={}, freespace={}", userId, bytes, updatedFreeSpace);
    }

    @Override
    @Transactional
    public void releaseSpace(Long userId, long bytes) {
        log.info("Start release space: userId={}, bytes={}", userId, bytes);

        StorageQuota quota = repository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new StorageQuotaNotFoundException("Quota not found for user", userId));
        long updatedUsedSpace = quota.getUsedSpace() - bytes;
        long freeSpace = quota.getStorageLimit() - updatedUsedSpace;
        quota.setUsedSpace(updatedUsedSpace);

        log.info("Released space for user={}, bytes={}, free space={}", userId, bytes, freeSpace);
    }

    @Override
    @Transactional
    public void reconcileUsedSpace(List<Long> userIds) {
        repository.reconcileUsedSpace(userIds);
    }

    @Override
    @Transactional
    public void batchReleaseUsedSpace(List<SpaceReleaseDto> spaceToRelease) {
        repository.batchReleaseUsedSpace(spaceToRelease);
    }
}
