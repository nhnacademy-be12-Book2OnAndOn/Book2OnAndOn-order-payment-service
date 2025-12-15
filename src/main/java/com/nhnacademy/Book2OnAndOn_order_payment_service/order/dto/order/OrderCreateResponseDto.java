package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemResponseDto;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateResponseDto {
    private Long orderId;
    private String orderNumber;
    private String orderTitle;
    private int totalItemAmount;
    private int deliveryFee;
    private int wrappingFee;
    private int couponDiscount;
    private int pointDiscount;
    private int totalDiscountAmount;
    private int totalAmount;
    private LocalDate wantDeliveryDate;

    private List<OrderItemResponseDto> orderItems;
    private DeliveryAddressResponseDto deliveryAddressResponseDto;
}
