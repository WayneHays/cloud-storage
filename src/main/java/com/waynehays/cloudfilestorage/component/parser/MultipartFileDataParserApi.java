package com.waynehays.cloudfilestorage.component.parser;

import com.waynehays.cloudfilestorage.dto.FileData;
import org.springframework.web.multipart.MultipartFile;

public interface MultipartFileDataParserApi {

    FileData parse(MultipartFile file, String directory);
}
