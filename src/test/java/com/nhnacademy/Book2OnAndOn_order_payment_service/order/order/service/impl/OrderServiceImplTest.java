package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.OrderNumberProvider;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.impl.OrderServiceImpl;
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
import org.mockito.junit.jupiter.MockitoSettings; // 추가
import org.mockito.quality.Strictness; // 추가
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // [수정] 불필요한 스터빙 감지 오류를 무시하도록 설정
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
    @DisplayName("회원 주문 준비 데이터 조회 성공 (Happy Path)")
    void prepareOrder_Member_Success() {
        Long userId = 1L;
        OrderPrepareRequestDto req = new OrderPrepareRequestDto(List.of());

        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of());
        given(userServiceClient.getUserAddresses(userId)).willReturn(List.of());
        given(couponServiceClient.getUsableCoupons(eq(userId), any())).willReturn(List.of());
        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(1000));

        OrderPrepareResponseDto result = orderService.prepareOrder(userId, req);

        assertThat(result).isNotNull();
        assertThat(result.currentPoint().getCurrentPoint()).isEqualTo(1000);
    }

    @Test
    @DisplayName("비회원 주문 준비 데이터 조회 성공 (Happy Path)")
    void prepareOrder_Guest_Success() {
        OrderPrepareRequestDto req = new OrderPrepareRequestDto(List.of());
        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of());

        OrderPrepareResponseDto result = orderService.prepareGuestOrder("guest", req);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("주문 상세 조회 성공 (Happy Path)")
    void getOrderDetail_Success() {
        Long userId = 1L;
        String orderNumber = "ORD-001";
        Order mockOrder = mock(Order.class);

        given(orderRepository.findByOrderNumber(orderNumber))
                .willReturn(Optional.of(mockOrder));

        given(orderViewAssembler.toOrderDetailView(eq(mockOrder), anyList()))
                .willReturn(new OrderDetailResponseDto());

        given(paymentService.getPayment(any())).willReturn(mock(PaymentResponse.class));

        OrderDetailResponseDto result = orderService.getOrderDetail(userId, orderNumber, null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("주문 취소 성공 (Happy Path)")
    void cancelOrder_Success() {
        Long userId = 1L;
        String orderNumber = "ORD-001";
        Order mockOrder = mock(Order.class);

        given(orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)).willReturn(Optional.of(mockOrder));

        given(mockOrder.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(mockOrder.getOrderNumber()).willReturn(orderNumber);

        orderService.cancelOrder(userId, orderNumber);

        verify(paymentService).cancelPayment(any());
    }

    @Test
    @DisplayName("취소 불가능한 상태의 주문 취소 시도 (Fail Path)")
    void cancelOrder_Fail_InvalidStatus() {
        Long userId = 1L;
        String orderNumber = "ORD-001";
        Order mockOrder = mock(Order.class);

        given(orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)).willReturn(Optional.of(mockOrder));
        given(mockOrder.getOrderStatus()).willReturn(OrderStatus.CANCELED);

        assertThatThrownBy(() -> orderService.cancelOrder(userId, orderNumber))
                .isInstanceOf(OrderNotCancellableException.class);
    }

    @Test
    @DisplayName("존재하지 않는 주문 상세 조회 (Fail Path)")
    void getOrderDetail_Fail_NotFound() {
        Long userId = 1L;
        String orderNumber = "NON-EXIST";

        given(orderRepository.findByOrderNumber(orderNumber))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderDetail(userId, orderNumber, null))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("지정 배송일 범위 초과 시 예외 발생 (Fail Path)")
    void createPreOrder_Fail_InvalidDeliveryDate() {
        Long userId = 1L;
        OrderCreateRequestDto req = new OrderCreateRequestDto();

        OrderItemRequestDto itemReq = new OrderItemRequestDto(1L, 1, false, null);
        req.setOrderItems(List.of(itemReq));
        req.setWantDeliveryDate(LocalDate.now().plusMonths(1));

        req.setDeliveryPolicyId(1L);

        DeliveryAddressRequestDto addressReq = new DeliveryAddressRequestDto();
        addressReq.setRecipient("홍길동");
        addressReq.setDeliveryAddress("광주");
        req.setDeliveryAddress(addressReq);

        BookOrderResponse mockBook = mock(BookOrderResponse.class);
        given(mockBook.getBookId()).willReturn(1L);
        given(mockBook.getTitle()).willReturn("테스트 도서");
        given(mockBook.getPriceStandard()).willReturn(10000L);
        given(mockBook.getPriceSales()).willReturn(9000L);

        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(mockBook));

        DeliveryPolicy mockPolicy = mock(DeliveryPolicy.class);

        // [수정] lenient 대신 표준 when 사용 (클래스 레벨에서 LENIENT 설정됨)
        when(deliveryPolicyRepository.findById(any())).thenReturn(Optional.of(mockPolicy));

        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(InvalidDeliveryDateException.class);
    }

    @Test
    @DisplayName("스케줄러: 다음 배치 ID 조회")
    void findNextBatch_Success() {
        LocalDateTime now = LocalDateTime.now();
        given(orderRepository.findNextBatch(anyInt(), any(), anyLong(), anyInt()))
                .willReturn(List.of(1L, 2L));

        List<Long> result = orderService.findNextBatch(now, 0L, 10);

        assertThat(result).hasSize(2);
    }
}