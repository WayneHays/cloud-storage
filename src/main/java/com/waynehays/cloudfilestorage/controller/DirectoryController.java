package com.waynehays.cloudfilestorage.controller;

import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import com.waynehays.cloudfilestorage.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/directory")
@RequiredArgsConstructor
public class DirectoryController {
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<List<ResourceDto>> getInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                     @RequestParam("path") String path) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(fileService.getDirectoryContent(userDetails.id(), path));
    }
}
