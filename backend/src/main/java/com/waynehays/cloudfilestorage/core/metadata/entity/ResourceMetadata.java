package com.waynehays.cloudfilestorage.core.metadata.entity;

import com.waynehays.cloudfilestorage.core.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "resource_metadatas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceMetadata extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_metadata_seq")
    @SequenceGenerator(name = "resource_metadata_seq", sequenceName = "resource_metadata_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(length = 36, updatable = false)
    private String storageKey;

    @Setter
    @Column(nullable = false, length = 500)
    private String path;

    @Setter
    @Column(nullable = false, length = 500)
    private String normalizedPath;

    @Setter
    @Column(nullable = false, length = 500)
    private String parentPath;

    @Setter
    @Column(nullable = false, length = 200)
    private String name;

    private Long size;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private ResourceType type;

    @Setter
    @Column(nullable = false)
    private boolean markedForDeletion = false;

    public ResourceMetadata(Long userId, String path, String normalizedPath,
                            String parentPath, String name) {
        this(userId, path, normalizedPath, parentPath, name, ResourceType.DIRECTORY);
    }

    public ResourceMetadata(Long userId, String storageKey, String path,
                            String normalizedPath, String parentPath,
                            String name, Long size) {
        this(userId, path, normalizedPath, parentPath, name, ResourceType.FILE);

        Objects.requireNonNull(size, "size must not be null");
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
        this.storageKey = Objects.requireNonNull(storageKey, "storageKey must not be null");
        this.size = size;
    }

    private ResourceMetadata(Long userId, String path, String normalizedPath,
                             String parentPath, String name, ResourceType type) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.normalizedPath = Objects.requireNonNull(normalizedPath, "normalizedPath must not be null");
        this.parentPath = Objects.requireNonNull(parentPath, "parentPath must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }
}
