package com.waynehays.cloudfilestorage.integration.base;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.auth.request.SignInRequest;
import com.waynehays.cloudfilestorage.dto.auth.request.SignUpRequest;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.integration.config.MinioTestContainer;
import com.waynehays.cloudfilestorage.integration.config.PostgresTestContainer;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
public abstract class AbstractControllerIntegrationTest {
    private static final String SIGN_UP_URL = "/api/auth/sign-up";
    private static final String SIGN_IN_URL = "/api/auth/sign-in";
    private static final String UPLOAD_URL = "/api/resource";

    private static final String DEFAULT_USERNAME = "testuser";
    private static final String DEFAULT_PASSWORD = "password123";
    private static final String DEFAULT_FILENAME = "file";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected FileInfoRepository fileInfoRepository;

    @Autowired
    protected FileStorage fileStorage;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    protected Cookie sessionCookie;
    protected Long userId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainer::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainer::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainer::getPassword);
        registry.add("minio.security.url", MinioTestContainer::getUrl);
        registry.add("minio.security.accessKey", MinioTestContainer::getUser);
        registry.add("minio.security.secretKey", MinioTestContainer::getPassword);
        registry.add("minio.storage.bucketName", MinioTestContainer::getBucket);
    }

    @BeforeEach
    void registerUser() throws Exception {
        sessionCookie = registerAndLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        userId = getUserId(DEFAULT_USERNAME);
    }

    @AfterEach
    void cleanUp() {
        fileInfoRepository.findAll().forEach(fi -> {
            try {
                fileStorage.delete(fi.getStorageKey());
            } catch (Exception ignored) {
            }
        });
        fileInfoRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected Cookie registerAndLogin(String username, String password) throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest(username, password);
        mockMvc.perform(post(SIGN_UP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        SignInRequest signInRequest = new SignInRequest(username, password);
        MvcResult result = mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andReturn();

        return result.getResponse().getCookie("SESSION");
    }

    protected Long getUserId(String username) {
        return userRepository.findByUsername(username).orElseThrow().getId();
    }

    protected ResultActions uploadFile(String filename, String content, String directory) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                DEFAULT_FILENAME, filename, MediaType.TEXT_PLAIN_VALUE, content.getBytes());

        var request = multipart(UPLOAD_URL)
                .file(file)
                .cookie(sessionCookie);

        if (directory != null) {
            request.param("path", directory);
        }

        return mockMvc.perform(request);
    }

    protected static String join(String... parts) {
        return String.join(Constants.PATH_SEPARATOR, parts);
    }
}
