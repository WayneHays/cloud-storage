package com.waynehays.cloudfilestorage.core.user.api;

import com.waynehays.cloudfilestorage.core.user.api.dto.SignInRequest;
import com.waynehays.cloudfilestorage.core.user.api.dto.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.dto.response.UserDto;
import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

interface AuthControllerApi {

    @Operation(summary = "Sign up new user",
            description = "Registers a new user account and automatically signs in")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered and signed in",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "username: must not be blank"}
                                    """))),
            @ApiResponse(responseCode = "409", description = "Username already taken",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Username already taken"}
                                    """)))
    })
    UserDto signUp(@RequestBody @Valid SignUpRequest signUpRequest,
                   HttpServletRequest request,
                   HttpServletResponse response);

    @Operation(summary = "Sign in",
            description = "Authenticates user and creates session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User signed in",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "username: must not be blank"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Invalid credentials"}
                                    """)))
    })
    UserDto signIn(@RequestBody @Valid SignInRequest signInRequest,
                   HttpServletRequest request,
                   HttpServletResponse response);
}
