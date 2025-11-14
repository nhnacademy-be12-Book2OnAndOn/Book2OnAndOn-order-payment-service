package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 배송 정보를 기록
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Delivery")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryId;

    private Long orderId;

    private String deliveryStatus;

    private String trackingNumber; // 운송장 번호

//    // 배송 시작일
//    private LocalDateTime deliveryStartAt;
//
//    // 배송 완료일
//    private LocalDateTime deliveryCompleteAt;
//
//    배송 시작일 완료일 필요시 추가
    
    /** DeliveryPolicy FK 등 추가 필요*/
}