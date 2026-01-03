package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.CouponPolicyDiscountType;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.CouponTargetResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.CurrentPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UserAddressResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderDetailResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestOrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.GuestOrder;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.DeliveryPolicyNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.ExceedUserPointException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.InvalidDeliveryDateException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.OrderNumberProvider;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.WrappingPaperService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.impl.OrderServiceImpl;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
        BookInfoDto bookInfo = new BookInfoDto(1L, 2);
        OrderPrepareRequestDto req = new OrderPrepareRequestDto(List.of(bookInfo));

        BookOrderResponse mockBook = createBookResponse(1L, "Book", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(userServiceClient.getUserAddresses(userId)).willReturn(List.of(mock(UserAddressResponseDto.class)));
        given(couponServiceClient.getUsableCoupons(eq(userId), any())).willReturn(List.of());
        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(1000));

        OrderPrepareResponseDto result = orderService.prepareOrder(userId, null, req);

        assertThat(result).isNotNull();
        assertThat(result.currentPoint().getCurrentPoint()).isEqualTo(1000);
        assertThat(mockBook.getQuantity()).isEqualTo(2); // 수량 세팅 확인
    }

    @Test
    @DisplayName("비회원 주문 준비 데이터 조회 성공")
    void prepareOrder_Guest_Success() {
        BookInfoDto bookInfo = new BookInfoDto(1L, 1);
        OrderPrepareRequestDto req = new OrderPrepareRequestDto(List.of(bookInfo));

        BookOrderResponse mockBook = createBookResponse(1L, "Book", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));

        OrderPrepareResponseDto result = orderService.prepareOrder(null, "guest-id", req);

        assertThat(result).isNotNull();
        assertThat(result.addresses()).isNull();
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createPreOrder_Success_Basic() {
        Long userId = 1L;
        OrderCreateRequestDto req = createValidOrderRequest();

        BookOrderResponse mockBook = createBookResponse(1L, "Test Book", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD-001");

        // 올바른 생성 방식: 기본 생성자 + Setter 사용
        OrderCreateResponseDto responseDto = new OrderCreateResponseDto();
        responseDto.setOrderId(1L);
        responseDto.setOrderNumber("ORD-001");

        given(orderTransactionService.createPendingOrder(eq(userId), any())).willReturn(responseDto);

        OrderCreateResponseDto result = orderService.createPreOrder(userId, null, req);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("주문 생성 실패 - DB 오류")
    void createPreOrder_Fail_DbError() {
        Long userId = 1L;
        OrderCreateRequestDto req = createValidOrderRequest();

        BookOrderResponse mockBook = createBookResponse(1L, "Test Book", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD-001");

        given(orderTransactionService.createPendingOrder(any(), any()))
                .willThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(OrderVerificationException.class);
    }

    @Test
    @DisplayName("주문 생성 실패 - 리소스 선점 오류")
    void createPreOrder_Fail_ResourceError() {
        Long userId = 1L;
        OrderCreateRequestDto req = createValidOrderRequest();

        BookOrderResponse mockBook = createBookResponse(1L, "Test Book", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD-001");

        OrderCreateResponseDto responseDto = new OrderCreateResponseDto();
        responseDto.setOrderId(1L);
        responseDto.setOrderNumber("ORD-001");

        given(orderTransactionService.createPendingOrder(eq(userId), any())).willReturn(responseDto);

        org.mockito.Mockito.doThrow(new RuntimeException("Resource Error"))
                .when(resourceManager).prepareResources(any(), any(), any(), any());

        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(OrderVerificationException.class);

        verify(resourceManager).releaseResources(any(), any(), any(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("검증 실패 - 책 정보 불일치")
    void verifyOrder_Fail_BookMismatch() {
        Long userId = 1L;
        OrderCreateRequestDto req = createValidOrderRequest();

        // 요청은 ID 1, 응답은 ID 999
        BookOrderResponse mockBook = createBookResponse(999L, "Test Book", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));

        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(OrderVerificationException.class);
    }

    @Test
    @DisplayName("검증 실패 - 배송비 정책 없음")
    void verifyOrder_Fail_PolicyNotFound() {
        Long userId = 1L;
        OrderCreateRequestDto req = createValidOrderRequest();

        BookOrderResponse mockBook = createBookResponse(1L, "Test Book", 10000L, 10L);
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));

        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(DeliveryPolicyNotFoundException.class);
    }

    @Test
    @DisplayName("검증 실패 - 배송일 너무 빠름")
    void verifyOrder_Fail_DeliveryDate_TooEarly() {
        Long userId = 1L;
        DeliveryAddressRequestDto address = new DeliveryAddressRequestDto("R", "A", "D", "P", "M");
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1, false, null);

        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(item), address, 1L, LocalDate.now(), null, null
        );

        BookOrderResponse mockBook = createBookResponse(1L, "Test Book", 1000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));

        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(InvalidDeliveryDateException.class);
    }

    @Test
    @DisplayName("쿠폰 최소 금액 미달")
    void createCouponDiscount_Fail_MinPrice() {
        Long userId = 1L;
        DeliveryAddressRequestDto address = new DeliveryAddressRequestDto("R", "A", "D", "P", "M");
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1, false, null);
        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(item), address, 1L, LocalDate.now().plusDays(2), 10L, null
        );

        BookOrderResponse mockBook = createBookResponse(1L, "Test Book", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));

        CouponTargetResponseDto coupon = new CouponTargetResponseDto(
                10L, null, null, 50000, 5000, CouponPolicyDiscountType.FIXED, 5000
        );
        given(couponServiceClient.getCouponTargets(10L)).willReturn(coupon);

        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(OrderVerificationException.class);
    }

    @Test
    @DisplayName("쿠폰 최소 결제 금액(100원) 미만 로깅 체크")
    void createCouponDiscount_Log_Under100() {
        // 이 테스트는 Exception을 던지지 않지만 로직을 타는지 확인합니다.
        Long userId = 1L;
        DeliveryAddressRequestDto address = new DeliveryAddressRequestDto("R", "A", "D", "P", "M");
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1, false, null); // 1000원
        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(item), address, 1L, LocalDate.now().plusDays(2), 10L, null
        );

        BookOrderResponse mockBook = createBookResponse(1L, "Test", 1000L, 10L);
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD");

        // 할인 950원 -> 남은 금액 50원 (< 100원)
        CouponTargetResponseDto coupon = new CouponTargetResponseDto(
                10L, null, null, 0, 2000, CouponPolicyDiscountType.FIXED, 950
        );
        given(couponServiceClient.getCouponTargets(10L)).willReturn(coupon);

        given(orderTransactionService.createPendingOrder(any(), any())).willReturn(new OrderCreateResponseDto());

        orderService.createPreOrder(userId, null, req);
    }

    @Test
    @DisplayName("쿠폰 정액 할인")
    void createCouponDiscount_Fixed() {
        Long userId = 1L;
        DeliveryAddressRequestDto address = new DeliveryAddressRequestDto("R", "A", "D", "P", "M");
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1, false, null);
        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(item), address, 1L, LocalDate.now().plusDays(2), 10L, null
        );

        BookOrderResponse mockBook = createBookResponse(1L, "Test", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD");

        CouponTargetResponseDto coupon = new CouponTargetResponseDto(
                10L, null, null, 5000, 1000, CouponPolicyDiscountType.FIXED, 1000
        );
        given(couponServiceClient.getCouponTargets(10L)).willReturn(coupon);

        given(orderTransactionService.createPendingOrder(any(), any())).willReturn(new OrderCreateResponseDto());

        orderService.createPreOrder(userId, null, req);
    }

    @Test
    @DisplayName("포인트 초과 예외")
    void createPointDiscount_Fail_ExceedBalance() {
        Long userId = 1L;
        DeliveryAddressRequestDto address = new DeliveryAddressRequestDto("R", "A", "D", "P", "M");
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1, false, null);
        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(item), address, 1L, LocalDate.now().plusDays(2), null, 2000
        );

        BookOrderResponse mockBook = createBookResponse(1L, "Test", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(1000));

        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(ExceedUserPointException.class);
    }

    @Test
    @DisplayName("포인트 사용 시 최소 결제금액 100원 미만 예외")
    void createPointDiscount_Fail_Under100() {
        Long userId = 1L;
        DeliveryAddressRequestDto address = new DeliveryAddressRequestDto("R", "A", "D", "P", "M");
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1, false, null); // 1000원
        OrderCreateRequestDto req = new OrderCreateRequestDto(
                List.of(item), address, 1L, LocalDate.now().plusDays(2), null, 950
        );

        BookOrderResponse mockBook = createBookResponse(1L, "Test", 1000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class))); // 0원 가정
        given(userServiceClient.getUserPoint(userId)).willReturn(new CurrentPointResponseDto(10000));

        // 1000 - 950 = 50원 < 100원
        assertThatThrownBy(() -> orderService.createPreOrder(userId, null, req))
                .isInstanceOf(ExceedUserPointException.class);
    }

    @Test
    @DisplayName("비회원 주문 생성 성공")
    void createGuestPreOrder_Success() {
        String guestId = "guest";
        DeliveryAddressRequestDto address = new DeliveryAddressRequestDto("R", "A", "D", "P", "M");
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1, false, null);

        GuestOrderCreateRequestDto req = new GuestOrderCreateRequestDto();
        req.setOrderItems(List.of(item));
        req.setDeliveryAddress(address);
        req.setDeliveryPolicyId(1L);
        req.setWantDeliveryDate(LocalDate.now().plusDays(2));
        req.setGuestName("name");
        req.setGuestPhoneNumber("phone");
        req.setGuestPassword("pass");

        BookOrderResponse mockBook = createBookResponse(1L, "Test", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD");

        OrderCreateResponseDto responseDto = new OrderCreateResponseDto();
        responseDto.setOrderId(1L);
        responseDto.setOrderNumber("ORD");

        given(orderTransactionService.createPendingOrder(isNull(), any())).willReturn(responseDto);
        given(orderTransactionService.getOrderEntity("ORD")).willReturn(mock(Order.class));
        given(passwordEncoder.encode("pass")).willReturn("encoded");

        OrderCreateResponseDto result = orderService.createGuestPreOrder(guestId, req);

        assertThat(result).isNotNull();
        verify(guestOrderRepository).save(any(GuestOrder.class));
    }

    @Test
    @DisplayName("비회원 주문 DB 저장 실패")
    void createGuestPreOrder_Fail_DbError() {
        String guestId = "guest";
        DeliveryAddressRequestDto address = new DeliveryAddressRequestDto("R", "A", "D", "P", "M");
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1, false, null);

        GuestOrderCreateRequestDto req = new GuestOrderCreateRequestDto();
        req.setOrderItems(List.of(item));
        req.setDeliveryAddress(address);
        req.setDeliveryPolicyId(1L);
        req.setWantDeliveryDate(LocalDate.now().plusDays(2));
        req.setGuestName("name");
        req.setGuestPhoneNumber("phone");
        req.setGuestPassword("pass");

        BookOrderResponse mockBook = createBookResponse(1L, "Test", 10000L, 10L);

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(mockBook));
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD");

        OrderCreateResponseDto responseDto = new OrderCreateResponseDto();
        responseDto.setOrderId(1L);
        responseDto.setOrderNumber("ORD");

        given(orderTransactionService.createPendingOrder(isNull(), any())).willReturn(responseDto);
        given(orderTransactionService.getOrderEntity("ORD")).willReturn(mock(Order.class));
        given(passwordEncoder.encode("pass")).willReturn("encoded");

        given(guestOrderRepository.save(any())).willThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> orderService.createGuestPreOrder(guestId, req))
                .isInstanceOf(OrderVerificationException.class);
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Member_Success() {
        Long userId = 1L;
        String orderNumber = "ORD-001";
        Order mockOrder = mock(Order.class);

        given(mockOrder.getOrderStatus()).willReturn(OrderStatus.COMPLETED);

        given(mockOrder.getOrderNumber()).willReturn(orderNumber);

        given(orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)).willReturn(Optional.of(mockOrder));

        // when
        orderService.cancelOrder(userId, orderNumber);

        // then
        verify(paymentService).cancelPayment(any());
        verify(orderTransactionService).changeStatusOrder(mockOrder, false);
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    void getOrderDetail_Success() {
        Long userId = 1L;
        String orderNumber = "ORD-001";
        Order mockOrder = mock(Order.class);
        OrderItem mockItem = mock(OrderItem.class);
        given(mockOrder.getOrderItems()).willReturn(List.of(mockItem));
        given(mockItem.getBookId()).willReturn(1L);

        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(mockOrder));
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of());
        given(orderViewAssembler.toOrderDetailView(any(), any())).willReturn(new OrderDetailResponseDto());
        given(paymentService.getPayment(any())).willReturn(mock(PaymentResponse.class));

        OrderDetailResponseDto result = orderService.getOrderDetail(userId, orderNumber, null);

        assertThat(result).isNotNull();
        verify(orderTransactionService).validateOrderExistence(mockOrder, userId, null);
    }

    @Test
    @DisplayName("주문 상세 조회 - 존재하지 않음")
    void getOrderDetail_Fail_NotFound() {
        given(orderRepository.findByOrderNumber("NONE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderDetail(1L, "NONE", null))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("주문 타이틀 생성 테스트 (2권 초과)")
    void createOrderTitle_Multiple() {
        // createOrderTitle private 메서드 테스트를 위해 createPreOrder를 이용
        Long userId = 1L;
        OrderCreateRequestDto req = createValidOrderRequest();

        // 아이템 3개
        List<OrderItemRequestDto> items = new ArrayList<>();
        items.add(new OrderItemRequestDto(1L, 1, false, null));
        items.add(new OrderItemRequestDto(2L, 1, false, null));
        items.add(new OrderItemRequestDto(3L, 1, false, null));
        req.setOrderItems(items);

        List<BookOrderResponse> books = new ArrayList<>();
        books.add(createBookResponse(1L, "Book1", 1000L, 1L));
        books.add(createBookResponse(2L, "Book2", 1000L, 1L));
        books.add(createBookResponse(3L, "Book3", 1000L, 1L));

        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(books);
        given(deliveryPolicyRepository.findById(any())).willReturn(Optional.of(mock(DeliveryPolicy.class)));
        given(orderNumberProvider.provideOrderNumber()).willReturn("ORD");
        given(orderTransactionService.createPendingOrder(any(), any())).willReturn(new OrderCreateResponseDto());

        orderService.createPreOrder(userId, null, req);
    }

    private OrderCreateRequestDto createValidOrderRequest() {
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1, false, null);
        DeliveryAddressRequestDto address = new DeliveryAddressRequestDto("R", "A", "D", "P", "M");
        return new OrderCreateRequestDto(
                List.of(item), address, 1L, LocalDate.now().plusDays(2), null, null
        );
    }

    // 세터 없는 DTO 생성을 위한 리플렉션 유틸 (protected/no-setter fields)
    private BookOrderResponse createBookResponse(Long id, String title, Long price, Long categoryId) {
        BookOrderResponse book = new BookOrderResponse();
        ReflectionTestUtils.setField(book, "bookId", id);
        ReflectionTestUtils.setField(book, "title", title);
        ReflectionTestUtils.setField(book, "priceSales", price);
        ReflectionTestUtils.setField(book, "categoryId", categoryId);
        return book;
    }
}