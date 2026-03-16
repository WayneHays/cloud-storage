package com.waynehays.cloudfilestorage.controller;

import com.waynehays.cloudfilestorage.dto.request.directory.CreateDirectoryRequest;
import com.waynehays.cloudfilestorage.dto.request.directory.GetDirectoryContentRequest;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.service.directory.DirectoryServiceApi;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
public class DirectoryController {
    private final DirectoryServiceApi directoryService;

    @GetMapping
    public ResponseEntity<List<ResourceDto>> getContent(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @Valid GetDirectoryContentRequest getContentRequest) {
        return ResponseEntity.ok()
                .body(directoryService.getContent(userDetails.id(), getContentRequest.path()));
    }

    @PostMapping
    public ResponseEntity<ResourceDto> createDirectory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       @Valid CreateDirectoryRequest createRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(directoryService.createDirectory(userDetails.id(), createRequest.path()));
    }
}
