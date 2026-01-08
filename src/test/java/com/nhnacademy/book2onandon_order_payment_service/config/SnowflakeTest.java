package com.nhnacademy.book2onandon_order_payment_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nhnacademy.book2onandon_order_payment_service.exception.ClockMovedBackwardsException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SnowflakeTest {

    @Test
    @DisplayName("기본 생성자로 생성된 Snowflake 객체는 ID를 정상적으로 생성한다")
    void nextId_DefaultConstructor_Success() {
        Snowflake snowflake = new Snowflake();
        long id = snowflake.nextId();
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("워커 ID와 데이터센터 ID가 최대값을 초과하면 생성 시 예외가 발생한다")
    void constructor_InvalidId_ThrowsException() {
        assertThatThrownBy(() -> new Snowflake(32, 1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Snowflake(1, 32))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Snowflake(-1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("동일한 밀리초 내에 여러 번 호출하면 시퀀스가 증가하며 서로 다른 ID를 생성한다")
    void nextId_SequenceIncrementsWithinSameMillis() {
        Snowflake snowflake = new Snowflake(1, 1);
        long id1 = snowflake.nextId();
        long id2 = snowflake.nextId();

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("생성된 ID는 구성 요소들이 비트 이동 로직에 따라 결합된 형태여야 한다")
    void nextId_BitLogicCheck() {
        Snowflake snowflake = new Snowflake(1, 1);
        long id = snowflake.nextId();
        
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("시간이 역행하는 상황(Clock moved backwards)이 발생하면 RuntimeException을 던진다")
    void nextId_ClockBackwards_ThrowsException() {
        Snowflake snowflake = new Snowflake(1, 1);

        long futureTime = System.currentTimeMillis() + 10000;
        ReflectionTestUtils.setField(snowflake, "lastTimestamp", futureTime);

        assertThatThrownBy(snowflake::nextId)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Clock moved backwards");
    }

    @Test
    @DisplayName("동일 밀리초 내에 시퀀스가 최대치에 도달하면 다음 밀리초까지 기다린 후 ID를 생성한다")
    void nextId_TilNextMillis_Wait() {
        Snowflake snowflake = new Snowflake(1, 1);
        
        for (int i = 0; i < 5000; i++) {
            long id = snowflake.nextId();
            assertThat(id).isPositive();
        }
    }

    @Test
    @DisplayName("시계가 뒤로 이동하면 ClockMovedBackwardsException 발생")
    void nextId_ShouldThrowClockMovedBackwardsException_WhenTimestampGoesBackwards() {
        Snowflake snowflake = new Snowflake();
        // given: lastTimestamp를 현재보다 큰 값으로 강제 세팅
        Field lastTimestampField = null;
        try {
            lastTimestampField = Snowflake.class.getDeclaredField("lastTimestamp");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        lastTimestampField.setAccessible(true);

        long futureTimestamp = System.currentTimeMillis() + 1000; // 미래 시간
        try {
            lastTimestampField.setLong(snowflake, futureTimestamp);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // when & then: nextId() 호출 시 예외 발생
        assertThrows(ClockMovedBackwardsException.class, snowflake::nextId);

    }
}