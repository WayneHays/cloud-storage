package com.waynehays.cloudfilestorage.unit.parser;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.parser.MultipartFileParser;
import com.waynehays.cloudfilestorage.parser.UploadRequestParser;
import com.waynehays.cloudfilestorage.validator.UploadObjectValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadRequestParserTest {

    @Mock
    private MultipartFileParser parser;

    @Mock
    private UploadObjectValidator validator;

    @InjectMocks
    private UploadRequestParser uploadRequestParser;

    private static final String DIRECTORY = "user/docs/";
    private static final long VALID_FILE_SIZE = 1024L;

    private UploadObjectDto dto(String filename) {
        return new UploadObjectDto(
                filename, filename, DIRECTORY, DIRECTORY + filename,
                VALID_FILE_SIZE, "application/octet-stream", InputStream::nullInputStream
        );
    }

    private MockMultipartFile multipartFile(String filename) {
        return new MockMultipartFile("file", filename, "text/plain", new byte[0]);
    }

    @Nested
    @DisplayName("Successful parsing")
    class SuccessfulParsing {

        @Test
        @DisplayName("Should parse and return single valid file")
        void shouldParseAndReturnSingleValidFile() {
            // given
            MockMultipartFile file = multipartFile("file.txt");
            UploadObjectDto dto = dto("file.txt");

            when(parser.parse(file, DIRECTORY)).thenReturn(dto);
            when(validator.validate(dto)).thenReturn(Optional.empty());

            // when
            List<UploadObjectDto> result = uploadRequestParser.parseAndValidate(List.of(file), DIRECTORY);

            // then
            assertThat(result).containsExactly(dto);
        }

        @Test
        @DisplayName("Should parse and return multiple valid files")
        void shouldParseAndReturnMultipleValidFiles() {
            // given
            MockMultipartFile file1 = multipartFile("file1.txt");
            MockMultipartFile file2 = multipartFile("file2.txt");
            UploadObjectDto dto1 = dto("file1.txt");
            UploadObjectDto dto2 = dto("file2.txt");

            when(parser.parse(file1, DIRECTORY)).thenReturn(dto1);
            when(parser.parse(file2, DIRECTORY)).thenReturn(dto2);
            when(validator.validate(dto1)).thenReturn(Optional.empty());
            when(validator.validate(dto2)).thenReturn(Optional.empty());

            // when
            List<UploadObjectDto> result = uploadRequestParser.parseAndValidate(List.of(file1, file2), DIRECTORY);

            // then
            assertThat(result).containsExactly(dto1, dto2);
        }
    }

    @Nested
    @DisplayName("Validation failures")
    class ValidationFailures {

        @Test
        @DisplayName("Should throw when single file fails validation")
        void shouldThrowWhenSingleFileFailsValidation() {
            // given
            MockMultipartFile file = multipartFile("file@invalid.txt");
            UploadObjectDto dto = dto("file@invalid.txt");

            when(parser.parse(file, DIRECTORY)).thenReturn(dto);
            when(validator.validate(dto)).thenReturn(Optional.of("'file@invalid.txt': contains invalid characters"));

            // when & then
            assertThatThrownBy(() -> uploadRequestParser.parseAndValidate(List.of(file), DIRECTORY))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("invalid characters");
        }

        @Test
        @DisplayName("Should collect all errors when multiple files fail validation")
        void shouldCollectAllErrorsWhenMultipleFilesFailValidation() {
            // given
            MockMultipartFile file1 = multipartFile("file@1.txt");
            MockMultipartFile file2 = multipartFile("file@2.txt");
            UploadObjectDto dto1 = dto("file@1.txt");
            UploadObjectDto dto2 = dto("file@2.txt");

            when(parser.parse(file1, DIRECTORY)).thenReturn(dto1);
            when(parser.parse(file2, DIRECTORY)).thenReturn(dto2);
            when(validator.validate(dto1)).thenReturn(Optional.of("'file@1.txt': contains invalid characters"));
            when(validator.validate(dto2)).thenReturn(Optional.of("'file@2.txt': contains invalid characters"));

            // when & then
            assertThatThrownBy(() -> uploadRequestParser.parseAndValidate(List.of(file1, file2), DIRECTORY))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("file@1.txt")
                    .hasMessageContaining("file@2.txt");
        }

        @Test
        @DisplayName("Should throw when at least one file fails validation")
        void shouldThrowWhenAtLeastOneFileFails() {
            // given
            MockMultipartFile file1 = multipartFile("valid.txt");
            MockMultipartFile file2 = multipartFile("file@invalid.txt");
            UploadObjectDto dto1 = dto("valid.txt");
            UploadObjectDto dto2 = dto("file@invalid.txt");

            when(parser.parse(file1, DIRECTORY)).thenReturn(dto1);
            when(parser.parse(file2, DIRECTORY)).thenReturn(dto2);
            when(validator.validate(dto1)).thenReturn(Optional.empty());
            when(validator.validate(dto2)).thenReturn(Optional.of("'file@invalid.txt': contains invalid characters"));

            // when & then
            assertThatThrownBy(() -> uploadRequestParser.parseAndValidate(List.of(file1, file2), DIRECTORY))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("file@invalid.txt");
        }
    }

    @Nested
    @DisplayName("Parser delegation")
    class ParserDelegation {

        @Test
        @DisplayName("Should delegate each file to parser with correct directory")
        void shouldDelegateEachFileToParserWithCorrectDirectory() {
            // given
            MockMultipartFile file1 = multipartFile("file1.txt");
            MockMultipartFile file2 = multipartFile("file2.txt");
            UploadObjectDto dto1 = dto("file1.txt");
            UploadObjectDto dto2 = dto("file2.txt");

            when(parser.parse(any(), eq(DIRECTORY))).thenReturn(dto1, dto2);
            when(validator.validate(any())).thenReturn(Optional.empty());

            // when
            uploadRequestParser.parseAndValidate(List.of(file1, file2), DIRECTORY);

            // then
            verify(parser).parse(file1, DIRECTORY);
            verify(parser).parse(file2, DIRECTORY);
        }

        @Test
        @DisplayName("Should propagate MultipartValidationException from parser")
        void shouldPropagateExceptionFromParser() {
            // given
            MockMultipartFile file = multipartFile("");

            when(parser.parse(file, DIRECTORY))
                    .thenThrow(new MultipartValidationException("Uploaded file has no filename"));

            // when & then
            assertThatThrownBy(() -> uploadRequestParser.parseAndValidate(List.of(file), DIRECTORY))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("no filename");
        }
    }
}
