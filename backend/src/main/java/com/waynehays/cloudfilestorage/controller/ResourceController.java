package com.waynehays.cloudfilestorage.controller;

import com.waynehays.cloudfilestorage.parser.MultipartFileDataParser;
import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.request.resource.DeleteRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.DownloadRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.GetInfoRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.MoveRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.SearchRequest;
import com.waynehays.cloudfilestorage.dto.request.resource.UploadRequest;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
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
import org.springframework.validation.annotation.Validated;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {
    private final MultipartFileDataParser multipartFileDataParser;
    private final ResourceDeletionServiceApi deletionService;
    private final ResourceDownloadServiceApi downloadService;
    private final ResourceInfoServiceApi infoService;
    private final ResourceMoveServiceApi moveService;
    private final ResourceSearchServiceApi searchService;
    private final ResourceUploadServiceApi uploadService;

    @GetMapping()
    public ResourceDto getResourceInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       @Valid GetInfoRequest getInfoRequest) {
        return infoService.getInfo(userDetails.id(), getInfoRequest.path());
    }

    @DeleteMapping()
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @Valid DeleteRequest deleteRequest) {
        deletionService.delete(userDetails.id(), deleteRequest.path());
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                  @Valid DownloadRequest downloadRequest) {
        DownloadResult result = downloadService.download(userDetails.id(), downloadRequest.path());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.name() + "\"")
                .body(result.body());
    }

    @PutMapping("/move")
    public ResourceDto moveOrRenameResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @Valid MoveRequest moveRequest) {
        return moveService.move(userDetails.id(), moveRequest.from(), moveRequest.to());
    }

    @GetMapping("/search")
    public List<ResourceDto> searchResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @Valid SearchRequest searchRequest) {
        return searchService.search(userDetails.id(), searchRequest.query());
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceDto> uploadResource(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @Valid UploadRequest uploadRequest,
                                                            @RequestParam("object") List<MultipartFile> objects) {
        List<UploadObjectDto> uploadObjects = objects.stream()
                .map(file -> multipartFileDataParser.parse(file, uploadRequest.path()))
                .toList();
        return uploadService.upload(userDetails.id(), uploadObjects);
    }
}
