package com.waynehays.cloudfilestorage.service.quota;

import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.StorageQuotaDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.UsedSpaceCorrectionDto;
import com.waynehays.cloudfilestorage.entity.StorageQuota;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.exception.StorageQuotaNotFoundException;
import com.waynehays.cloudfilestorage.mapper.StorageQuotaMapper;
import com.waynehays.cloudfilestorage.repository.quota.StorageQuotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageQuotaService implements StorageQuotaServiceApi {
    private final StorageQuotaMapper mapper;
    private final StorageQuotaRepository repository;

    @Override
    @Transactional
    public void createStorageQuota(Long userId, long storageLimit) {
        StorageQuota quota = new StorageQuota();
        quota.setUserId(userId);
        quota.setUsedSpace(0);
        quota.setStorageLimit(storageLimit);
        repository.saveAndFlush(quota);
    }

    @Override
    @Transactional
    public void reserveSpace(Long userId, long bytes) {
        StorageQuota quota = repository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new StorageQuotaNotFoundException("Quota not found for user", userId));
        long freeSpace = quota.getStorageLimit() - quota.getUsedSpace();

        if (bytes > freeSpace) {
            throw new ResourceStorageLimitException("Not enough storage space", bytes, freeSpace);
        }

        quota.setUsedSpace(quota.getUsedSpace() + bytes);
    }

    @Override
    @Transactional
    public void releaseSpace(Long userId, long bytes) {
        repository.decreaseUsedSpace(userId, bytes);
    }

    @Override
    public Page<StorageQuotaDto> findAllQuotas(int page, int limit) {
        return repository.findAll(PageRequest.of(page, limit))
                .map(mapper::toDto);
    }

    @Override
    @Transactional
    public void batchUpdateUsedSpace(List<UsedSpaceCorrectionDto> corrections) {
        List<Object[]> params = corrections.stream()
                .map(c -> new Object[]{c.actualUsedSpace(), c.userId()})
                .toList();
        repository.batchUpdateUsedSpace(params);
    }

    @Override
    @Transactional
    public void batchDecreaseUsedSpace(List<SpaceReleaseDto> releases) {
        List<Object[]> params = releases.stream()
                .map(r -> new Object[]{r.bytes(), r.userId()})
                .toList();
        repository.batchDecreaseUsedSpace(params);
    }
}
