package com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@MappedSuperclass
//@MappedSuperclass의 역할은:
//이 클래스를 테이블로 만들지 말고
//상속한 엔티티에 필드를 그대로 물려줘라
//JPA 매핑 정보(createdAt, updatedAt)도 상속하라.
public abstract class BaseTimeEntity { // 중복 코드 제거 + 자동화된 시간 관리

    @Column(updatable = false)
    protected LocalDateTime createdAt;

    protected LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
