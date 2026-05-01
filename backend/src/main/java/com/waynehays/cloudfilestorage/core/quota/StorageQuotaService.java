package com.waynehays.cloudfilestorage.core.quota;

import com.waynehays.cloudfilestorage.core.quota.dto.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaLimitException;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaNotFoundException;
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
class StorageQuotaService implements StorageQuotaServiceApi {
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
        quota.setStorageLimit(storageLimit);
        repository.saveAndFlush(quota);

        log.info("Storage quota created: userId={}, limit={}", userId, storageLimit);
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

        log.info("Reserved space: bytes={}, freespace={}", bytes, updatedFreeSpace);
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
