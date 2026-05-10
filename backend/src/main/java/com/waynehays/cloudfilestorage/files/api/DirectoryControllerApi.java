package com.waynehays.cloudfilestorage.files.api;

import com.waynehays.cloudfilestorage.core.exception.ErrorResponse;
import com.waynehays.cloudfilestorage.files.api.dto.request.CreateDirectoryRequest;
import com.waynehays.cloudfilestorage.files.api.dto.request.GetDirectoryContentRequest;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.infrastructure.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

interface DirectoryControllerApi {

    @Operation(summary = "Get directory content",
            description = """
                    Returns list of files and directories inside specified directory (non-recursive).
                    Empty path returns root content
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Directory content retrieved",
                    content = @Content(schema = @Schema(implementation = ResourceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"message": "path: Invalid directory path"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Invalid credentials"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Directory not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Resource not found: 'photos/'"}
                                    """)))
    })
    List<ResourceResponse> getContent(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @Valid GetDirectoryContentRequest getContentRequest);

    @Operation(summary = "Create directory",
            description = "Creates a new empty directory at specified path. Parent directory must exist")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Directory created",
                    content = @Content(schema = @Schema(implementation = ResourceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"message": "path: Invalid directory path"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Invalid credentials"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Parent directory not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Resource not found: 'documents/'"}
                                    """))),
            @ApiResponse(responseCode = "409", description = "Directory already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Directory already exists"}
                                    """)))
    })
    ResourceResponse createDirectory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid CreateDirectoryRequest createRequest);
}
