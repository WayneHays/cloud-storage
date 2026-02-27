package com.waynehays.cloudfilestorage.controller;

import com.waynehays.cloudfilestorage.dto.file.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.EmptyFileException;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import com.waynehays.cloudfilestorage.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
                .body(fileService.upload(userDetails.id(), path, file));
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @RequestParam(value = "path", required = false) String path) {

        FileDownloadDto file = fileService.download(userDetails.id(), path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s\"".formatted(file.filename()))
                .body(new InputStreamResource(file.inputStream()));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteFile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestParam("path") String path) {
        fileService.delete(userDetails.id(), path);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/move")
    public ResponseEntity<ResourceDto> moveFile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @RequestParam("from") String from,
                                                @RequestParam("to") String to) {

        return ResponseEntity.ok()
                .body(fileService.move(userDetails.id(), from, to));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceDto>> search(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @RequestParam("query") String query) {
        return ResponseEntity.ok().body(fileService.search(userDetails.id(), query));
    }
}
