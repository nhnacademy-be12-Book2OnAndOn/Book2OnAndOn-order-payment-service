package com.nhnacademy.book2onandon_order_payment_service.order.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.book2onandon_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.book2onandon_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.book2onandon_order_payment_service.order.assembler.impl.OrderViewAssemblerImpl;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderDetailResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderViewAssemblerImplTest {

    private final OrderViewAssemblerImpl assembler = new OrderViewAssemblerImpl();

    @Test
    @DisplayName("Order 엔티티를 OrderCreateResponseDto로 정확하게 변환한다")
    void toOrderCreateViewTest() {
        Order order = mock(Order.class);
        DeliveryAddress address = mock(DeliveryAddress.class);
        OrderItem item = mock(OrderItem.class);

        given(order.getDeliveryAddress()).willReturn(address);
        given(order.getOrderItems()).willReturn(List.of(item));
        given(order.getOrderId()).willReturn(1L);
        given(order.getOrderNumber()).willReturn("ORD-100");
        given(item.isWrapped()).willReturn(false);
        given(item.getOrderItemStatus()).willReturn(OrderItemStatus.PENDING);

        OrderCreateResponseDto result = assembler.toOrderCreateView(order);

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getOrderNumber()).isEqualTo("ORD-100");
    }

    @Test
    @DisplayName("주문 상세 보기 변환 시 책 정보 리스트와 매핑 성공")
    void toOrderDetailView_Success() {
        Order order = mock(Order.class);
        OrderItem item = mock(OrderItem.class);
        BookOrderResponse book = mock(BookOrderResponse.class);

        given(order.getOrderItems()).willReturn(List.of(item));
        given(order.getDeliveryAddress()).willReturn(mock(DeliveryAddress.class));
        given(order.getOrderStatus()).willReturn(OrderStatus.PENDING);
        given(item.getBookId()).willReturn(10L);
        given(item.getOrderItemStatus()).willReturn(OrderItemStatus.PENDING);
        given(book.getBookId()).willReturn(10L);
        given(book.getTitle()).willReturn("테스트 도서");
        given(book.getPriceSales()).willReturn(10000L);

        OrderDetailResponseDto result = assembler.toOrderDetailView(order, List.of(book));

        assertThat(result.getOrderItems()).isNotEmpty();
        assertThat(result.getOrderItems().get(0).getBookTitle()).isEqualTo("테스트 도서");
    }

    @Test
    @DisplayName("상세 보기 변환 중 책 정보가 매칭되지 않으면 예외 발생 (Fail Path)")
    void toOrderDetailView_Fail_BookNotFound() {
        Order order = mock(Order.class);
        OrderItem item = mock(OrderItem.class);
        given(order.getOrderItems()).willReturn(List.of(item));
        given(item.getBookId()).willReturn(1L);

        assertThatThrownBy(() -> assembler.toOrderDetailView(order, List.of()))
                .isInstanceOf(OrderVerificationException.class)
                .hasMessageContaining("책 정보가 일치하지 않습니다");
    }

    @Test
    @DisplayName("포장지가 있는 주문 항목 변환 테스트")
    void toOrderItemView_WithWrapping() {
        OrderItem item = mock(OrderItem.class);
        BookOrderResponse book = mock(BookOrderResponse.class);
        WrappingPaper paper = mock(WrappingPaper.class);

        given(item.isWrapped()).willReturn(true);
        given(item.getWrappingPaper()).willReturn(paper);
        given(item.getOrderItemStatus()).willReturn(OrderItemStatus.PENDING);
        given(paper.getWrappingPaperId()).willReturn(5L);
        given(book.getPriceSales()).willReturn(1000L);

        var result = assembler.toOrderItemView(item, book);

        assertThat(result.getWrappingPaperId()).isEqualTo(5L);
        assertThat(result.isWrapped()).isTrue();
    }

    @Test
    @DisplayName("구현되지 않은 prepareView 호출 시 null 반환 확인")
    void toOrderPrepareView_ReturnsNull() {
        assertThat(assembler.toOrderPrepareView(null)).isNull();
    }
}