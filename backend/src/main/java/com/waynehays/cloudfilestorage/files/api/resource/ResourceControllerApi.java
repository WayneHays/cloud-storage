package com.waynehays.cloudfilestorage.files.api.resource;

import com.waynehays.cloudfilestorage.files.dto.request.DeleteRequest;
import com.waynehays.cloudfilestorage.files.dto.request.DownloadRequest;
import com.waynehays.cloudfilestorage.files.dto.request.GetInfoRequest;
import com.waynehays.cloudfilestorage.files.dto.request.MoveRequest;
import com.waynehays.cloudfilestorage.files.dto.request.SearchRequest;
import com.waynehays.cloudfilestorage.files.dto.request.UploadRequest;
import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ErrorDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.core.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

public interface ResourceControllerApi {

    @Operation(summary = "Get resource info",
            description = "Returns metadata (path, name, size, type) of file or directory by full path")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource found",
                    content = @Content(schema = @Schema(implementation = ResourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    ResourceDto getResourceInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @Valid GetInfoRequest getInfoRequest);

    @Operation(summary = "Delete resource",
            description = "Deletes file or directory by path. Directory is deleted with all its contents recursively")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Resource deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
    })
    void deleteResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                        @Valid DeleteRequest deleteRequest);

    @Operation(summary = "Download resource",
            description = "Downloads file as octet-stream or directory as ZIP archive containing all nested files")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource downloading"),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    ResponseEntity<StreamingResponseBody> downloadResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                           @Valid DownloadRequest downloadRequest);

    @Operation(summary = "Move or rename resource",
            description = "Moves resource to new path or renames it. Works for both files and directories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource moved or renamed",
                    content = @Content(schema = @Schema(implementation = ResourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "409", description = "Resource by target path already exists",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    ResourceDto moveOrRenameResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid MoveRequest moveRequest);

    @Operation(summary = "Search resources",
            description = "Searches files and directories by case-insensitive name substring match")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed",
                    content = @Content(schema = @Schema(implementation = ResourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    List<ResourceDto> searchResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid SearchRequest searchRequest);

    @Operation(summary = "Upload resources",
            description = """
                    Uploads one or more files to specified directory.
                    Supports nested paths in filenames to create directory structure
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Resources uploaded",
                    content = @Content(schema = @Schema(implementation = ResourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or file",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "409", description = "Resource already exists",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    List<ResourceDto> uploadResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid UploadRequest uploadRequest,
                                     @RequestParam("files") List<MultipartFile> files);
}
