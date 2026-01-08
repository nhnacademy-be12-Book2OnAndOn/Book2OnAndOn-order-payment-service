
package com.nhnacademy.book2onandon_order_payment_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class SchedulerConfigTest {

    private final SchedulerConfig schedulerConfig = new SchedulerConfig();

    @Test
    @DisplayName("RedisConnectionFactory를 사용하여 RedisLockProvider Bean을 생성한다")
    void lockProvider_Success() {
        RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);

        LockProvider lockProvider = schedulerConfig.lockProvider(mockFactory);

        assertThat(lockProvider)
                .isNotNull()
                .isInstanceOf(RedisLockProvider.class);
    }

    @Test
    @DisplayName("SchedulerConfig 인스턴스가 정상적으로 생성된다")
    void configInstanceTest() {
        assertThat(schedulerConfig).isNotNull();
    }
}