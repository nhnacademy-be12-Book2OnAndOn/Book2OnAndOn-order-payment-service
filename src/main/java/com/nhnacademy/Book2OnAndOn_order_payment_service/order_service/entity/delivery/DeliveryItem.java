package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.delivery;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.order.OrderItem;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "DeliveryItem")
public class DeliveryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryItemId;

    @Column(name = "delivery_id", nullable = false)
    private Long deliveryId;

    @Column(name = "delivery_item_quantity", columnDefinition = "TINYINT", nullable = false)
    private int deliveryItemQuantity;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;
}