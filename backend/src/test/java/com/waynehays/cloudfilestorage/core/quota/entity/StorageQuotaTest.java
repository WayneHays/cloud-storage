package com.waynehays.cloudfilestorage.core.quota.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageQuotaTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should create quota with usedSpace = 0 by default")
        void shouldCreateQuotaWithZeroUsedSpace() {
            StorageQuota quota = new StorageQuota(1L, 1000L);

            assertThat(quota.getUserId()).isEqualTo(1L);
            assertThat(quota.getStorageLimit()).isEqualTo(1000L);
            assertThat(quota.getUsedSpace()).isZero();
        }

        @Test
        @DisplayName("Should throw NPE when userId is null")
        void shouldThrowWhenUserIdNull() {
            assertThatThrownBy(() -> new StorageQuota(null, 1000L))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("Should throw IAE when storageLimit is zero")
        void shouldThrowWhenStorageLimitZero() {
            assertThatThrownBy(() -> new StorageQuota(1L, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("storageLimit");
        }

        @Test
        @DisplayName("Should throw IAE when storageLimit is negative")
        void shouldThrowWhenStorageLimitNegative() {
            assertThatThrownBy(() -> new StorageQuota(1L, -1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("storageLimit");
        }
    }

    @Nested
    @DisplayName("setUsedSpace")
    class SetUsedSpace {

        @Test
        @DisplayName("Should accept zero")
        void shouldAcceptZero() {
            StorageQuota quota = new StorageQuota(1L, 1000L);
            quota.setUsedSpace(0L);

            assertThat(quota.getUsedSpace()).isZero();
        }

        @Test
        @DisplayName("Should accept positive value")
        void shouldAcceptPositiveValue() {
            StorageQuota quota = new StorageQuota(1L, 1000L);
            quota.setUsedSpace(500L);

            assertThat(quota.getUsedSpace()).isEqualTo(500L);
        }

        @Test
        @DisplayName("Should throw IAE when negative")
        void shouldThrowWhenNegative() {
            StorageQuota quota = new StorageQuota(1L, 1000L);

            assertThatThrownBy(() -> quota.setUsedSpace(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("usedSpace");
        }
    }
}