package com.nhnacademy.Book2OnAndOn_order_payment_service.payment_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 *  결제 정보
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Payment")
public class Payment {
    @Id
    private String paymentKey;

    // 주문 참조
    private Long orderId;

 /**   / 결제 유형 (NORMAL, BILLING 등) /
    private
    / 결제 수단 (CARD, EASY_PAY 등) /
    private
    / 현재 결제 처리 상태 /
    private PaymentStatus paymentStatus;
    / 결제 서비스 제공 회사 /
    private PaymentProvider paymentProvider;

    ENUM값 받는 4개 처리  흠. ..
    */

    private int paymentTotalAmount;

    private int paymentBalanceAmount;

    private OffsetDateTime paymentRequestAt;

    private OffsetDateTime paymentApprovedAt;

}