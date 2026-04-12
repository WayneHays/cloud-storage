package com.waynehays.cloudfilestorage.integration.base;

import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.repository.metadata.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.repository.quota.StorageQuotaRepository;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractRestControllerBaseTest extends AbstractIntegrationBaseTest {
    protected static final String PATH_RESOURCE = "/api/resource";
    protected static final String PATH_DIRECTORY = "/api/directory";
    protected static final String PATH_SIGN_UP = "/api/auth/sign-up";
    protected static final String PATH_SIGN_OUT = "/api/auth/sign-out";
    protected static final String PARAM_PATH = "path";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected StorageQuotaRepository quotaRepository;

    @Autowired
    protected ResourceMetadataRepository metadataRepository;

    @AfterEach
    void cleanStorage() {
        metadataRepository.deleteAll();
        quotaRepository.deleteAll();
        userRepository.deleteAll();
        cleanMinioBucket();
    }

    protected String buildBody(String username, String password) {
        return """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);
    }

    protected void uploadFile(Cookie sessionCookie, String directory, String filename, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "object",
                filename,
                "text/plain",
                content);

        mockMvc.perform(multipart(PATH_RESOURCE)
                        .with(csrf())
                        .file(file)
                        .param(PARAM_PATH, directory)
                        .cookie(sessionCookie))
                .andExpect(status().isCreated());
    }

    protected void createDirectory(Cookie sessionCookie, String path) throws Exception {
        mockMvc.perform(post(PATH_DIRECTORY)
                        .with(csrf())
                        .param(PARAM_PATH, path)
                        .cookie(sessionCookie))
                .andExpect(status().isCreated());
    }

    protected Cookie registerAndLoginDefaultUser() throws Exception {
        String requestBody = buildBody("user", "password");
        MvcResult result = mockMvc.perform(post(PATH_SIGN_UP)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getCookie("SESSION");
    }

    protected ResultActions getDirectoryContent(Cookie sessionCookie, String path) throws Exception {
        return mockMvc.perform(get(PATH_DIRECTORY)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(sessionCookie));
    }
}
