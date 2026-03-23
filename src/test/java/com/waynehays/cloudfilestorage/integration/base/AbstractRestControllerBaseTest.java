package com.waynehays.cloudfilestorage.integration.base;

import com.waynehays.cloudfilestorage.repository.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractRestControllerBaseTest extends AbstractIntegrationBaseTest{
    protected static final String RESOURCE_PATH = "/api/resource";
    protected static final String DIRECTORY_PATH = "/api/directory";

    private static final String SIGN_UP_PATH = "/api/auth/sign-up";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MinioTestCleaner minioCleaner;

    @Autowired
    protected ResourceMetadataRepository metadataRepository;

    @AfterEach
    void cleanStorage() {
        metadataRepository.deleteAll();
        userRepository.deleteAll();
        minioCleaner.deleteAll();
    }

    protected void uploadFile(Cookie sessionCookie, String directory, String filename, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                filename,
                "text/plain",
                content);

        mockMvc.perform(multipart(RESOURCE_PATH)
                        .file(file)
                        .param("path", directory)
                        .cookie(sessionCookie))
                .andExpect(status().isCreated());
    }

    protected void createDirectory(Cookie sessionCookie, String path) throws Exception {
        mockMvc.perform(post(DIRECTORY_PATH)
                        .param("path", path)
                        .cookie(sessionCookie))
                .andExpect(status().isCreated());
    }

    protected void createDirectoryAndExpectSuccess(Cookie sessionCookie, String path) throws Exception {
        String parentPath = PathUtils.extractParentPath(path);
        String name = PathUtils.extractFilename(PathUtils.removeTrailingSeparator(path));

        mockMvc.perform(post(DIRECTORY_PATH)
                        .param("path", path)
                        .cookie(sessionCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(parentPath))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
    }

    protected Cookie registerAndLoginDefaultUser() throws Exception {
        String requestBody = """
                {
                    "username": "user",
                    "password": "password"
                }
                """;

        MvcResult result = mockMvc.perform(post(SIGN_UP_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getCookie("SESSION");
    }
}
