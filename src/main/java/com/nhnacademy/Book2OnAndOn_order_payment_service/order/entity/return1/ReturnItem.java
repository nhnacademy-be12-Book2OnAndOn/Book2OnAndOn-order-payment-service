package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "ReturnItem")
public class ReturnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id")
    private Long returnItemId;

    @Column(name = "return_quantity", columnDefinition = "TINYINT")
    @NotNull
    private Integer returnQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id")
    @NotNull
    private Return returnEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    @NotNull
    private OrderItem orderItem;
}