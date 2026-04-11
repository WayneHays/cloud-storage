package com.waynehays.cloudfilestorage.dto.request.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SearchRequest(

        @Schema(description = """
                Search query for file or directory names. Matches substrings in names.
                Allowed characters: letters (Latin and Cyrillic), digits, dots, dashes, underscores and spaces
                """)
        @NotBlank(message = "Search query cannot be blank")
        @Size(max = 200, message = "Query cannot be more than 200 symbols")
        @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ0-9._\\- ]+$", message = "Query contains invalid characters")
        String query
) {
}
