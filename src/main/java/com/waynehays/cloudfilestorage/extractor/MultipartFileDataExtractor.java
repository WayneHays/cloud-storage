package com.waynehays.cloudfilestorage.extractor;

import com.waynehays.cloudfilestorage.dto.files.FileData;
import org.springframework.web.multipart.MultipartFile;

public interface MultipartFileDataExtractor {

    FileData extract(MultipartFile file, String directory);
}
