package com.waynehays.cloudfilestorage.component.parser;

import com.waynehays.cloudfilestorage.dto.ObjectData;
import org.springframework.web.multipart.MultipartFile;

public interface MultipartFileDataParserApi {

    ObjectData parse(MultipartFile file, String directory);
}
