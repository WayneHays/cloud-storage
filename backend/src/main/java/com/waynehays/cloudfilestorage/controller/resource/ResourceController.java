package com.waynehays.cloudfilestorage.controller.resource;

import com.waynehays.cloudfilestorage.dto.internal.DownloadResult;
import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.request.resource.DeleteRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.DownloadRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.GetInfoRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.MoveRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.SearchRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.UploadRequest;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.parser.MultipartFileDataParser;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import com.waynehays.cloudfilestorage.service.resource.deletion.ResourceDeletionServiceApi;
import com.waynehays.cloudfilestorage.service.resource.download.ResourceDownloadServiceApi;
import com.waynehays.cloudfilestorage.service.resource.info.ResourceInfoServiceApi;
import com.waynehays.cloudfilestorage.service.resource.move.ResourceMoveServiceApi;
import com.waynehays.cloudfilestorage.service.resource.search.ResourceSearchServiceApi;
import com.waynehays.cloudfilestorage.service.resource.upload.ResourceUploadServiceApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController implements ResourceControllerApi {
    private final MultipartFileDataParser multipartFileDataParser;
    private final ResourceDeletionServiceApi deletionService;
    private final ResourceDownloadServiceApi downloadService;
    private final ResourceInfoServiceApi infoService;
    private final ResourceMoveServiceApi moveService;
    private final ResourceSearchServiceApi searchService;
    private final ResourceUploadServiceApi uploadService;

    @Override
    @GetMapping
    public ResourceDto getResourceInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @Valid GetInfoRequest request) {
        return infoService.getInfo(userDetails.id(), request.path());
    }

    @Override
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @Valid DeleteRequest request) {
        deletionService.delete(userDetails.id(), request.path());
    }

    @Override
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                  @Valid DownloadRequest request) {
        DownloadResult result = downloadService.download(userDetails.id(), request.path());

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = result.content()) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.name() + "\"")
                .body(body);
    }

    @Override
    @PutMapping("/move")
    public ResourceDto moveOrRenameResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @Valid MoveRequest request) {
        return moveService.move(userDetails.id(), request.from(), request.to());
    }

    @Override
    @GetMapping("/search")
    public List<ResourceDto> searchResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @Valid SearchRequest request) {
        return searchService.search(userDetails.id(), request.query());
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceDto> uploadResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @Valid UploadRequest request,
                                            @RequestParam("files") List<MultipartFile> files) {
        List<UploadObjectDto> uploadObjects = files.stream()
                .map(file -> multipartFileDataParser.parse(file, request.path()))
                .toList();
        return uploadService.upload(userDetails.id(), uploadObjects);
    }
}
