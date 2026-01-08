package com.nhnacademy.book2onandon_order_payment_service.order.order.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.nhnacademy.book2onandon_order_payment_service.client.BookServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.CouponServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.UserServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.*;
import com.nhnacademy.book2onandon_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.book2onandon_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.*;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest.GuestOrderCreateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemStatusUpdateDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.*;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.*;
import com.nhnacademy.book2onandon_order_payment_service.order.provider.OrderNumberProvider;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.book2onandon_order_payment_service.order.service.WrappingPaperService;
import com.nhnacademy.book2onandon_order_payment_service.order.service.impl.OrderServiceImpl;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.service.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private DeliveryPolicyRepository deliveryPolicyRepository;
    @Mock
    private GuestOrderRepository guestOrderRepository;
    @Mock
    private WrappingPaperService wrappingPaperService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private OrderTransactionService orderTransactionService;
    @Mock
    private BookServiceClient bookServiceClient;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private CouponServiceClient couponServiceClient;
    @Mock
    private OrderNumberProvider orderNumberProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OrderViewAssembler orderViewAssembler;
    @Mock
    private OrderResourceManager resourceManager;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("prepareOrder - 회원 주문 준비 데이터 조회 성공")
    void prepareOrder_Success() {
        Long userId = 1L;
        OrderPrepareRequestDto req = new OrderPrepareRequestDto(List.of(new BookInfoDto(100L, 2)));

        BookOrderResponse bookResp = new BookOrderResponse();
        bookResp.getBookId();
        bookResp.getCategoryId();
        bookResp.getTitle();
        bookResp.getPriceSales();

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(bookResp));
        given(userServiceClient.getUserAddresses(userId)).willReturn(new ArrayList<>());
        given(couponServiceClient.getUsableCoupons(eq(userId), any())).willReturn(new ArrayList<>());
        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(1000));

        OrderPrepareResponseDto result = orderService.prepareOrder(userId, req);

        assertThat(result).isNotNull();
        verify(bookServiceClient).getBooksForOrder(anyList());
    }

    @Test
    @DisplayName("createPreOrder - 임시 주문 생성 성공")
    void createPreOrder_Success() {
        OrderCreateRequestDto req = createOrderCreateRequest();

        OrderCreateResponseDto createResp = mock(OrderCreateResponseDto.class);
        given(createResp.getOrderId()).willReturn(1L);

        BookOrderResponse bookResp = mock(BookOrderResponse.class);
        given(bookResp.getBookId()).willReturn(100L);
        given(bookResp.getPriceSales()).willReturn(10000L); // BigDecimal인 경우
        given(bookResp.getTitle()).willReturn("테스트 책");

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(bookResp));
        given(deliveryPolicyRepository.findById(anyLong())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD-123");
        given(userServiceClient.getUserPoint(anyLong())).willReturn(new CurrentPointResponseDto(5000));
        given(orderTransactionService.createPendingOrder(anyLong(), any())).willReturn(createResp);

        OrderCreateResponseDto result = orderService.createPreOrder(1L, null, req);

        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(resourceManager).prepareResources(anyLong(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("createPreOrder - 리소스 선점 실패 시 보상 트랜잭션 실행 확인")
    void createPreOrder_ResourceFail_Rollback() {
        OrderCreateRequestDto req = createOrderCreateRequest();
        OrderCreateResponseDto createResp = mock(OrderCreateResponseDto.class);

        BookOrderResponse bookResp = mock(BookOrderResponse.class);
        given(bookResp.getBookId()).willReturn(100L);
        given(bookResp.getPriceSales()).willReturn(10000L);
        given(bookResp.getTitle()).willReturn("정상적인 제목");

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(bookResp));
        given(deliveryPolicyRepository.findById(anyLong())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(userServiceClient.getUserPoint(anyLong())).willReturn(new CurrentPointResponseDto(5000));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD-123");

        lenient().when(orderTransactionService.createPendingOrder(any(), any())).thenReturn(createResp);

        doThrow(new RuntimeException("Resource Fail")).when(resourceManager)
                .prepareResources(any(), any(), any(), any());

        assertThatThrownBy(() -> orderService.createPreOrder(1L, null, req))
                .isInstanceOf(OrderVerificationException.class);

        verify(resourceManager).releaseResources(any(), any(), anyInt(), anyLong());
    }


    @Test
    @DisplayName("getOrderDetail - 주문 상세 조회 성공")
    void getOrderDetail_Success() {
        String orderNo = "ORD-123";
        Order order = mock(Order.class);
        given(order.getOrderItems()).willReturn(new ArrayList<>());
        given(orderRepository.findByOrderNumber(orderNo)).willReturn(Optional.of(order));
        given(orderViewAssembler.toOrderDetailView(any(), any())).willReturn(new OrderDetailResponseDto());
        given(paymentService.getPayment(any())).willReturn(mock(PaymentResponse.class));

        OrderDetailResponseDto result = orderService.getOrderDetail(1L, orderNo, null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("cancelOrder - 주문 취소 성공")
    void cancelOrder_Success() {
        Order order = mock(Order.class);
        given(order.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(order.getOrderNumber()).willReturn("ORD-123");
        given(orderRepository.findByUserIdAndOrderNumber(anyLong(), anyString())).willReturn(Optional.of(order));

        orderService.cancelOrder(1L, "ORD-123");

        verify(paymentService).cancelPayment(any());
        verify(orderTransactionService, times(2)).changeStatusOrder(any(), eq(false));
    }

    @Test
    @DisplayName("processCancelOrder - 취소 불가능한 상태일 때 예외 발생")
    void cancelOrder_Fail_Status() {
        Order order = mock(Order.class);
        given(order.getOrderStatus()).willReturn(OrderStatus.SHIPPING); // 취소 불가
        given(orderRepository.findByUserIdAndOrderNumber(anyLong(), anyString())).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "ORD-123"))
                .isInstanceOf(OrderNotCancellableException.class);
    }

    @Test
    @DisplayName("createWantDeliveryDate - 배송일 범위 오류 예외 발생")
    void createWantDeliveryDate_Invalid() {
        OrderCreateRequestDto req = createOrderCreateRequest();
        req.setWantDeliveryDate(LocalDate.now());

        BookOrderResponse bookResp = mock(BookOrderResponse.class);
        given(bookResp.getBookId()).willReturn(100L);
        given(bookResp.getPriceSales()).willReturn(10000L);
        given(bookResp.getTitle()).willReturn("테스트 도서");
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(bookResp));

        DeliveryPolicy mockPolicy = mock(DeliveryPolicy.class);
        given(deliveryPolicyRepository.findById(anyLong())).willReturn(Optional.of(mockPolicy));
        given(mockPolicy.calculateDeliveryFee(anyInt())).willReturn(3000);

        given(userServiceClient.getUserPoint(anyLong())).willReturn(new CurrentPointResponseDto(10000));

        assertThatThrownBy(() -> orderService.createPreOrder(1L, null, req))
                .isInstanceOf(InvalidDeliveryDateException.class);
    }

    @Test
    @DisplayName("deleteJunkOrder - 스케줄러 정크 데이터 삭제")
    void deleteJunkOrder_Success() {
        given(orderRepository.deleteByIds(anyList())).willReturn(5);
        int result = orderService.deleteJunkOrder(List.of(1L, 2L));
        assertThat(result).isEqualTo(5);
    }

    @Test
    @DisplayName("setOrderItemStatus - 특정 상품 상태 변경")
    void setOrderItemStatus_Success() {
        String orderNo = "ORD-123";
        Order order = mock(Order.class);
        OrderItem item = mock(OrderItem.class);
        given(item.getOrderItemId()).willReturn(50L);
        given(order.getOrderItems()).willReturn(List.of(item));
        given(orderRepository.findByOrderNumber(orderNo)).willReturn(Optional.of(order));

        orderService.setOrderItemStatus(orderNo, new OrderItemStatusUpdateDto(50L, OrderItemStatus.ORDER_COMPLETE));

        verify(item).setOrderItemStatus(OrderItemStatus.ORDER_COMPLETE);
    }

    private OrderCreateRequestDto createOrderCreateRequest() {
        OrderCreateRequestDto dto = new OrderCreateRequestDto();
        dto.setOrderItems(List.of(new OrderItemRequestDto(100L, 1, false, null)));
        dto.setDeliveryAddress(new DeliveryAddressRequestDto("서울", "상세", "메시지", "수령인", "01012341234"));
        dto.setDeliveryPolicyId(1L);
        dto.setWantDeliveryDate(LocalDate.now().plusDays(2));
        dto.setPoint(100);
        return dto;
    }

    @Test
    @DisplayName("prepareGuestOrder - 비회원 주문 준비 조회 성공")
    void prepareGuestOrder_Success() {
        OrderPrepareRequestDto req = new OrderPrepareRequestDto(List.of(new BookInfoDto(100L, 1)));
        BookOrderResponse book = createMockBook(100L, 10000L, "책", 1L);
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(book));

        OrderPrepareResponseDto result = orderService.prepareGuestOrder("guest-id", req);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrderList - 일반 사용자 주문 목록 페이징 조회")
    void getOrderList_Success() {
        Page<OrderSimpleDto> page = new PageImpl<>(List.of());
        given(orderRepository.findAllByUserId(anyLong(), any(Pageable.class), any(OrderStatus.class))).willReturn(page);

        Page<OrderSimpleDto> result = orderService.getOrderList(1L, PageRequest.of(0, 10));
        assertThat(result).isNotNull();
    }

    // -------------------------------------------------------------------------
    // 2. 쿠폰 및 포인트 할인 로직 보강 (Uncovered Line 집중 공략)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createCouponDiscount - 고정 금액 할인(FIXED) 및 최소 금액 미달 예외")
    void createCouponDiscount_Fixed_And_MinPriceError() {
        OrderCreateRequestDto req = createOrderCreateRequest();
        req.setMemberCouponId(1L);
        req.setPoint(0);

        BookOrderResponse book = createMockBook(100L, 5000L, "책", 1L);
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(book));
        given(deliveryPolicyRepository.findById(anyLong())).willReturn(Optional.of(mock(DeliveryPolicy.class)));

        // [수정] 예외가 터지면 아래 호출은 일어나지 않을 수 있으므로 lenient()를 추가합니다.
        lenient().when(userServiceClient.getUserPoint(anyLong()))
                .thenReturn(new CurrentPointResponseDto(1000));

        CouponTargetResponseDto coupon = new CouponTargetResponseDto(1L, List.of(100L), null, 10000, 0, CouponPolicyDiscountType.FIXED, 2000);
        given(couponServiceClient.getCouponTargets(1L)).willReturn(coupon);

        // When & Then
        assertThatThrownBy(() -> orderService.createPreOrder(1L, null, req))
                .isInstanceOf(OrderVerificationException.class)
                .hasMessageContaining("최소 주문 금액");
    }

    @Test
    @DisplayName("createCouponDiscount - 퍼센트 할인 및 포인트 부족 예외 커버")
    void createCouponDiscount_Percent_MaxPriceLimit() {
        OrderCreateRequestDto req = createOrderCreateRequest();
        req.setMemberCouponId(2L);
        req.setPoint(0); // [해결] 포인트 0으로 설정하여 로직 단순화

        BookOrderResponse book = createMockBook(100L, 50000L, "책", 1L);
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(book));
        given(deliveryPolicyRepository.findById(anyLong())).willReturn(Optional.of(mock(DeliveryPolicy.class)));

        // [해결] NPE 방지를 위해 빈 객체가 아닌 포인트 정보 리턴
//        given(userServiceClient.getUserPoint(anyLong())).willReturn(new CurrentPointResponseDto(1000));
        given(orderTransactionService.createPendingOrder(any(), any())).willReturn(mock(OrderCreateResponseDto.class));

        CouponTargetResponseDto coupon = new CouponTargetResponseDto(2L, null, List.of(1L), 0, 3000, CouponPolicyDiscountType.PERCENT, 10);
        given(couponServiceClient.getCouponTargets(2L)).willReturn(coupon);

        orderService.createPreOrder(1L, null, req);
        verify(couponServiceClient).getCouponTargets(2L);
    }

    @Test
    @DisplayName("createPointDiscount - 포인트 사용 후 결제액 100원 미만 예외 커버")
    void createPointDiscount_MinAmountError() {
        OrderCreateRequestDto req = createOrderCreateRequest();
        req.setPoint(4950); // 주문액 5000원 - 포인트 4950원 = 50원 (100원 미만)

        BookOrderResponse book = createMockBook(100L, 5000L, "책", 1L);
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(book));
        given(deliveryPolicyRepository.findById(anyLong())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(userServiceClient.getUserPoint(anyLong())).willReturn(new CurrentPointResponseDto(10000));

        assertThatThrownBy(() -> orderService.createPreOrder(1L, null, req))
                .isInstanceOf(ExceedUserPointException.class)
                .hasMessageContaining("최소 결제 금액 100원 이상");
    }

    // -------------------------------------------------------------------------
    // 3. 비회원 및 관리자 로직 커버
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cancelGuestOrder - 비회원 주문 취소 커버")
    void cancelGuestOrder_Success() {
        Order order = mock(Order.class);
        given(order.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));

        orderService.cancelGuestOrder("ORD-123", "token");
        verify(paymentService).cancelPayment(any());
    }

    @Test
    @DisplayName("getOrderListWithAdmin - 관리자 전체 목록 조회")
    void getOrderListWithAdmin_Success() {
        given(orderRepository.findAllByAdmin(any(), any())).willReturn(new PageImpl<>(List.of()));
        orderService.getOrderListWithAdmin(PageRequest.of(0, 10));
        verify(orderRepository).findAllByAdmin(any(), any());
    }

    @Test
    @DisplayName("getOrderDetailWithAdmin - 관리자 상세 조회 커버")
    void getOrderDetailWithAdmin_Success() {
        Order order = mock(Order.class);
        given(order.getOrderItems()).willReturn(new ArrayList<>());
        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
        given(orderViewAssembler.toOrderDetailView(any(), any())).willReturn(new OrderDetailResponseDto());
        given(paymentService.getPayment(any())).willReturn(mock(PaymentResponse.class));

        orderService.getOrderDetailWithAdmin("ORD-123");
        verify(orderRepository).findByOrderNumber("ORD-123");
    }

    @Test
    @DisplayName("cancelOrderByAdmin - 관리자 주문 취소 커버")
    void cancelOrderByAdmin_Success() {
        Order order = mock(Order.class);
        given(order.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));

        orderService.cancelOrderByAdmin("ORD-123");
        verify(resourceManager).releaseResources(any(), any(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("setOrderStatus - 주문 상태 변경 커버")
    void setOrderStatus_Success() {
        Order order = mock(Order.class);
        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));

        orderService.setOrderStatus("ORD-123", new OrderStatusUpdateDto(OrderStatus.SHIPPING));
        verify(order).setOrderStatus(OrderStatus.SHIPPING);
    }

    // -------------------------------------------------------------------------
    // 기존 테스트 유지 및 헬퍼 메서드
    // -------------------------------------------------------------------------

    private BookOrderResponse createMockBook(Long id, Long price, String title, Long categoryId) {
        BookOrderResponse book = mock(BookOrderResponse.class);
        lenient().when(book.getBookId()).thenReturn(id);
        lenient().when(book.getPriceSales()).thenReturn(price);
        lenient().when(book.getTitle()).thenReturn(title);
        lenient().when(book.getCategoryId()).thenReturn(categoryId);
        return book;
    }
}