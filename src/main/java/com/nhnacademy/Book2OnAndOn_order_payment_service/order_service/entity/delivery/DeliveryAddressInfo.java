package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.delivery;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.order.Order;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "DeliveryAddressInfo")
public class DeliveryAddressInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryAddressId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "delivery_address", length = 100, nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_address_detail", length = 100)
    private String deliveryAddressDetail;

    @Column(name = "delivery_message", length = 100)
    private String deliveryMessage;

    @Column(name = "recipient", length = 50, nullable = false)
    private String recipient;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
}