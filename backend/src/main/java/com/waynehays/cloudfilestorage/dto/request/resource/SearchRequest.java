package com.waynehays.cloudfilestorage.dto.request.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SearchRequest(

        @NotBlank(message = "Search query cannot be blank")
        @Size(max = 200, message = "Query cannot be more than 200 symbols")
        @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ0-9._\\- ]+$", message = "Query contains invalid characters")
        String query
) {
}
