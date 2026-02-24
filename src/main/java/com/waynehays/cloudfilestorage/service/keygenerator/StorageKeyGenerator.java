package com.waynehays.cloudfilestorage.service.keygenerator;

public interface StorageKeyGenerator {
    String generate(Long userId, String directory, String extension);
}
