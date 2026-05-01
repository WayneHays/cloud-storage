package com.waynehays.cloudfilestorage.files.api.directory;

import com.waynehays.cloudfilestorage.core.user.CustomUserDetails;
import com.waynehays.cloudfilestorage.files.dto.request.directory.CreateDirectoryRequest;
import com.waynehays.cloudfilestorage.files.dto.request.directory.GetDirectoryContentRequest;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.directory.DirectoryServiceApi;
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
    public List<ResourceDto> getContent(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @Valid GetDirectoryContentRequest request) {
        return directoryService.getContent(userDetails.id(), request.path());
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceDto createDirectory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       @Valid CreateDirectoryRequest request) {
        return directoryService.createDirectory(userDetails.id(), request.path());
    }
}
