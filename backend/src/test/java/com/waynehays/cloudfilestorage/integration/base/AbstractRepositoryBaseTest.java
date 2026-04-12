package com.waynehays.cloudfilestorage.integration.base;

import com.waynehays.cloudfilestorage.entity.User;
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
public class AbstractRepositoryBaseTest {

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
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        em.persist(user);
        em.flush();
        return user.getId();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestPostgres.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", TestPostgres.INSTANCE::getUsername);
        registry.add("spring.datasource.password", TestPostgres.INSTANCE::getPassword);
    }
}
