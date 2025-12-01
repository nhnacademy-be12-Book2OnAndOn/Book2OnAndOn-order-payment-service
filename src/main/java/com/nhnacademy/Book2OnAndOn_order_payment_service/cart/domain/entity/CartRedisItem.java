package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// Redis 캐시에 올려두는 단순 캐시 DTO
// CartRedisItem은 Redis에 그대로 값으로 저장되는 객체라서, Java 직렬화 대상으로써 Serializable을 구현하도록 요구하는 경우가 많음.
public class CartRedisItem implements Serializable {
    // Redis에 저장되는 값(value) 객체 -> 빠른 조회 + 필요 최소 필드만 저장
    @NotNull
    private Long bookId;
    @Max(99)
    @Min(1)
    private int quantity;
    private boolean selected;
//    private LocalDateTime createdAt;
//
//    public CartRedisItem(Long bookId, int quantity, boolean selected) {
//        this(bookId, quantity, selected, LocalDateTime.now());
//    }
    private long updatedAt;
}

