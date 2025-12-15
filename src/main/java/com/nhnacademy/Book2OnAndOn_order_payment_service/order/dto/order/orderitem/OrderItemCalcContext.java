package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemCalcContext{
    private Long bookId;
    private Integer quantity;
    private Integer unitPrice;
    private Long categoryId;
    private int itemTotalPrice;

    public static OrderItemCalcContext of(OrderItem orderItem, BookOrderResponse book){
        int total = orderItem.getOrderItemQuantity() * book.getPriceSales().intValue();

        return new OrderItemCalcContext(
                orderItem.getBookId(),
                orderItem.getOrderItemQuantity(),
                book.getPriceSales().intValue(),
                book.getCategoryId(),
                total
        );
    }
}
