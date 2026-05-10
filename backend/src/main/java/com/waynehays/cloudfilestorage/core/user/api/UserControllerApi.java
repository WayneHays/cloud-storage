package com.waynehays.cloudfilestorage.core.user.api;

import com.waynehays.cloudfilestorage.core.exception.ErrorResponse;
import com.waynehays.cloudfilestorage.core.user.api.dto.response.UserResponse;
import com.waynehays.cloudfilestorage.infrastructure.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

interface UserControllerApi {

    @Operation(summary = "Get current user",
            description = "Returns information about currently authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User information retrieved",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Invalid credentials"}
                                    """)))
    })
    UserResponse me(@AuthenticationPrincipal CustomUserDetails userDetails);
}
