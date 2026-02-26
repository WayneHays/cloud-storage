package com.waynehays.cloudfilestorage.controller;

import com.waynehays.cloudfilestorage.dto.files.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.EmptyFileException;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import com.waynehays.cloudfilestorage.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {
    private final FileService fileService;

    @PostMapping()
    public ResponseEntity<ResourceDto> uploadFile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @RequestParam(value = "path", required = false) String path,
                                                  MultipartFile file) {
        if (file.isEmpty()) {
            throw new EmptyFileException("File to upload is empty");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileService.uploadFile(userDetails.id(), path, file));
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @RequestParam(value = "path", required = false) String path) {
        FileDownloadDto file = fileService.downloadFile(userDetails.id(), path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.size())
                .body(new InputStreamResource(file.inputStream()));
    }
}
