package com.waynehays.cloudfilestorage.controller.user;

import com.waynehays.cloudfilestorage.dto.response.ErrorDto;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
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
