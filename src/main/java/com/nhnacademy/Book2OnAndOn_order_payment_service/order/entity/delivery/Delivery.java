package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Delivery")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long deliveryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_company", length = 30)
    private DeliveryCompany deliveryCompany;

    @Column(name = "waybill", length = 20)
    private String waybill;

    @Column(name = "delivery_start_at")
    private LocalDateTime deliveryStartAt; // 운송장 등록 시간

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // 생성자 (주문 완료 시)
    public Delivery(Order order) {
        this.order = order;
    }

    // 운송장 등록
    public void registerWaybill(DeliveryCompany deliveryCompany, String waybill) {
        this.deliveryCompany = deliveryCompany;
        this.waybill = waybill;
        this.deliveryStartAt = LocalDateTime.now();

    }

    // 정보 수정
    public void updateTrackingInfo(DeliveryCompany deliveryCompany, String waybill) {
        this.deliveryCompany = deliveryCompany;
        this.waybill = waybill;
    }
}