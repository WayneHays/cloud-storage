package com.waynehays.cloudfilestorage.files.api;

import com.waynehays.cloudfilestorage.files.api.dto.request.CreateDirectoryRequest;
import com.waynehays.cloudfilestorage.files.api.dto.request.GetDirectoryContentRequest;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.operation.directory.DirectoryServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
class DirectoryController implements DirectoryControllerApi{
    private final DirectoryServiceApi directoryService;

    @Override
    @GetMapping
    public List<ResourceResponse> getContent(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @Valid GetDirectoryContentRequest request) {
        return directoryService.getContent(userDetails.id(), request.path());
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse createDirectory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @Valid CreateDirectoryRequest request) {
        return directoryService.createDirectory(userDetails.id(), request.path());
    }
}
