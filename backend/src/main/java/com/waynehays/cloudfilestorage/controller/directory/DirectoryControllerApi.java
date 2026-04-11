package com.waynehays.cloudfilestorage.controller.directory;

import com.waynehays.cloudfilestorage.dto.request.directory.CreateDirectoryRequest;
import com.waynehays.cloudfilestorage.dto.request.directory.GetDirectoryContentRequest;
import com.waynehays.cloudfilestorage.dto.response.ErrorDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

public interface DirectoryControllerApi {

    @Operation(summary = "Get directory content",
            description = """
                    Returns list of files and directories inside specified directory (non-recursive).
                    Empty path returns root content
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Directory content retrieved",
                    content = @Content(schema = @Schema(implementation = ResourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Directory not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    List<ResourceDto> getContent(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @Valid GetDirectoryContentRequest getContentRequest);

    @Operation(summary = "Create directory",
            description = "Creates a new empty directory at specified path. Parent directory must exist")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Directory created",
                    content = @Content(schema = @Schema(implementation = ResourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Parent directory not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "409", description = "Directory already exists",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    ResourceDto createDirectory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @Valid CreateDirectoryRequest createRequest);
}
