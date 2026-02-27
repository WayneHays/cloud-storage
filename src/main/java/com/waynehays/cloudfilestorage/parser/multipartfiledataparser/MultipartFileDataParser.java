package com.waynehays.cloudfilestorage.parser.multipartfiledataparser;

import com.waynehays.cloudfilestorage.dto.file.FileData;
import org.springframework.web.multipart.MultipartFile;

public interface MultipartFileDataParser {

    FileData parse(MultipartFile file, String directory);
}
