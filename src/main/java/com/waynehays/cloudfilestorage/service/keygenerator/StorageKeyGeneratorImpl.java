package com.waynehays.cloudfilestorage.service.keygenerator;

import com.waynehays.cloudfilestorage.constant.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class StorageKeyGeneratorImpl implements StorageKeyGenerator {
    private static final String USER_DIRECTORY_FORMAT = "user-%d-files";

    @Override
    public String generate(Long userId, String directory, String filename) {
        StringBuilder key = new StringBuilder();
        key.append(USER_DIRECTORY_FORMAT.formatted(userId));

        if (StringUtils.isNotEmpty(directory)) {
            key.append(Constants.PATH_SEPARATOR).append(directory);
        }

        key.append(Constants.PATH_SEPARATOR).append(filename);

        return key.toString();
    }
}
