package com.waynehays.cloudfilestorage.core.quota.entity;

import com.waynehays.cloudfilestorage.core.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "storage_quotas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageQuota extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storage_quotas_seq")
    @SequenceGenerator(name = "storage_quotas_seq", sequenceName = "storage_quotas_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private long usedSpace = 0;

    @Column(nullable = false)
    private long storageLimit;

    public StorageQuota(Long userId, long storageLimit) {
        if (storageLimit <= 0) {
            throw new IllegalArgumentException("storageLimit must be positive");
        }
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.storageLimit = storageLimit;
    }

    public void setUsedSpace(long usedSpace) {
        if (usedSpace < 0) {
            throw new IllegalArgumentException("usedSpace must not be negative");
        }
        this.usedSpace = usedSpace;
    }
}
