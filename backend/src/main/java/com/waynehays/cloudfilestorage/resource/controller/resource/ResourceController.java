package com.waynehays.cloudfilestorage.resource.controller.resource;

import com.waynehays.cloudfilestorage.resource.parser.MultipartFileParser;
import com.waynehays.cloudfilestorage.resource.dto.internal.DownloadResult;
import com.waynehays.cloudfilestorage.resource.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.resource.dto.request.DeleteRequest;
import com.waynehays.cloudfilestorage.resource.dto.request.DownloadRequest;
import com.waynehays.cloudfilestorage.resource.dto.request.GetInfoRequest;
import com.waynehays.cloudfilestorage.resource.dto.request.MoveRequest;
import com.waynehays.cloudfilestorage.resource.dto.request.SearchRequest;
import com.waynehays.cloudfilestorage.resource.dto.request.UploadRequest;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.auth.security.CustomUserDetails;
import com.waynehays.cloudfilestorage.resource.service.ResourceDeletionServiceApi;
import com.waynehays.cloudfilestorage.resource.service.ResourceDownloadServiceApi;
import com.waynehays.cloudfilestorage.resource.service.ResourceInfoServiceApi;
import com.waynehays.cloudfilestorage.resource.service.moving.ResourceMoveServiceApi;
import com.waynehays.cloudfilestorage.resource.service.ResourceSearchServiceApi;
import com.waynehays.cloudfilestorage.resource.service.uploading.ResourceUploadServiceApi;
import com.waynehays.cloudfilestorage.resource.validator.UploadRequestValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController implements ResourceControllerApi {
    private final MultipartFileParser multipartFileParser;
    private final UploadRequestValidator uploadRequestValidator;
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

        StreamingResponseBody body = switch (result) {
            case DownloadResult.File file -> out -> {
                try (InputStream in = file.contentSupplier().get()) {
                    in.transferTo(out);
                    log.info("Finished file download: userId={}, path={}", userDetails.id(), request.path());
                }
            };
            case DownloadResult.Archive archive -> archive.writer()::writeTo;
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(result.name()))
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
        List<UploadObjectDto> objects = multipartFileParser.parseAll(files, request.path());
        uploadRequestValidator.validate(objects);
        return uploadService.upload(userDetails.id(), objects);
    }

    private String contentDisposition(String filename) {
        String asciiFallback = filename.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "\\\"");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
    }
}
