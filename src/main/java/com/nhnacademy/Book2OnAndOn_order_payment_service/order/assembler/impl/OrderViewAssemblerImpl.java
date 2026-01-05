package com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.impl;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderDetailResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OrderViewAssemblerImpl implements OrderViewAssembler {
    @Override
    public OrderPrepareResponseDto toOrderPrepareView(Order order) {
        return null;
    }

    @Override
    public OrderCreateResponseDto toOrderCreateView(Order order) {
        DeliveryAddressResponseDto deliveryAddressResponseDto = toDeliveryAddressView(order.getDeliveryAddress());
        List<OrderItemCreateResponseDto> orderItemCreateResponseDtoList = order.getOrderItems()
                .stream()
                .map(this::toOrderItemCreateView)
                .toList();

        return new OrderCreateResponseDto(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderTitle(),
                order.getOrderDateTime(),
                order.getTotalItemAmount(),
                order.getDeliveryFee(),
                order.getWrappingFee(),
                order.getCouponDiscount(),
                order.getPointDiscount(),
                order.getTotalDiscountAmount(),
                order.getTotalAmount(),
                order.getWantDeliveryDate(),
                orderItemCreateResponseDtoList,
                deliveryAddressResponseDto
        );
    }

    @Override
    public OrderDetailResponseDto toOrderDetailView(Order order, List<BookOrderResponse> bookOrderResponseList) {
        List<OrderItem> orderItemList = order.getOrderItems();

        Map<Long, BookOrderResponse> bookMap = bookOrderResponseList.stream()
                .collect(Collectors.toMap(BookOrderResponse::getBookId, Function.identity()));

        List<OrderItemResponseDto> orderItemResponseDtoList = orderItemList.stream()
                .map(item -> {
                    BookOrderResponse book = bookMap.get(item.getBookId());
                    if(book == null) throw new OrderVerificationException("책 정보가 일치하지 않습니다");

                    return toOrderItemView(item, book);
                })
                .toList();

        return new OrderDetailResponseDto(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus().getDescription(),
                order.getOrderDateTime(),
                order.getTotalAmount(),
                order.getTotalDiscountAmount(),
                order.getTotalItemAmount(),
                order.getDeliveryFee(),
                order.getWrappingFee(),
                order.getCouponDiscount(),
                order.getPointDiscount(),
                order.getWantDeliveryDate(),
                orderItemResponseDtoList,
                toDeliveryAddressView(order.getDeliveryAddress()),
                null
        );
    }

    @Override
    public OrderItemCreateResponseDto toOrderItemCreateView(OrderItem orderItem) {
        return new OrderItemCreateResponseDto (
                    orderItem.getOrderItemId(),
                    orderItem.getBookId(),
                    orderItem.getOrderItemQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.isWrapped(),
                    orderItem.getOrderItemStatus(),
                    orderItem.isWrapped() && (orderItem.getWrappingPaper() != null)
                            ? orderItem.getWrappingPaper().getWrappingPaperId()
                            : null
        );
    }

    @Override
    public OrderItemResponseDto toOrderItemView(OrderItem orderItem, BookOrderResponse bookOrder) {
        return new OrderItemResponseDto(
                orderItem.getOrderItemId(),
                orderItem.getBookId(),
                bookOrder.getTitle(),
                bookOrder.getImageUrl(),
                orderItem.getOrderItemQuantity(),
                bookOrder.getPriceSales().intValue(),
                orderItem.isWrapped(),
                orderItem.getOrderItemStatus().getDescription(),
                orderItem.isWrapped() && (orderItem.getWrappingPaper() != null)
                        ? orderItem.getWrappingPaper().getWrappingPaperId()
                        : null
        );
    }

    @Override
    public DeliveryAddressResponseDto toDeliveryAddressView(DeliveryAddress deliveryAddress) {
        return new DeliveryAddressResponseDto(
                    deliveryAddress.getDeliveryAddress(),
                    deliveryAddress.getDeliveryAddressDetail(),
                    deliveryAddress.getDeliveryMessage(),
                    deliveryAddress.getRecipient(),
                    deliveryAddress.getRecipientPhoneNumber()
        );
    }
}
