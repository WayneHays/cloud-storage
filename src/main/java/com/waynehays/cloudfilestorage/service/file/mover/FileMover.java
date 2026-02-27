package com.waynehays.cloudfilestorage.service.file.mover;

import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;

public interface FileMover {

    ResourceDto move(Long userId, String directoryFrom, String directoryTo);
}
