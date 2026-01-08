package com.nhnacademy.book2onandon_order_payment_service.config;

import com.nhnacademy.book2onandon_order_payment_service.exception.ClockMovedBackwardsException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class Snowflake {

    private final long workerId;
    private final long datacenterId;

    private static final long TWEPOCH = LocalDateTime.of(2025, 12, 5, 0, 0, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = -1L ^ (-1L << DATACENTER_ID_BITS);

    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIME_STAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public Snowflake() {
        this(1, 1); // 기본값
    }

    public Snowflake(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0)
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0",
                    MAX_WORKER_ID));

        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0)
            throw new IllegalArgumentException(
                    String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));

        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new ClockMovedBackwardsException(
                    "Clock moved backwards. Refusing to generate id!");
        }


        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;

            if (sequence == 0)
                timestamp = tilNextMillis(lastTimestamp);

        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - TWEPOCH) << TIME_STAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp)
            timestamp = timeGen();

        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}
