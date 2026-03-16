package com.waynehays.cloudfilestorage.integration.base;

import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.integration.config.container.MinioTestContainerInitializer;
import com.waynehays.cloudfilestorage.integration.config.container.PostgresTestContainerInitializer;
import com.waynehays.cloudfilestorage.integration.config.initializer.MinioInitializer;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractRestControllerBaseTest {
    protected static final String RESOURCE_PATH = "/api/resource";
    protected static final String DIRECTORY_PATH = "/api/directory";

    private static final String SIGN_UP_PATH = "/api/auth/sign-up";
    private static final String TEST_BUCKET_NAME = "test-bucket";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MinioTestCleaner minioCleaner;

    @Autowired
    protected FileStorageApi fileStorage;

    @AfterEach
    void cleanStorage() {
        userRepository.deleteAll();
        minioCleaner.deleteAll();
    }

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainerInitializer::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainerInitializer::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainerInitializer::getPassword);
        registry.add("minio.security.url", MinioTestContainerInitializer::getUrl);
        registry.add("minio.security.access-key", MinioTestContainerInitializer::getUser);
        registry.add("minio.security.secret-key", MinioTestContainerInitializer::getPassword);
        registry.add("minio.storage.bucket-name", () -> TEST_BUCKET_NAME);
    }

    @BeforeAll
    static void initBucket() {
        MinioInitializer.createBucket(TEST_BUCKET_NAME);
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
