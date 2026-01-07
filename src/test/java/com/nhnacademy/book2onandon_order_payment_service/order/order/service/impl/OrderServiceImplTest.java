package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.OrderNumberProvider;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.impl.OrderServiceImpl;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock private OrderRepository orderRepository;
    @Mock private DeliveryPolicyRepository deliveryPolicyRepository;
    @Mock private GuestOrderRepository guestOrderRepository;
    @Mock private WrappingPaperService wrappingPaperService;
    @Mock private PaymentService paymentService;
    @Mock private OrderTransactionService orderTransactionService;
    @Mock private BookServiceClient bookServiceClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private CouponServiceClient couponServiceClient;
    @Mock private OrderNumberProvider orderNumberProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OrderViewAssembler orderViewAssembler;
    @Mock private OrderResourceManager resourceManager;

    @Test
    @DisplayName("회원 주문 준비 데이터 조회 성공")
    void prepareOrder_Member_Success() {
        Long userId = 1L;
        BookInfoDto bookInfo = new BookInfoDto(100L, 2);
        OrderPrepareRequestDto req = new OrderPrepareRequestDto(List.of(bookInfo));
        BookOrderResponse bookResp = new BookOrderResponse();
        bookResp.getBookId();

        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(bookResp));
        given(userServiceClient.getUserAddresses(userId)).willReturn(List.of());
        given(couponServiceClient.getUsableCoupons(eq(userId), any())).willReturn(List.of());
        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(1000));

        OrderPrepareResponseDto result = orderService.prepareOrder(userId, req);

        assertThat(result).isNotNull();
        verify(userServiceClient).getUserPoint(userId);
    }

    @Test
    @DisplayName("주문 상세 조회 성공 - 회원/비회원 공통")
    void getOrderDetail_Success() {
        String orderNumber = "ORD-001";
        Order mockOrder = mock(Order.class);
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(mockOrder));
        given(mockOrder.getOrderItems()).willReturn(List.of());
        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of());
        given(orderViewAssembler.toOrderDetailView(any(), any())).willReturn(new OrderDetailResponseDto());
        given(paymentService.getPayment(any(PaymentRequest.class))).willReturn(mock(PaymentResponse.class));

        OrderDetailResponseDto result = orderService.getOrderDetail(1L, orderNumber, null);

        assertThat(result).isNotNull();
        verify(orderTransactionService).validateOrderExistence(any(), anyLong(), any());
    }

    @Test
    @DisplayName("주문 취소 성공 - 회원")
    void cancelOrder_Success() {
        Long userId = 1L;
        String orderNumber = "ORD-001";
        Order mockOrder = mock(Order.class);

        given(orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)).willReturn(Optional.of(mockOrder));
        given(mockOrder.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(mockOrder.getOrderNumber()).willReturn(orderNumber);

        orderService.cancelOrder(userId, orderNumber);

        verify(paymentService, times(1)).cancelPayment(any());
        verify(orderTransactionService, times(2)).changeStatusOrder(eq(mockOrder), eq(false));
        verify(resourceManager, times(1)).releaseResources(eq(orderNumber), eq(userId), anyInt(), any());
    }

    @Test
    @DisplayName("비회원 주문 취소 성공")
    void cancelGuestOrder_Success() {
        String orderNumber = "ORD-GUEST";
        String guestToken = "token";
        Order mockOrder = mock(Order.class);

        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(mockOrder));
        given(mockOrder.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(mockOrder.getOrderNumber()).willReturn(orderNumber);

        orderService.cancelGuestOrder(orderNumber, guestToken);

        verify(paymentService).cancelPayment(any());
        verify(orderTransactionService).validateOrderExistence(mockOrder, null, guestToken);
    }

    @Test
    @DisplayName("관리자 주문 목록 조회 성공")
    void getOrderListWithAdmin_Success() {
        Page<OrderSimpleDto> mockPage = mock(Page.class);
        given(orderRepository.findAllByAdmin(any(), any())).willReturn(mockPage);

        Page<OrderSimpleDto> result = orderService.getOrderListWithAdmin(mock(Pageable.class));

        assertThat(result).isEqualTo(mockPage);
    }

    @Test
    @DisplayName("주문 상태 변경 성공")
    void setOrderStatus_Success() {
        String orderNumber = "ORD-001";
        Order mockOrder = mock(Order.class);
        OrderStatusUpdateDto req = new OrderStatusUpdateDto(OrderStatus.SHIPPING);
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(mockOrder));

        orderService.setOrderStatus(orderNumber, req);

        verify(mockOrder).setOrderStatus(OrderStatus.SHIPPING);
    }

    @Test
    @DisplayName("주문 항목 상태 변경 성공")
    void setOrderItemStatus_Success() {
        String orderNumber = "ORD-001";
        Order mockOrder = mock(Order.class);
        OrderItem mockItem = mock(OrderItem.class);
        given(mockItem.getOrderItemId()).willReturn(10L);
        given(mockOrder.getOrderItems()).willReturn(List.of(mockItem));
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(mockOrder));

        orderService.setOrderItemStatus(orderNumber, new com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemStatusUpdateDto(10L, OrderItemStatus.ORDER_COMPLETE));

        verify(mockItem).setOrderItemStatus(OrderItemStatus.ORDER_COMPLETE);
    }

    @Test
    @DisplayName("취소 불가능한 상태의 주문 취소 시도 - 실패")
    void cancelOrder_Fail_InvalidStatus() {
        Order mockOrder = mock(Order.class);
        given(orderRepository.findByUserIdAndOrderNumber(anyLong(), anyString())).willReturn(Optional.of(mockOrder));
        given(mockOrder.getOrderStatus()).willReturn(OrderStatus.SHIPPING);

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "ORD-001"))
                .isInstanceOf(OrderNotCancellableException.class);
    }

    @Test
    @DisplayName("존재하지 않는 주문 조회 시도 - 실패")
    void getOrderDetail_Fail_NotFound() {
        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderDetail(1L, "NON-EXIST", null))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("스케줄러: 주문 정크 데이터 삭제 성공")
    void deleteJunkOrder_Success() {
        List<Long> ids = List.of(1L, 2L);
        given(orderRepository.deleteByIds(ids)).willReturn(2);

        int result = orderService.deleteJunkOrder(ids);

        assertThat(result).isEqualTo(2);
    }
}