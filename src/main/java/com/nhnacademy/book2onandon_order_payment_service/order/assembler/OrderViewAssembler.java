package com.nhnacademy.book2onandon_order_payment_service.order.assembler;

import com.nhnacademy.book2onandon_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.DeliveryAddressResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderDetailResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemCreateResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import java.util.List;

/**
 * 하나의 엔티티에서 여러 응답DTO를 만들기 위한 클래스
 */
public interface OrderViewAssembler {
    OrderPrepareResponseDto toOrderPrepareView(Order order);
    OrderCreateResponseDto toOrderCreateView(Order order);
    OrderDetailResponseDto toOrderDetailView(Order order, List<BookOrderResponse> bookOrderResponseList);
    OrderItemCreateResponseDto toOrderItemCreateView(OrderItem orderItem);
    OrderItemResponseDto toOrderItemView(OrderItem orderItem, BookOrderResponse bookOrder);
    DeliveryAddressResponseDto toDeliveryAddressView(DeliveryAddress deliveryAddress);
}
