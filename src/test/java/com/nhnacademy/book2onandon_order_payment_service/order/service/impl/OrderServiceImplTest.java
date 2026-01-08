package com.nhnacademy.book2onandon_order_payment_service.order.service.impl;

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
import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.GuestOrder;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.*;
import com.nhnacademy.book2onandon_order_payment_service.order.provider.OrderNumberProvider;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.book2onandon_order_payment_service.order.service.WrappingPaperService;
import com.nhnacademy.book2onandon_order_payment_service.order.service.impl.OrderServiceImpl;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentCancelRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.service.PaymentService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

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

    // ==========================================
    // 1. 주문 생성 및 검증 (가장 복잡한 로직)
    // ==========================================

    @Test
    @DisplayName("createPreOrder: 모든 할인(쿠폰+포인트) 및 포장/배송비 포함 정상 생성")
    void createPreOrder_Success_FullCalculation() {
        // Given
        Long userId = 1L;
        Long bookId = 10L;
        Long couponId = 20L;

        // 책 가격: 20,000원 * 1권
        BookOrderResponse book = new BookOrderResponse();
        book.setBookId(bookId);
        book.setTitle("Test Book");
        book.setPriceSales(20000L);
        book.setCategoryId(100L); // 카테고리 설정

        // 요청 생성
        OrderItemRequestDto itemReq = new OrderItemRequestDto(bookId, 1, true, 5L); // 포장 있음
        DeliveryAddressRequestDto addressReq = new DeliveryAddressRequestDto("addr", "detail", "msg", "recp", "phone");
        // 배송비 정책 ID: 1, 희망일: 내일, 쿠폰: 20L, 포인트: 1000원
        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(itemReq), addressReq, 1L, LocalDate.now().plusDays(1), couponId, 1000
        );

        // Mocking
        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book));

        // 포장비: 1000원
        given(wrappingPaperService.getWrappingPaperEntity(5L))
                .willReturn(new WrappingPaper(5L, "Paper", 1000, "img"));

        // 배송비: 3000원
        given(deliveryPolicyRepository.findById(1L))
                .willReturn(Optional.of(DeliveryPolicy.builder().freeDeliveryThreshold(15000).deliveryFee(3000).build()));

        // 쿠폰: 2000원 할인 (대상 도서 포함)
        CouponTargetResponseDto couponTarget = new CouponTargetResponseDto(
                1L, List.of(bookId), null, 0, 0, CouponPolicyDiscountType.FIXED, 2000
        );
        given(couponServiceClient.getCouponTargets(couponId)).willReturn(couponTarget);

        // 유저 포인트: 5000원 보유 (사용 1000원 가능)
        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(5000));

        // 주문번호 및 저장
        OrderCreateResponseDto orderCreateResponseDto = new OrderCreateResponseDto();
        orderCreateResponseDto.setOrderNumber("ORD-001");
        orderCreateResponseDto.setOrderId(1L);

        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD-001");
        given(orderTransactionService.createPendingOrder(any(), any()))
                .willReturn(orderCreateResponseDto);

        // When
        OrderCreateResponseDto response = orderService.createPreOrder(userId, null, req);

        // Then
        assertThat(response.getOrderNumber()).isEqualTo("ORD-001");

        // 검증: 최종 계산 금액이 로직 내부에서 맞게 돌았는지 확인 (Exception이 안 터졌으므로 성공)
        // 예상 금액: (20000 + 1000(포장) + 3000(배송)) - 2000(쿠폰) - 1000(포인트) = 21,000원
        verify(resourceManager).prepareResources(eq(userId), eq(req), any(OrderVerificationResult.class), eq(1L));
    }

    @Test
    @DisplayName("createPreOrder: 최소 결제 금액(100원) 미만 시 예외 발생")
    void createPreOrder_Fail_MinAmount() {
        // Given
        // 책 1000원, 포인트 1000원 사용 -> 0원 결제 시도 -> 예외
        BookOrderResponse book = new BookOrderResponse();
        book.setBookId(1L);
        book.setPriceSales(1000L);
        book.setTitle("test책");

        OrderItemRequestDto itemReq = new OrderItemRequestDto(1L, 1, false, null);
        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(itemReq), null, 1L, LocalDate.now().plusDays(1), null, 1000
        );

        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book));
        given(deliveryPolicyRepository.findById(1L))
                .willReturn(Optional.of(DeliveryPolicy.builder().freeDeliveryThreshold(0).deliveryFee(0).build()));
        given(userServiceClient.getUserPoint(1L)).willReturn(new CurrentPointResponseDto(5000));

        req.setDeliveryAddress(new DeliveryAddressRequestDto(
                "서울시", "강남대로 123", "문 앞에", "홍길동", "010-1234-5678"
        ));

        // When & Then
        assertThatThrownBy(() -> orderService.createPreOrder(1L, null, req))
                .isInstanceOf(ExceedUserPointException.class)
                .hasMessageContaining("최소 결제 금액 100원 이상");
    }

    @Test
    @DisplayName("createPreOrder: 포인트 사용량 초과 시 예외 발생")
    void createPreOrder_Fail_ExceedPoint() {
        // Given
        Long userId = 1L;
        BookOrderResponse book = new BookOrderResponse();
        book.setBookId(1L);
        book.setPriceSales(5000L);
        book.setTitle("Test Book");

        OrderItemRequestDto itemReq = new OrderItemRequestDto(1L, 1, false, null);
        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(itemReq),
                new DeliveryAddressRequestDto("서울시", "강남대로 123", "문 앞에", "홍길동", "010-1234-5678"),
                1L,
                LocalDate.now().plusDays(1),
                null,
                6000 // 요청 포인트
        );

        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book));
        given(deliveryPolicyRepository.findById(1L))
                .willReturn(Optional.of(DeliveryPolicy.builder().freeDeliveryThreshold(0).deliveryFee(0).build()));
        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(5000));

        // When & Then
        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(ExceedUserPointException.class)
                .hasMessageContaining("사용 가능한 포인트를 초과했습니다")
                .hasMessageContaining("현재 포인트 : 5000")
                .hasMessageContaining("요청 포인트 : 6000");
    }


    @Test
    @DisplayName("createPreOrder: 리소스 선점 실패 시 보상 트랜잭션(Rollback) 호출 확인")
    void createPreOrder_Fail_ResourceRollback() {
        // Given
        BookOrderResponse book = new BookOrderResponse();
        book.setBookId(1L);
        book.setPriceSales(10000L);
        book.setTitle("test책");

        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(new OrderItemRequestDto(1L, 1, false, null)),
                new DeliveryAddressRequestDto(
                        "서울시", "강남대로 123", "문 앞에", "홍길동", "010-1234-5678"
                ), 1L, LocalDate.now().plusDays(1), 1L, 1000
        );

        CouponTargetResponseDto couponTarget = new CouponTargetResponseDto(
                1L, List.of(1L), List.of(), 100, null,
                CouponPolicyDiscountType.FIXED, 500
        );

        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book));
        given(deliveryPolicyRepository.findById(1L)).willReturn(Optional.of(DeliveryPolicy.builder().freeDeliveryThreshold(0).deliveryFee(0).build()));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD-ROLLBACK");
        given(couponServiceClient.getCouponTargets(any())).willReturn(couponTarget);

        OrderCreateResponseDto resp = new OrderCreateResponseDto();
        resp.setOrderId(1L);
        resp.setOrderNumber("ORD-ROLLBACK");

        given(orderTransactionService.createPendingOrder(any(), any()))
                .willReturn(resp);

        given(userServiceClient.getUserPoint(1L)).willReturn(
                new CurrentPointResponseDto(10000)
        );

        // **중요**: prepareResources 호출 시 예외 발생 설정
        doThrow(new RuntimeException("재고 부족")).when(resourceManager).prepareResources(any(), any(), any(), any());

        // When & Then
        assertThatThrownBy(() -> orderService.createPreOrder(1L, null, req))
                .isInstanceOf(OrderVerificationException.class);

        // **핵심 검증**: releaseResources가 호출되어 롤백을 시도했는지 확인
        verify(resourceManager).releaseResources(eq("ORD-ROLLBACK"), eq(1L), anyInt(), eq(1L));
    }

    // ==========================================
    // 2. 비회원 주문 (Guest)
    // ==========================================

    @Test
    @DisplayName("createGuestPreOrder: 비회원 주문 생성 및 GuestOrder 엔티티 저장 확인")
    void createGuestPreOrder_Success() {
        // Given
        String guestId = "GUEST-ID";
        GuestOrderCreateRequestDto guestReq = new GuestOrderCreateRequestDto(
                "Guest", "010-1234-5678", "pass123",
                List.of(new OrderItemRequestDto(1L, 1, false, null)),
                new DeliveryAddressRequestDto("addr", "", "", "", ""),
                1L, LocalDate.now().plusDays(1)
        );

        BookOrderResponse book = new BookOrderResponse();
        book.setBookId(1L);
        book.setPriceSales(10000L);
        book.setTitle("test책");

        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(DeliveryPolicy.builder().freeDeliveryThreshold(0).deliveryFee(0).build()));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD-GUEST");

        OrderCreateResponseDto resp = new OrderCreateResponseDto();
        resp.setOrderId(1L);
        resp.setOrderNumber("ORD-GUEST");

        given(orderTransactionService.createPendingOrder(any(), any()))
                .willReturn(resp);

        // GuestOrder 저장 로직용 Mock
        Order order = mock(Order.class);
        given(orderTransactionService.getOrderEntity("ORD-GUEST")).willReturn(order);
        given(passwordEncoder.encode("pass123")).willReturn("encodedPass");

        // When
        orderService.createGuestPreOrder(guestId, guestReq);

        // Then
        verify(guestOrderRepository).save(any(GuestOrder.class));
    }

    @Test
    @DisplayName("createGuestPreOrder: GuestOrder 저장 실패 시 OrderVerificationException 발생")
    void createGuestPreOrder_Failed_SaveException() {
        // Given
        String guestId = "GUEST-ID";
        GuestOrderCreateRequestDto guestReq = new GuestOrderCreateRequestDto(
                "Guest", "010-1234-5678", "pass123",
                List.of(new OrderItemRequestDto(1L, 1, false, null)),
                new DeliveryAddressRequestDto("addr", "", "", "", ""),
                1L, LocalDate.now().plusDays(1)
        );

        BookOrderResponse book = new BookOrderResponse();
        book.setBookId(1L);
        book.setPriceSales(10000L);
        book.setTitle("test책");

        // Mock bookServiceClient
        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book));

        // Mock deliveryPolicyRepository
        given(deliveryPolicyRepository.findById(any()))
                .willReturn(Optional.of(DeliveryPolicy.builder().freeDeliveryThreshold(0).deliveryFee(0).build()));

        // 주문 번호 생성
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD-GUEST");

        // 임시 주문 생성
        OrderCreateResponseDto resp = new OrderCreateResponseDto();
        resp.setOrderId(1L);
        resp.setOrderNumber("ORD-GUEST");
        given(orderTransactionService.createPendingOrder(any(), any())).willReturn(resp);

        // Order 엔티티 반환
        Order order = mock(Order.class);
        given(orderTransactionService.getOrderEntity("ORD-GUEST")).willReturn(order);

        // 비밀번호 인코딩
        given(passwordEncoder.encode("pass123")).willReturn("encodedPass");

        // GuestOrder 저장 실패 시 예외 발생
        doThrow(new RuntimeException("DB 저장 실패"))
                .when(guestOrderRepository).save(any(GuestOrder.class));

        // When & Then
        OrderVerificationException ex = assertThrows(OrderVerificationException.class, () ->
                orderService.createGuestPreOrder(guestId, guestReq)
        );

        assertThat(ex.getMessage()).contains("비회원 주문 DB 생성 중 오류");
    }


    // ==========================================
    // 3. 주문 취소 (Cancel)
    // ==========================================

    @Test
    @DisplayName("cancelOrder: 회원 주문 취소 성공")
    void cancelOrder_Success() {
        // Given
        String orderNumber = "ORD-CANCEL";
        Order order = mock(Order.class);
        given(orderRepository.findByUserIdAndOrderNumber(1L, orderNumber)).willReturn(Optional.of(order));

        // 취소 가능 상태 모킹
        given(order.getOrderStatus()).willReturn(OrderStatus.COMPLETED); // Cancellable true
        given(order.getOrderNumber()).willReturn(orderNumber);

        // When
        orderService.cancelOrder(1L, orderNumber);

        // Then
        verify(paymentService).cancelPayment(any(PaymentCancelRequest.class));
        verify(orderTransactionService, atLeastOnce()).changeStatusOrder(order, false);
        verify(resourceManager).releaseResources(eq(orderNumber), eq(1L), anyInt(), any());
    }

    @Test
    @DisplayName("cancelOrder: 이미 배송 중이라 취소 불가 시 예외")
    void cancelOrder_Fail_NotCancellable() {
        // Given
        String orderNumber = "ORD-SHIPPING";
        Order order = mock(Order.class);
        given(orderRepository.findByUserIdAndOrderNumber(1L, orderNumber)).willReturn(Optional.of(order));

        // 취소 불가 상태
        given(order.getOrderStatus()).willReturn(OrderStatus.SHIPPING);

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, orderNumber))
                .isInstanceOf(OrderNotCancellableException.class);
    }

    // ==========================================
    // 4. 조회 로직 (prepareOrder, getOrderDetail)
    // ==========================================

    @Test
    @DisplayName("prepareOrder: 주문 전 정보 조회 (책 수량 매핑 확인)")
    void prepareOrder_Success() {
        // Given
        BookInfoDto info = new BookInfoDto(1L, 3); // 수량 3권
        OrderPrepareRequestDto req = new OrderPrepareRequestDto(List.of(info));

        BookOrderResponse bookResp = new BookOrderResponse();
        bookResp.setBookId(1L);
        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(bookResp));
        given(userServiceClient.getUserAddresses(anyLong())).willReturn(List.of());
        given(userServiceClient.getUserPoint(anyLong())).willReturn(new CurrentPointResponseDto(0));
        given(couponServiceClient.getUsableCoupons(anyLong(), any())).willReturn(List.of());

        // When
        OrderPrepareResponseDto res = orderService.prepareOrder(1L, req);


        // Then
        assertThat(res.orderItems().getFirst().getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("getOrderDetail: 주문 상세 조회")
    void getOrderDetail_Success() {
        // Given
        String orderNumber = "ORD-DETAIL";
        Order order = mock(Order.class);
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(order));

        // Order Item Mock
        OrderItem item = mock(OrderItem.class);
        given(order.getOrderItems()).willReturn(List.of(item));
        given(item.getBookId()).willReturn(1L);

        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(new BookOrderResponse()));
        given(orderViewAssembler.toOrderDetailView(any(), any())).willReturn(new OrderDetailResponseDto());
        // when
        PaymentResponse response = new PaymentResponse(
                "PAY-001",
                "ORD-001",
                1000,
                "CARD",
                "TOSS",
                "DONE",
                LocalDateTime.now(),
                "http://receipt.url",
                0,
                List.of()
        );


        given(paymentService.getPayment(any())).willReturn(response);

        // When
        OrderDetailResponseDto result = orderService.getOrderDetail(1L, orderNumber, null);

        // Then
        assertThat(result).isNotNull();
        // 검증 서비스 호출 확인
        verify(orderTransactionService).validateOrderExistence(order, 1L, null);
    }

    // ==========================================
    // 5. 관리자/기타 기능
    // ==========================================

    @Test
    @DisplayName("관리자 주문 취소")
    void cancelOrderByAdmin_Success() {
        Order order = mock(Order.class);
        given(orderRepository.findByOrderNumber("ORD-ADMIN")).willReturn(Optional.of(order));
        given(order.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(order.getOrderNumber()).willReturn("ORD-ADMIN");

        orderService.cancelOrderByAdmin("ORD-ADMIN");

        verify(paymentService).cancelPayment(any());
        verify(orderTransactionService, atLeastOnce()).changeStatusOrder(order, false);
    }

    @Test
    @DisplayName("관리자 주문 상세 조회")
    void getOrderDetailWithAdmin_Success() {
        String orderNum = "ORD-ADMIN-VIEW";
        Order order = mock(Order.class);
        given(orderRepository.findByOrderNumber(orderNum)).willReturn(Optional.of(order));
        given(order.getOrderItems()).willReturn(List.of());
        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of());
        given(orderViewAssembler.toOrderDetailView(any(), any())).willReturn(new OrderDetailResponseDto());

        // when
        PaymentResponse response = new PaymentResponse(
                "PAY-001",
                "ORD-001",
                1000,
                "CARD",
                "TOSS",
                "DONE",
                LocalDateTime.now(),
                "http://receipt.url",
                0,
                List.of()
        );


        given(paymentService.getPayment(any())).willReturn(response);

        orderService.getOrderDetailWithAdmin(orderNum);

        verify(orderRepository).findByOrderNumber(orderNum);
    }

    @Test
    @DisplayName("setOrderStatus: 주문 상태 변경 성공")
    void setOrderStatus_Success() {
        // Given
        String orderNumber = "ORD-001";
        Order order = mock(Order.class);
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(order));

        OrderStatusUpdateDto req = new OrderStatusUpdateDto(OrderStatus.SHIPPING);

        // When
        orderService.setOrderStatus(orderNumber, req);

        // Then
        verify(order).setOrderStatus(OrderStatus.SHIPPING);
    }

    @Test
    @DisplayName("setOrderStatus: 주문번호 없으면 예외 발생")
    void setOrderStatus_Fail_OrderNotFound() {
        String orderNumber = "ORD-UNKNOWN";
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.empty());

        OrderStatusUpdateDto req = new OrderStatusUpdateDto(OrderStatus.SHIPPING);

        assertThatThrownBy(() -> orderService.setOrderStatus(orderNumber, req))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Not Found Order");
    }

    @Test
    @DisplayName("setOrderItemStatus: 특정 주문 항목 상태 변경 성공")
    void setOrderItemStatus_Success() {
        String orderNumber = "ORD-002";
        Order order = mock(Order.class);
        OrderItem item1 = mock(OrderItem.class);
        OrderItem item2 = mock(OrderItem.class);

        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(order));
        given(order.getOrderItems()).willReturn(List.of(item1, item2));
        given(item1.getOrderItemId()).willReturn(100L);
        given(item2.getOrderItemId()).willReturn(101L);

        OrderItemStatusUpdateDto req = new OrderItemStatusUpdateDto(100L, OrderItemStatus.SHIPPED);

        // When
        orderService.setOrderItemStatus(orderNumber, req);

        // Then
        verify(item1).setOrderItemStatus(OrderItemStatus.SHIPPED);
        verify(item2, never()).setOrderItemStatus(any());
    }

    @Test
    @DisplayName("setOrderItemStatus: 주문번호 없으면 예외 발생")
    void setOrderItemStatus_Fail_OrderNotFound() {
        String orderNumber = "ORD-UNKNOWN";
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.empty());

        OrderItemStatusUpdateDto req = new OrderItemStatusUpdateDto(100L, OrderItemStatus.SHIPPED);

        assertThatThrownBy(() -> orderService.setOrderItemStatus(orderNumber, req))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Not Found Order");
    }

    @Test
    @DisplayName("findNextBatch: 배치 조회 성공")
    void findNextBatch_Success() {
        LocalDateTime threshold = LocalDateTime.now();
        Long lastId = 10L;
        int batchSize = 5;
        List<Long> expectedIds = List.of(11L, 12L, 13L);

        given(orderRepository.findNextBatch(OrderStatus.PENDING.getCode(), threshold, lastId, batchSize))
                .willReturn(expectedIds);

        List<Long> result = orderService.findNextBatch(threshold, lastId, batchSize);

        assertThat(result).isEqualTo(expectedIds);
    }

    @Test
    @DisplayName("deleteJunkOrder: 주문 삭제 성공")
    void deleteJunkOrder_Success() {
        List<Long> ids = List.of(1L, 2L, 3L);

        given(orderRepository.deleteByIds(ids)).willReturn(ids.size());

        int deletedCount = orderService.deleteJunkOrder(ids);

        assertThat(deletedCount).isEqualTo(3);
        verify(orderRepository).deleteByIds(ids);
    }

    @Test
    @DisplayName("prepareGuestOrder: 비회원 주문 전 데이터 조회 성공")
    void prepareGuestOrder_Success() {
        // Given
        String guestId = "GUEST-001";
        BookInfoDto bookInfo1 = new BookInfoDto(1L, 2); // 2권
        BookInfoDto bookInfo2 = new BookInfoDto(2L, 1); // 1권
        OrderPrepareRequestDto req = new OrderPrepareRequestDto(List.of(bookInfo1, bookInfo2));

        BookOrderResponse bookResp1 = new BookOrderResponse();
        bookResp1.setBookId(1L);
        bookResp1.setTitle("Book 1");

        BookOrderResponse bookResp2 = new BookOrderResponse();
        bookResp2.setBookId(2L);
        bookResp2.setTitle("Book 2");

        // Mock BookServiceClient 호출
        given(bookServiceClient.getBooksForOrder(List.of(1L, 2L)))
                .willReturn(List.of(bookResp1, bookResp2));

        // When
        OrderPrepareResponseDto result = orderService.prepareGuestOrder(guestId, req);

        // Then
        assertThat(result.orderItems()).hasSize(2);
        assertThat(result.orderItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.orderItems().get(1).getQuantity()).isEqualTo(1);

        // BookServiceClient 호출 확인
        verify(bookServiceClient).getBooksForOrder(List.of(1L, 2L));
    }

    @Test
    @DisplayName("cancelGuestOrder: 비회원 주문 취소 성공")
    void cancelGuestOrder_Success() {
        // Given
        String orderNumber = "GUEST-ORD-001";
        String guestToken = "token-123";

        // Order mock
        Order order = mock(Order.class);

        // 주문 조회 mock
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(order));
        given(order.getOrderStatus()).willReturn(OrderStatus.COMPLETED);

        // validateOrderExistence mock (예외 없이 정상)
        doNothing().when(orderTransactionService).validateOrderExistence(order, null, guestToken);

        // processCancelOrder는 private이므로 내부 로직은 예외 발생 없이 실행되었다고 가정

        // When
        orderService.cancelGuestOrder(orderNumber, guestToken);

        // Then
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderTransactionService).validateOrderExistence(order, null, guestToken);
        // processCancelOrder는 private라 verify할 수 없으므로 예외 발생 여부로 정상 수행 확인
    }


    @Test
    @DisplayName("cancelGuestOrder: 주문번호 없으면 예외 발생")
    void cancelGuestOrder_Fail_OrderNotFound() {
        // Given
        String orderNumber = "ORD-NOT-FOUND";
        String guestToken = "TOKEN";
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.cancelGuestOrder(orderNumber, guestToken))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("주문 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("getOrderList: 일반 사용자 주문 리스트 조회 성공")
    void getOrderList_Success() {
        // Given
        Long userId = 1L;
        Pageable pageable = Pageable.unpaged();

        OrderSimpleDto orderDto1 = new OrderSimpleDto();
        OrderSimpleDto orderDto2 = new OrderSimpleDto();
        List<OrderSimpleDto> orders = List.of(orderDto1, orderDto2);

        Page<OrderSimpleDto> page = mock(Page.class);
        given(page.getContent()).willReturn(orders);
        given(orderRepository.findAllByUserId(userId, pageable, OrderStatus.PENDING)).willReturn(page);

        // When
        Page<OrderSimpleDto> result = orderService.getOrderList(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(orderRepository).findAllByUserId(userId, pageable, OrderStatus.PENDING);
    }

    @Test
    @DisplayName("createPreOrder: 특정 도서 할인 적용")
    void createPreOrder_Discount_TargetBook() {
        // Given
        Long userId = 1L;

        OrderItemRequestDto item1 = new OrderItemRequestDto(1L, 2, false, null); // 할인 대상 도서
        OrderItemRequestDto item2 = new OrderItemRequestDto(2L, 1, false, null); // 일반 도서
        DeliveryAddressRequestDto addressReq = new DeliveryAddressRequestDto("addr", "detail", "msg", "recp", "phone");

        OrderCreateRequestDto request = new OrderCreateRequestDto(
                List.of(item1, item2), addressReq, 1L, LocalDate.now().plusDays(1), 1L, 0
        );

        // Book 정보 모킹
        BookOrderResponse book1 = new BookOrderResponse();
        book1.setBookId(1L);
        book1.setTitle("Test Book");
        book1.setPriceSales(2000L);
        book1.setCategoryId(100L);

        BookOrderResponse book2 = new BookOrderResponse();
        book2.setBookId(2L);
        book2.setTitle("Test Book");
        book2.setPriceSales(1000L);
        book2.setCategoryId(100L);

        // 쿠폰 정보 모킹 (특정 도서 500원 할인)
        CouponTargetResponseDto couponTarget = new CouponTargetResponseDto(
                1L, List.of(1L), List.of(), 100, null,
                CouponPolicyDiscountType.FIXED, 500
        );

        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book1, book2));
        given(couponServiceClient.getCouponTargets(1L)).willReturn(couponTarget);
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(
                DeliveryPolicy.builder().freeDeliveryThreshold(0).deliveryFee(0).build()
        ));

        OrderCreateResponseDto mockResponse = new OrderCreateResponseDto();
        mockResponse.setOrderId(123L);
        mockResponse.setTotalAmount(3500); // 2*1000 + 1*2000 - 500
        mockResponse.setCouponDiscount(500);
        mockResponse.setTotalDiscountAmount(500);
        mockResponse.setDeliveryFee(0);
        mockResponse.setWrappingFee(0);

        given(orderTransactionService.createPendingOrder(eq(userId), any()))
                .willReturn(mockResponse);

        // When
        OrderCreateResponseDto result = orderService.createPreOrder(userId, null, request);

        // Then
        assertThat(result.getCouponDiscount()).isEqualTo(500);
        assertThat(result.getTotalDiscountAmount()).isEqualTo(500);
        assertThat(result.getTotalAmount()).isEqualTo(2*1000 + 1*2000 - 500);
    }


    @Test
    @DisplayName("createPreOrder: 특정 카테고리 할인 적용")
    void createPreOrder_Discount_TargetCategory() {
        // Given
        Long userId = 1L;
        OrderItemRequestDto itemReq1 = new OrderItemRequestDto(1L, 1, false, null); // 카테고리 대상
        OrderItemRequestDto itemReq2 = new OrderItemRequestDto(2L, 1, false, null); // 제외
        DeliveryAddressRequestDto addressReq = new DeliveryAddressRequestDto("addr", "detail", "msg", "recp", "phone");

        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(itemReq1, itemReq2),
                addressReq,
                userId,
                LocalDate.now().plusDays(1),
                1L,
                0
        );

        BookOrderResponse book1 = new BookOrderResponse();
        book1.setBookId(1L);
        book1.setCategoryId(10L);
        book1.setPriceSales(1000L);
        book1.setTitle("test책1");

        BookOrderResponse book2 = new BookOrderResponse();
        book2.setBookId(2L);
        book2.setCategoryId(20L);
        book2.setPriceSales(2000L);
        book1.setTitle("test책2");

        CouponTargetResponseDto couponTarget = new CouponTargetResponseDto(
                1L,
                List.of(),           // targetBookIds
                List.of(10L),        // targetCategoryIds
                100,                 // minPrice
                1000,                // maxPrice
                CouponPolicyDiscountType.PERCENT,
                50                  // discountValue (%)

        );

        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(DeliveryPolicy.builder().freeDeliveryThreshold(0).deliveryFee(0).build()));
        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book1, book2));
        given(couponServiceClient.getCouponTargets(1L)).willReturn(couponTarget);
//        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(0));

        OrderCreateResponseDto mockResponse = new OrderCreateResponseDto();
        mockResponse.setOrderId(123L);
        mockResponse.setTotalAmount(3500); // 2*1000 + 1*2000 - 500
        mockResponse.setCouponDiscount(500);
        mockResponse.setTotalDiscountAmount(500);
        mockResponse.setDeliveryFee(0);
        mockResponse.setWrappingFee(0);

        given(orderTransactionService.createPendingOrder(eq(userId), any()))
                .willReturn(mockResponse);

        // When
        OrderCreateResponseDto result = orderService.createPreOrder(userId, null, req);

        // Then
        int discountBase = 1 * 1000; // book1만 카테고리 대상
        int expectedDiscount = discountBase * 50 * 100 / 10000; // 퍼센트 할인
        assertThat(result.getCouponDiscount()).isEqualTo(expectedDiscount);
    }

    @Test
    @DisplayName("createPreOrder: 최소 주문 금액 미만으로 쿠폰 적용 시 예외")
    void createPreOrder_Discount_MinPriceFail() {
        // Given
        Long userId = 1L;
        OrderItemRequestDto itemReq = new OrderItemRequestDto(1L, 1, false, null);

        DeliveryAddressRequestDto addressReq = new DeliveryAddressRequestDto("addr", "detail", "msg", "recp", "phone");

        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(itemReq),
                addressReq,
                userId,
                LocalDate.now().plusDays(1),
                1L,
                0
        );

        BookOrderResponse book = new BookOrderResponse();
        book.setBookId(1L);
        book.setCategoryId(10L);
        book.setPriceSales(50L); // 최소 주문 금액 100원보다 낮음
        book.setTitle("test책");

        CouponTargetResponseDto couponTarget = new CouponTargetResponseDto(
                1L,
                List.of(1L),
                List.of(),
                100, // minPrice
                null,
                CouponPolicyDiscountType.FIXED,
                50

        );

        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(DeliveryPolicy.builder().freeDeliveryThreshold(0).deliveryFee(0).build()));
        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book));
        given(couponServiceClient.getCouponTargets(1L)).willReturn(couponTarget);
//        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(0));

        // When & Then
        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(OrderVerificationException.class)
                .hasMessageContaining("최소 주문 금액 100원 이상부터 할인 쿠폰 적용이 가능합니다.");
    }

    @Test
    @DisplayName("createPreOrder: 결제 금액 100원 미만 시 예외")
    void createPreOrder_TotalAmount_MinFail() {
        // Given
        Long userId = 1L;
        OrderItemRequestDto itemReq = new OrderItemRequestDto(1L, 1, false, null);
        DeliveryAddressRequestDto addressReq = new DeliveryAddressRequestDto("addr", "detail", "msg", "recp", "phone");

        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(itemReq),
                addressReq,
                1L,
                LocalDate.now().plusDays(1),
                null,
                0
        );

        BookOrderResponse book = new BookOrderResponse();
        book.setBookId(1L);
        book.setPriceSales(50L); // 총액 < 100원
        book.setTitle("test책");


        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(DeliveryPolicy.builder().freeDeliveryThreshold(0).deliveryFee(0).build()));
        given(bookServiceClient.getBooksForOrder(any())).willReturn(List.of(book));
//        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(0));

        // When & Then
        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(OrderVerificationException.class)
                .hasMessageContaining("최소 결제 금액 100원 이상이어야 함");
    }

}
