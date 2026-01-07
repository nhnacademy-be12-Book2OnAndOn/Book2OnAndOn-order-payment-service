package com.nhnacademy.book2onandon_order_payment_service.order.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.book2onandon_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.BookOrderContext;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderItemFactory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderItemFactoryTest {

    private final OrderItemFactory orderItemFactory = new OrderItemFactory();

    @Test
    @DisplayName("요청 리스트와 도서 컨텍스트를 기반으로 주문 상품 엔티티 리스트를 생성한다")
    void create_Success() {
        Long bookId = 1L;
        OrderItemRequestDto req = mock(OrderItemRequestDto.class);
        BookOrderContext context = mock(BookOrderContext.class);
        BookOrderResponse bookResponse = mock(BookOrderResponse.class);

        given(req.getBookId()).willReturn(bookId);
        given(req.getQuantity()).willReturn(2);
        given(req.isWrapped()).willReturn(true);
        
        given(context.get(bookId)).willReturn(bookResponse);
        given(bookResponse.getBookId()).willReturn(bookId);
        given(bookResponse.getPriceSales()).willReturn(15000L);

        List<OrderItem> result = orderItemFactory.create(List.of(req), context);

        assertThat(result).hasSize(1);
        OrderItem item = result.get(0);
        assertThat(item.getBookId()).isEqualTo(bookId);
        assertThat(item.getUnitPrice()).isEqualTo(15000);
        assertThat(item.getOrderItemQuantity()).isEqualTo(2);
        assertThat(item.isWrapped()).isTrue();
    }

    @Test
    @DisplayName("빈 요청 리스트가 전달되면 빈 결과 리스트를 반환한다")
    void create_EmptyRequest() {
        BookOrderContext context = mock(BookOrderContext.class);

        List<OrderItem> result = orderItemFactory.create(List.of(), context);

        assertThat(result).isEmpty();
    }
}