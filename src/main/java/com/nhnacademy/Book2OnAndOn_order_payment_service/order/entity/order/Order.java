package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.converter.OrderStatusConverter;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1.Return;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "Orders")
public class Order {
    // 속성
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    @NotNull
    private Long userId;

    @Column(name = "order_number", unique = true, columnDefinition = "CHAR(12)")
    @NotNull
    private String orderNumber;

    @Column(name = "order_datatime")
    @NotNull
    private LocalDateTime orderDatetime = LocalDateTime.now();

    @Column(name = "order_status")
    @NotNull
    private OrderStatus orderStatus;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "total_discount_amount")
    private Integer totalDiscountAmount = 0;

    @Column(name = "total_item_amount")
    private Integer totalItemAmount;

    @Column(name = "delivery_fee")
    private Integer deliveryFee;

    @Column(name = "wrapping_fee")
    private Integer wrappingFee = 0;

    @Column(name = "coupon_discount")
    private Integer couponDiscount = 0;

    @Column(name = "point_discount")
    private Integer pointDiscount = 0;


    // 양방향 연관관계
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private DeliveryAddress deliveryAddress;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private GuestOrder guestOrder;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Delivery> deliveries = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Return> returns = new ArrayList<>();
}