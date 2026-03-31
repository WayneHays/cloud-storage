package com.waynehays.cloudfilestorage.unit.component.validator;

import com.waynehays.cloudfilestorage.component.validator.PathNotEqualsValidator;
import com.waynehays.cloudfilestorage.dto.request.resource.MoveRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathNotEqualsValidatorTest {

    private final PathNotEqualsValidator validator = new PathNotEqualsValidator();

    @Test
    void shouldAcceptDifferentPaths() {
        // given
        MoveRequest request = new MoveRequest("directory/file.txt", "directory/renamed.txt");

        // when & then
        assertThat(validator.isValid(request, null)).isTrue();
    }

    @Test
    void shouldRejectIdenticalPaths() {
        // given
        MoveRequest request = new MoveRequest("directory/file.txt", "directory/file.txt");

        // when & then
        assertThat(validator.isValid(request, null)).isFalse();
    }

    @Test
    void shouldRejectPathsDifferingOnlyByCase() {
        // given
        MoveRequest request = new MoveRequest("Directory/File.txt", "directory/file.txt");

        // when & then
        assertThat(validator.isValid(request, null)).isFalse();
    }
}
