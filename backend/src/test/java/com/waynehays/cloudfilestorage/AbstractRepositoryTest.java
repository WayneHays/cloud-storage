package com.waynehays.cloudfilestorage;

import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.core.metadata.factory.ResourceMetadataFactory;
import com.waynehays.cloudfilestorage.core.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@Import({PostgresTestContainerConfig.class, RedisTestContainerConfig.class})
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
public abstract class AbstractRepositoryTest {
    private final ResourceMetadataFactory metadataFactory = new ResourceMetadataFactory();

    @Autowired
    protected TestEntityManager em;

    protected Long userId;
    protected Long otherUserId;

    @BeforeEach
    void persistUsers() {
        userId = persistUser("user");
        otherUserId = persistUser("other");
    }

    private Long persistUser(String username) {
        User user = new User(username, "password");
        em.persistAndFlush(user);
        return user.getId();
    }

    protected void persistFile(Long userId, String storageKey, String path, long size) {
        em.persistAndFlush(file(
                userId,
                storageKey,
                path,
                size));
    }

    protected void persistDirectory(Long userId, String path) {
        em.persistAndFlush(directory(userId, path));
    }

    protected ResourceMetadata file(Long userId, String storageKey, String path, long size) {
        return metadataFactory.createFile(userId, storageKey, path, size);
    }

    protected ResourceMetadata directory(Long userId, String path) {
        return metadataFactory.createDirectory(userId, path);
    }
}
