package com.waynehays.cloudfilestorage.unit.service.keygenerator;

import com.waynehays.cloudfilestorage.service.keygenerator.StorageKeyGeneratorImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageKeyGeneratorImplTest {

    private final StorageKeyGeneratorImpl storageKeyGenerator = new StorageKeyGeneratorImpl();

    @Test
    @DisplayName("Should generate correct key with directory and extension")
    void shouldGenerateCorrectKeyWithDirectoryAndFilenameAndExtension() {
        // given
        String directory = "docs";
        Long userId = 1L;
        String filename = "file.txt";

        // when
        String result = storageKeyGenerator.generate(userId, directory, filename);

        // then
        assertThat(result).isEqualTo("user-1-files/docs/file.txt");
    }

    @Test
    @DisplayName("Should generate correct key without directory")
    void shouldGenerateCorrectKeyWithoutDirectory() {
        // given
        Long userId = 1L;
        String filename = "file.txt";

        // when
        String result = storageKeyGenerator.generate(userId, null, filename);

        // then
        assertThat(result).isEqualTo("user-1-files/file.txt");
    }

    @Test
    @DisplayName("Should generate correct key with inner directory")
    void shouldGenerateCorrectKey_ifInnerDirectory() {
        // given
        String directory = "docs/text";
        Long userId = 1L;
        String filename = "file.txt";

        // when
        String result = storageKeyGenerator.generate(userId, directory, filename);

        // then
        assertThat(result).isEqualTo("user-1-files/docs/text/file.txt");
    }
}
