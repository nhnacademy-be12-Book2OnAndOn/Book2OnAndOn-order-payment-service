package com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
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
    private long createdAt;
    private long updatedAt;

    // 생성 메서드 : createdAt과 updatedAt의 초기값 설정
    public static CartRedisItem newItem(Long bookId, int quantity, boolean selected) {
        long now = System.currentTimeMillis();
        return new CartRedisItem(bookId, quantity, selected, now, now);
    }

    // 갱신 메서드 : 장바구니 항목이 수정될 때 갱신 시간을 기록
    public void touchUpdatedAt() {
        this.updatedAt = System.currentTimeMillis();
    }
}

