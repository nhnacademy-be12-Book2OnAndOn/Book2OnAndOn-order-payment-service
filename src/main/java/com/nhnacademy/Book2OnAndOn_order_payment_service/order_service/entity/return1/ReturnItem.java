package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.return1;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.order.OrderItem;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ReturnItem")
public class ReturnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long returnItemId;

    @Column(name = "return_id", nullable = false)
    private Long returnId;

    @Column(name = "return_quantity", columnDefinition = "TINYINT", nullable = false)
    private int returnQuantity;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private Return returnEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;
}