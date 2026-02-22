package com.waynehays.cloudfilestorage.parser.multipartfiledataparser;

import com.waynehays.cloudfilestorage.dto.files.FileData;
import org.springframework.web.multipart.MultipartFile;

public interface MultipartFileDataParser {

    FileData extract(MultipartFile file, String directory);
}
