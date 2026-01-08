package com.nhnacademy.book2onandon_order_payment_service.order.service;

import com.nhnacademy.book2onandon_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.BookOrderContext;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderItemFactory {
    public List<OrderItem> create(
            List<OrderItemRequestDto> reqItems,
            BookOrderContext bookContext
    ) {
        return reqItems.stream()
                .map(req -> {
                    BookOrderResponse book = bookContext.get(req.getBookId());

                    return OrderItem.builder()
                            .bookId(book.getBookId())
                            .unitPrice(book.getPriceSales().intValue())
                            .orderItemQuantity(req.getQuantity())
                            .isWrapped(req.isWrapped())
                            .build();
                })
                .toList();
    }
}
