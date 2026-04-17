package com.waynehays.cloudfilestorage.integration.repository;

import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.integration.container.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
public abstract class AbstractRepositoryTest {

    @Autowired
    protected TestEntityManager em;

    protected Long userId;
    protected Long otherUserId;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainer::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainer::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainer::getPassword);
    }

    @BeforeEach
    void persistUsers() {
        userId = persistUser("user");
        otherUserId = persistUser("other");
    }

    private Long persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        em.persistAndFlush(user);
        return user.getId();
    }

    protected ResourceMetadata file(Long userId, String path, String parentPath, String name, long size) {
        ResourceMetadata r = new ResourceMetadata();
        r.setUserId(userId);
        r.setPath(path);
        r.setNormalizedPath(path.toLowerCase());
        r.setParentPath(parentPath.toLowerCase());
        r.setName(name);
        r.setType(ResourceType.FILE);
        r.setSize(size);
        r.setMarkedForDeletion(false);
        return r;
    }

    protected ResourceMetadata directory(Long userId, String path, String parentPath, String name) {
        ResourceMetadata r = new ResourceMetadata();
        r.setUserId(userId);
        r.setPath(path);
        r.setNormalizedPath(path.toLowerCase());
        r.setParentPath(parentPath.toLowerCase());
        r.setName(name);
        r.setType(ResourceType.DIRECTORY);
        r.setSize(0L);
        r.setMarkedForDeletion(false);
        return r;
    }
}
