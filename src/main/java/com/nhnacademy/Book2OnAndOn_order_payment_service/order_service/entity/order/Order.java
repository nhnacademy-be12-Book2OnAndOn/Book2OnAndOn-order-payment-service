package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.converter.OrderStatusConverter;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.return1.Return;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.delivery.Delivery;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 12, unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "order_datatime", nullable = false)
    private LocalDateTime orderDatetime;

    @Convert(converter = OrderStatusConverter.class)
    private OrderStatus orderStatus;


    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Delivery delivery;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private GuestOrder guestOrder;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Return> returns = new ArrayList<>();

}