package com.waynehays.cloudfilestorage.service.storagequota;

import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.exception.UserNotFoundException;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StorageQuotaService implements StorageQuotaServiceApi {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void reserveSpace(Long userId, long bytes) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found", userId));
        long freeSpace = user.getStorageLimit() - user.getUsedSpace();

        if (bytes > freeSpace) {
            throw new ResourceStorageLimitException("Not enough storage space", bytes, freeSpace);
        }

        user.setUsedSpace(user.getUsedSpace() + bytes);
    }

    @Override
    @Transactional
    public void releaseSpace(Long userId, long bytes) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found", userId));
        user.setUsedSpace(Math.max(0, user.getUsedSpace() - bytes));
    }
}
