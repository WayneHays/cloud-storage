package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.core.user.dto.response.UserDto;
import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

public interface UserControllerApi {

    @Operation(summary = "Get current user",
            description = "Returns information about currently authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User information retrieved",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    UserDto me(@AuthenticationPrincipal CustomUserDetails userDetails);
}
