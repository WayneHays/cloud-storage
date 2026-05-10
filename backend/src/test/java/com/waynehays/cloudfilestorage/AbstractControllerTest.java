package com.waynehays.cloudfilestorage;

import com.waynehays.cloudfilestorage.core.metadata.repository.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.core.quota.repository.StorageQuotaRepository;
import com.waynehays.cloudfilestorage.core.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import({PostgresTestContainerConfig.class, RedisTestContainerConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractControllerTest {
    protected static final String PATH_RESOURCE = "/api/resource";
    protected static final String PATH_DIRECTORY = "/api/directory";
    protected static final String PATH_SIGN_IN = "/api/auth/sign-in";
    protected static final String PATH_SIGN_UP = "/api/auth/sign-up";
    protected static final String PATH_SIGN_OUT = "/api/auth/sign-out";
    protected static final String PATH_MOVE = PATH_RESOURCE + "/move";
    protected static final String PATH_DOWNLOAD = PATH_RESOURCE + "/download";
    protected static final String PATH_SEARCH = PATH_RESOURCE + "/search";
    protected static final String PATH_ME = "/api/user/me";
    protected static final String PARAM_PATH = "path";
    protected static final String PARAM_FROM = "from";
    protected static final String PARAM_TO = "to";
    protected static final String PARAM_QUERY = "query";
    private static final String PARAM_FILES = "files";

    protected static final String DEFAULT_USER = "user";
    protected static final String DEFAULT_PASSWORD = "password";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("minio.security.url", MinioTestContainer::getUrl);
        registry.add("minio.security.access-key", MinioTestContainer::getUsername);
        registry.add("minio.security.secret-key", MinioTestContainer::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceMetadataRepository metadataRepository;

    @Autowired
    protected StorageQuotaRepository quotaRepository;

    @Autowired
    protected MockMvc mockMvc;

    protected Cookie sessionCookie;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        sessionCookie = registerAndLoginDefaultUser();
    }

    @AfterEach
    void cleanStorage() {
        metadataRepository.deleteAll();
        quotaRepository.deleteAll();
        userRepository.deleteAll();
        MinioTestContainer.cleanTestBucket();
    }

    protected String buildRequestBody(String username, String password) {
        return """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);
    }

    protected MockMultipartFile multipartFile(String filename, byte[] content) {
        return new MockMultipartFile(PARAM_FILES, filename, "text/plain", content);
    }

    protected ResultActions performUpload(Cookie session, String path, MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart(PATH_RESOURCE)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(session);
        for (MockMultipartFile file : files) {
            builder.file(file);
        }
        return mockMvc.perform(builder);
    }

    protected ResultActions uploadFile(Cookie sessionCookie, String directory, String filename, byte[] content) throws Exception {
        return performUpload(sessionCookie, directory, multipartFile(filename, content));
    }

    protected void uploadFileAndExpectIsCreated(Cookie sessionCookie, String directory, String filename, byte[] content) throws Exception {
        uploadFile(sessionCookie, directory, filename, content)
                .andExpect(status().isCreated());
    }

    protected ResultActions createDirectory(Cookie sessionCookie, String path) throws Exception {
        return mockMvc.perform(post(PATH_DIRECTORY)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(sessionCookie));
    }

    protected ResultActions getDirectoryContent(Cookie sessionCookie, String path) throws Exception {
        return mockMvc.perform(get(PATH_DIRECTORY)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(sessionCookie));
    }

    protected long getUsedSpaceForDefaultUser() {
        Long userId = userRepository.findByUsername(DEFAULT_USER)
                .orElseThrow()
                .getId();
        return quotaRepository.findAll().stream()
                .filter(q -> q.getUserId().equals(userId))
                .findFirst()
                .orElseThrow()
                .getUsedSpace();
    }

    private Cookie registerAndLoginDefaultUser() throws Exception {
        String requestBody = buildRequestBody(DEFAULT_USER, DEFAULT_PASSWORD);
        MvcResult result = mockMvc.perform(post(PATH_SIGN_UP)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getCookie("SESSION");
    }
}
