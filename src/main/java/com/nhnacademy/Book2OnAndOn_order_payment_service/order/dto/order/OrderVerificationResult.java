package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import java.time.LocalDate;
import java.util.List;

public record OrderVerificationResult(String orderNumber,
                                      String orderTitle,
                                      Integer totalAmount,
                                      Integer totalDiscountAmount,
                                      Integer totalItemAmount,
                                      Integer deliveryFee,
                                      Integer wrappingFee,
                                      Integer couponDiscount,
                                      Integer pointDiscount,
                                      LocalDate wantDeliveryDate,
                                      List<OrderItem> orderItemList,
                                      DeliveryAddress deliveryAddress) {
}
