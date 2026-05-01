package com.waynehays.cloudfilestorage.files.api.resource;

import com.waynehays.cloudfilestorage.core.user.CustomUserDetails;
import com.waynehays.cloudfilestorage.files.dto.request.resource.DeleteRequest;
import com.waynehays.cloudfilestorage.files.dto.request.resource.DownloadRequest;
import com.waynehays.cloudfilestorage.files.dto.request.resource.GetInfoRequest;
import com.waynehays.cloudfilestorage.files.dto.request.resource.MoveRequest;
import com.waynehays.cloudfilestorage.files.dto.request.resource.SearchRequest;
import com.waynehays.cloudfilestorage.files.dto.request.resource.UploadRequest;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

interface ResourceControllerApi {

    @Operation(summary = "Get resource info",
            description = "Returns metadata (path, name, size, type) of file or directory by full path")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource found",
                    content = @Content(schema = @Schema(implementation = ResourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "path: Invalid path"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Invalid credentials"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Resource not found: 'docs/file.txt'"}
                                    """)))
    })
    ResourceDto getResourceInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @Valid GetInfoRequest getInfoRequest);

    @Operation(summary = "Delete resource",
            description = "Deletes file or directory by path. Directory is deleted with all its contents recursively")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Resource deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "path: Invalid path"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Invalid credentials"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Resource not found: 'docs/file.txt'"}
                                    """)))
    })
    void deleteResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                        @Valid DeleteRequest deleteRequest);

    @Operation(summary = "Download resource",
            description = "Downloads file as octet-stream or directory as ZIP archive containing all nested files")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource downloading"),
            @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "path: Invalid path"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Resource not found: 'docs/file.txt'"}
                                    """)))
    })
    ResponseEntity<StreamingResponseBody> downloadResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                           @Valid DownloadRequest downloadRequest);

    @Operation(summary = "Move or rename resource",
            description = "Moves resource to new path or renames it. Works for both files and directories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource moved or renamed",
                    content = @Content(schema = @Schema(implementation = ResourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path or move operation",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Cannot move directory to file: 'docs/' -> 'file.txt'"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Resource not found: 'docs/file.txt'"}
                                    """))),
            @ApiResponse(responseCode = "409", description = "Resource by target path already exists",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Resources already exist: [docs/file.txt]"}
                                    """)))
    })
    ResourceDto moveOrRenameResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid MoveRequest moveRequest);

    @Operation(summary = "Search resources",
            description = "Searches files and directories by case-insensitive name substring match")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed",
                    content = @Content(schema = @Schema(implementation = ResourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "query: must not be blank"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Invalid credentials"}
                                    """)))
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
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "File size is too large, max file size 500 MB"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Invalid credentials"}
                                    """))),
            @ApiResponse(responseCode = "409", description = "Resource already exists",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class),
                            examples = @ExampleObject(value = """
                                    {"message": "Resources already exist: [docs/file.txt]"}
                                    """)))
    })
    List<ResourceDto> uploadResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid UploadRequest uploadRequest,
                                     @RequestParam("files") List<MultipartFile> files);
}
