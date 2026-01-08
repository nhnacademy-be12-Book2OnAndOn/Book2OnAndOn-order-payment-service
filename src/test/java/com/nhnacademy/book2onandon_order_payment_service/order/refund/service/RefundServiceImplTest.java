package com.nhnacademy.book2onandon_order_payment_service.order.refund.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.nhnacademy.book2onandon_order_payment_service.client.BookServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.UserServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundItemRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundSearchCondition;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundStatusUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.response.RefundAvailableItemResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundReason;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.RefundNotCancelableException;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.RefundNotFoundException;
import com.nhnacademy.book2onandon_order_payment_service.order.provider.GuestTokenProvider;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.refund.RefundItemRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.refund.RefundRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.impl.RefundServiceImpl;
import feign.FeignException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @InjectMocks
    RefundServiceImpl refundService;

    @Mock RefundRepository refundRepository;
    @Mock RefundItemRepository refundItemRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock DeliveryRepository deliveryRepository;

    @Mock BookServiceClient bookServiceClient;
    @Mock UserServiceClient userServiceClient;

    @Mock ApplicationEventPublisher applicationEventPublisher;
    @Mock GuestTokenProvider guestTokenProvider;

    // =========================================================
    // 1) 관리자 검색 (기존 테스트 보강)
    // =========================================================

    @Test
    @DisplayName("관리자 검색: 유저 키워드로 검색했지만 결과 없으면 DB 조회 없이 빈 페이지 반환")
    void search_with_keyword_no_result() {
        RefundSearchCondition condition = new RefundSearchCondition();
        condition.setUserKeyword("존재하지않는유저");
        Pageable pageable = PageRequest.of(0, 10);

        given(userServiceClient.searchUserIdsByKeyword("존재하지않는유저"))
                .willReturn(Collections.emptyList());

        Page<RefundResponseDto> result = refundService.getRefundListForAdmin(condition, pageable);

        assertThat(result.isEmpty()).isTrue();
        verify(refundRepository, never()).searchRefunds(any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("관리자 검색: userId가 있으면 keyword보다 우선하며, keyword 검색 호출 없이 userId로만 조회")
    void search_userId_priority_over_keyword() {
        RefundSearchCondition condition = new RefundSearchCondition();
        condition.setUserId(99L);
        condition.setUserKeyword("홍길동"); // 있어도 무시되어야 함
        Pageable pageable = PageRequest.of(0, 10);

        Refund refund = mock(Refund.class);
        given(refund.getRefundItems()).willReturn(List.of());
        Page<Refund> mockPage = new PageImpl<>(List.of(refund));

        // includeGuest 기본값이 false일 가능성이 높아서 그대로 eq(false)로 검증
        given(refundRepository.searchRefunds(
                any(), any(), any(),
                eq(List.of(99L)),
                any(),
                eq(condition.isIncludeGuest()),
                any(Pageable.class)
        )).willReturn(mockPage);

        try (MockedStatic<RefundResponseDto> mocked = mockStatic(RefundResponseDto.class)) {
            mocked.when(() -> RefundResponseDto.from(any(Refund.class), anyMap()))
                    .thenReturn(mock(RefundResponseDto.class));

            refundService.getRefundListForAdmin(condition, pageable);
        }

        verify(userServiceClient, never()).searchUserIdsByKeyword(anyString());
        verify(refundRepository, times(1)).searchRefunds(any(), any(), any(), eq(List.of(99L)), any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("관리자 검색: 날짜 + keyword 정상 검색 (to는 plusDays(1) atStartOfDay)")
    void search_normal_with_dates_and_keyword() {
        RefundSearchCondition condition = new RefundSearchCondition();
        condition.setStartDate(LocalDate.of(2025, 1, 1));
        condition.setEndDate(LocalDate.of(2025, 1, 31));
        condition.setUserKeyword("홍길동");

        List<Long> userIds = List.of(1L, 2L);
        given(userServiceClient.searchUserIdsByKeyword("홍길동")).willReturn(userIds);

        Refund refund = mock(Refund.class);
        given(refund.getRefundItems()).willReturn(List.of());
        Page<Refund> refundPage = new PageImpl<>(List.of(refund));

        given(refundRepository.searchRefunds(
                eq(null),
                eq(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
                eq(LocalDateTime.of(2025, 2, 1, 0, 0, 0)),
                eq(userIds),
                eq(null),
                eq(condition.isIncludeGuest()),
                any(Pageable.class)
        )).willReturn(refundPage);

        try (MockedStatic<RefundResponseDto> mocked = mockStatic(RefundResponseDto.class)) {
            mocked.when(() -> RefundResponseDto.from(any(Refund.class), anyMap()))
                    .thenReturn(mock(RefundResponseDto.class));

            refundService.getRefundListForAdmin(condition, PageRequest.of(0, 10));
        }

        verify(refundRepository, times(1)).searchRefunds(any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    // =========================================================
    // 2) createRefund - 핵심 분기 커버
    // =========================================================

    @Test
    @DisplayName("createRefund(회원): 10일 이내 OTHER면 자동 IN_INSPECTION, 아이템/주문 상태 변경 및 저장")
    void createRefund_member_success_autoAccept() {
        Long orderId = 10L;
        Long userId = 1L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);
        given(order.getUserId()).willReturn(userId);
        given(order.getOrderStatus()).willReturn(OrderStatus.DELIVERED);

        OrderItem oi = mock(OrderItem.class);
        given(oi.getOrderItemId()).willReturn(11L);
        given(oi.getOrderItemStatus()).willReturn(OrderItemStatus.ORDER_COMPLETE);
        given(oi.getOrderItemQuantity()).willReturn(3);
        given(oi.getBookId()).willReturn(100L);

        given(order.getOrderItems()).willReturn(List.of(oi));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        Delivery delivery = mock(Delivery.class);
        given(delivery.getDeliveryStartAt()).willReturn(LocalDateTime.now().minusDays(5));
        given(deliveryRepository.findByOrder_OrderId(orderId)).willReturn(Optional.of(delivery));

        RefundItemRequestDto itemReq = mock(RefundItemRequestDto.class);
        given(itemReq.getOrderItemId()).willReturn(11L);
        given(itemReq.getRefundQuantity()).willReturn(1);

        RefundRequestDto req = new RefundRequestDto(orderId, List.of(itemReq), "OTHER", "detail");

        given(orderItemRepository.findByOrderIdAndItemIdsForUpdate(eq(orderId), anyList()))
                .willReturn(List.of(oi));

        // snapshot: 완료/진행중 없음
        given(refundItemRepository.sumByOrderIdAndStatus(orderId, RefundStatus.REFUND_COMPLETED))
                .willReturn(List.of());
        given(refundItemRepository.sumByOrderIdAndStatuses(eq(orderId), anyList()))
                .willReturn(List.of());

        try (MockedStatic<RefundResponseDto> mocked = mockStatic(RefundResponseDto.class)) {
            mocked.when(() -> RefundResponseDto.from(any(Refund.class), anyMap()))
                    .thenReturn(mock(RefundResponseDto.class));

            RefundResponseDto result = refundService.createRefund(orderId, userId, req, null);
            assertThat(result).isNotNull();
        }

        verify(orderItemRepository, times(1)).findByOrderIdAndItemIdsForUpdate(eq(orderId), anyList());
        verify(oi, times(1)).setOrderItemStatus(OrderItemStatus.RETURN_REQUESTED);
        verify(order, times(1)).updateStatus(OrderStatus.RETURN_REQUESTED);
        verify(refundRepository, times(1)).save(any(Refund.class));
    }

    @Test
    @DisplayName("createRefund: OTHER + 10일 초과면 자동 REJECTED, 주문/아이템 락/상태변경 없이 refund만 저장")
    void createRefund_autoRejected_no_item_lock() {
        Long orderId = 10L;
        Long userId = 1L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);
        given(order.getUserId()).willReturn(userId);
        given(order.getOrderStatus()).willReturn(OrderStatus.DELIVERED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        Delivery delivery = mock(Delivery.class);
        given(delivery.getDeliveryStartAt()).willReturn(LocalDateTime.now().minusDays(15));
        given(deliveryRepository.findByOrder_OrderId(orderId)).willReturn(Optional.of(delivery));

        RefundItemRequestDto itemReq = mock(RefundItemRequestDto.class);
        given(itemReq.getOrderItemId()).willReturn(11L);
        given(itemReq.getRefundQuantity()).willReturn(1);

        RefundRequestDto req = new RefundRequestDto(orderId, List.of(itemReq), "OTHER", null);

        try (MockedStatic<RefundResponseDto> mocked = mockStatic(RefundResponseDto.class)) {
            mocked.when(() -> RefundResponseDto.from(any(Refund.class), anyMap()))
                    .thenReturn(mock(RefundResponseDto.class));

            refundService.createRefund(orderId, userId, req, null);
        }

        verify(orderItemRepository, never()).findByOrderIdAndItemIdsForUpdate(anyLong(), anyList());
        verify(refundRepository, times(1)).save(any(Refund.class));
    }

    @Test
    @DisplayName("createRefund: 주문이 없으면 OrderNotFoundException")
    void createRefund_orderNotFound() {
        given(orderRepository.findById(999L)).willReturn(Optional.empty());

        RefundRequestDto req = new RefundRequestDto(999L, List.of(mock(RefundItemRequestDto.class)), "OTHER", null);

        assertThatThrownBy(() -> refundService.createRefund(999L, 1L, req, null))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("createRefund: 회원인데 주문 userId 불일치면 AccessDeniedException")
    void createRefund_member_accessDenied() {
        Long orderId = 10L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);
        given(order.getUserId()).willReturn(1L);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        RefundRequestDto req = new RefundRequestDto(orderId, List.of(mock(RefundItemRequestDto.class)), "OTHER", null);

        assertThatThrownBy(() -> refundService.createRefund(orderId, 999L, req, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================
    // 3) cancelRefund - 취소가능/불가 분기 커버
    // =========================================================

    @Test
    @DisplayName("cancelRefund: refund가 없으면 RefundNotFoundException")
    void cancelRefund_notFound() {
        given(refundRepository.findByIdForUpdate(1L)).willReturn(null);

        assertThatThrownBy(() -> refundService.cancelRefund(10L, 1L, 1L, null))
                .isInstanceOf(RefundNotFoundException.class);
    }

    @Test
    @DisplayName("cancelRefund: 상태가 취소불가(APPROVED 등)이면 RefundNotCancelableException")
    void cancelRefund_notCancelableStatus() {
        Long refundId = 1L;
        Long orderId = 10L;
        Long userId = 1L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);
        given(order.getUserId()).willReturn(userId);

        Refund refund = Refund.create(order, RefundReason.OTHER, null);
        refund.setRefundId(refundId);
        refund.setRefundStatus(RefundStatus.APPROVED); // 취소불가

        given(refundRepository.findByIdForUpdate(refundId)).willReturn(refund);

        assertThatThrownBy(() -> refundService.cancelRefund(orderId, refundId, userId, null))
                // [수정] 정확히 발생한 예외 클래스를 지정합니다.
                .isInstanceOf(RefundNotCancelableException.class)
                // [수정] 실제 로그에 찍힌 메시지와 일치하도록 검증 문구를 보강합니다.
                .hasMessageContaining("현재 상태에서는 반품 취소가 불가합니다.")
                .hasMessageContaining("refundId=" + refundId)
                .hasMessageContaining("status=" + RefundStatus.APPROVED)
                .hasMessageContaining("cancelable=REQUESTED,IN_INSPECTION");
    }

    @Test
    @DisplayName("cancelRefund: REQUESTED면 REQUEST_CANCELED로 변경되고, 아이템/주문 상태가 롤백된다")
    void cancelRefund_success_rollbacks() {
        Long refundId = 1L;
        Long orderId = 10L;
        Long userId = 1L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);
        given(order.getUserId()).willReturn(userId);

        OrderItem oi = mock(OrderItem.class);
        given(oi.getOrderItemStatus()).willReturn(OrderItemStatus.ORDER_COMPLETE);
        given(order.getOrderItems()).willReturn(List.of(oi));

        Refund refund = Refund.create(order, RefundReason.OTHER, null);
        refund.setRefundId(refundId);
        refund.setRefundStatus(RefundStatus.REQUESTED);
        refund.setOriginalOrderStatus(OrderStatus.COMPLETED.getCode());

        RefundItem ri = RefundItem.create(refund, oi, 1);
        refund.addRefundItem(ri);

        given(refundRepository.findByIdForUpdate(refundId)).willReturn(refund);
        given(refundRepository.existsActiveRefundByOrderIdExcludingRefundId(eq(orderId), eq(refundId), anyList()))
                .willReturn(false);

        try (MockedStatic<RefundResponseDto> mocked = mockStatic(RefundResponseDto.class)) {
            mocked.when(() -> RefundResponseDto.from(any(Refund.class), anyMap()))
                    .thenReturn(mock(RefundResponseDto.class));

            refundService.cancelRefund(orderId, refundId, userId, null);
        }

        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.REQUEST_CANCELED);
        verify(oi, times(1)).setOrderItemStatus(OrderItemStatus.ORDER_COMPLETE);
        verify(order, times(1)).updateStatus(OrderStatus.COMPLETED);
    }

    // =========================================================
    // 4) updateRefundStatus - 전이 규칙 + 완료 처리 분기 커버
    // =========================================================

    @Test
    @DisplayName("updateRefundStatus: REQUESTED -> REFUND_COMPLETED(코드 3 가정)은 허용되지 않아 예외")
    void updateRefundStatus_invalid_transition() {
        Long refundId = 1L;

        Order order = mock(Order.class);
        Refund refund = Refund.create(order, RefundReason.OTHER, null);
        refund.setRefundId(refundId);
        refund.setRefundStatus(RefundStatus.REQUESTED);

        given(refundRepository.findByIdForUpdate(refundId)).willReturn(refund);

        RefundStatusUpdateRequestDto req =
                new RefundStatusUpdateRequestDto(RefundStatus.REFUND_COMPLETED.getCode());

        assertThatThrownBy(() -> refundService.updateRefundStatus(refundId, req))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updateRefundStatus: 이미 REFUND_COMPLETED면 변경 없이 그대로 반환")
    void updateRefundStatus_alreadyCompleted_returns() {
        Long refundId = 1L;

        Order order = mock(Order.class);
        Refund refund = Refund.create(order, RefundReason.OTHER, null);
        refund.setRefundId(refundId);

        refund.setRefundStatus(RefundStatus.REFUND_COMPLETED);
        given(refundRepository.findByIdForUpdate(refundId)).willReturn(refund);

        RefundStatusUpdateRequestDto req =
                new RefundStatusUpdateRequestDto(RefundStatus.REFUND_COMPLETED.getCode()); // 6

        try (MockedStatic<RefundResponseDto> mocked = mockStatic(RefundResponseDto.class)) {
            mocked.when(() -> RefundResponseDto.from(any(Refund.class), anyMap()))
                    .thenReturn(mock(RefundResponseDto.class));

            refundService.updateRefundStatus(refundId, req);
        }
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("updateRefundStatus: APPROVED -> REFUND_COMPLETED(코드 6) 성공 시 아이템/주문 상태 변경 + 이벤트 발행")
    void updateRefundStatus_completed_path_updates_and_publishes_event() {
        Long refundId = 1L;
        Long orderId = 10L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);

        OrderItem oi = mock(OrderItem.class);

        // ===== set 되면 get도 바뀌게 만든다 =====
        AtomicReference<OrderItemStatus> statusRef =
                new AtomicReference<>(OrderItemStatus.ORDER_COMPLETE);

        doAnswer(inv -> {
            OrderItemStatus newStatus = inv.getArgument(0, OrderItemStatus.class);
            statusRef.set(newStatus);
            return null;
        }).when(oi).setOrderItemStatus(any(OrderItemStatus.class));

        given(oi.getOrderItemStatus()).willAnswer(inv -> statusRef.get());
        // ================================================

        given(oi.getBookId()).willReturn(100L);
        given(oi.getUnitPrice()).willReturn(10000);
        given(oi.getOrderItemQuantity()).willReturn(2);

        given(order.getOrderItems()).willReturn(List.of(oi));

        // 포인트 계산용
        given(order.getPointDiscount()).willReturn(1000);
        given(order.getCouponDiscount()).willReturn(500);
        given(order.getDeliveryFee()).willReturn(2500);

        Delivery delivery = mock(Delivery.class);
        given(delivery.getDeliveryStartAt()).willReturn(LocalDateTime.now().minusDays(5));
        given(deliveryRepository.findByOrder_OrderId(orderId)).willReturn(Optional.of(delivery));

        Refund refund = Refund.create(order, RefundReason.OTHER, null);
        refund.setRefundId(refundId);
        refund.setRefundStatus(RefundStatus.APPROVED);

        RefundItem ri = RefundItem.create(refund, oi, 1);
        refund.addRefundItem(ri);

        given(refundRepository.findByIdForUpdate(refundId)).willReturn(refund);

        // completedBase 계산에서 과거 완료 반품 리스트 조회
        given(refundRepository.findByOrderOrderId(orderId)).willReturn(List.of());

        // 배송비 차감 중복 체크
        given(refundRepository.existsCompletedRefundWithShippingDeduction(anyLong(), anyLong(), eq(RefundStatus.REFUND_COMPLETED)))
                .willReturn(false);

        // 제목 조회 실패 분기
        given(bookServiceClient.getBooksForOrder(anyList())).willThrow(mock(FeignException.class));

        RefundStatusUpdateRequestDto req = new RefundStatusUpdateRequestDto(6);

        try (MockedStatic<RefundResponseDto> mocked = mockStatic(RefundResponseDto.class)) {
            mocked.when(() -> RefundResponseDto.from(any(Refund.class), anyMap()))
                    .thenReturn(mock(RefundResponseDto.class));

            refundService.updateRefundStatus(refundId, req);
        }

        // item -> RETURN_COMPLETED
        verify(oi, times(1)).setOrderItemStatus(OrderItemStatus.RETURN_COMPLETED);

        // 주문 상태 검증(서비스 로직이 전체반품이라고 판단하면 RETURN_COMPLETED로 감)
        verify(order, times(1)).updateStatus(OrderStatus.RETURN_COMPLETED);

        verify(applicationEventPublisher, times(1)).publishEvent(any(Object.class));
        assertThat(refund.getShippingDeductionAmount()).isNotNull();
        assertThat(refund.getShippingDeductionAmount()).isNotNegative();
    }


    // =========================================================
    // 5) getRefundableItems / getRefundDetails / getRefundsForMember
    // =========================================================

    @Test
    @DisplayName("getRefundableItems: 주문상태/기간 정책 통과 시 폼 응답 생성")
    void getRefundableItems_success() {
        Long orderId = 10L;
        Long userId = 1L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);
        given(order.getUserId()).willReturn(userId);
        given(order.getOrderStatus()).willReturn(OrderStatus.DELIVERED);

        OrderItem oi = mock(OrderItem.class);
        given(oi.getOrderItemId()).willReturn(11L);
        given(oi.getBookId()).willReturn(100L);
        given(oi.getOrderItemQuantity()).willReturn(2);
        given(oi.getUnitPrice()).willReturn(10000);
        given(oi.getOrderItemStatus()).willReturn(OrderItemStatus.DELIVERED);

        given(order.getOrderItems()).willReturn(List.of(oi));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        Delivery delivery = mock(Delivery.class);
        given(delivery.getDeliveryStartAt()).willReturn(LocalDateTime.now().minusDays(5));
        given(deliveryRepository.findByOrder_OrderId(orderId)).willReturn(Optional.of(delivery));

        // snapshot: 완료/진행중 없음
        given(refundItemRepository.sumByOrderIdAndStatus(
                orderId,
                RefundStatus.REFUND_COMPLETED
        )).willReturn(List.of());
        given(refundItemRepository.sumByOrderIdAndStatuses(eq(orderId), anyList()))
                .willReturn(List.of());

        BookOrderResponse bor = mock(BookOrderResponse.class);
        given(bor.getBookId()).willReturn(100L);
        given(bor.getTitle()).willReturn("제목");
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(bor));

        List<RefundAvailableItemResponseDto> result = refundService.getRefundableItems(orderId, userId, null);

        assertThat(result)
                .isNotNull()
                .hasSize(1);
    }

    @Test
    @DisplayName("getRefundDetails: 주문/반품 매칭 실패면 IllegalArgumentException")
    void getRefundDetails_order_mismatch() {
        Long orderId = 10L;
        Long refundId = 1L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(999L); // mismatch

        Refund refund = Refund.create(order, RefundReason.OTHER, null);
        refund.setRefundId(refundId);

        given(refundRepository.findById(refundId)).willReturn(Optional.of(refund));

        assertThatThrownBy(() -> refundService.getRefundDetails(orderId, refundId, 1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getRefundsForMember: 목록 조회 후 DTO 변환(from) 경유")
    void getRefundsForMember_success() {
        Long userId = 1L;

        Refund refund = mock(Refund.class);
        RefundItem ri = mock(RefundItem.class);
        OrderItem oi = mock(OrderItem.class);

        given(oi.getBookId()).willReturn(100L);
        given(ri.getOrderItem()).willReturn(oi);
        given(refund.getRefundItems()).willReturn(List.of(ri));

        Page<Refund> page = new PageImpl<>(List.of(refund));
        given(refundRepository.findByOrderUserId(eq(userId), any(Pageable.class))).willReturn(page);

        BookOrderResponse bor = mock(BookOrderResponse.class);
        given(bor.getBookId()).willReturn(100L);
        given(bor.getTitle()).willReturn("제목");
        given(bookServiceClient.getBooksForOrder(anyList())).willReturn(List.of(bor));

        try (MockedStatic<RefundResponseDto> mocked = mockStatic(RefundResponseDto.class)) {
            mocked.when(() -> RefundResponseDto.from(any(Refund.class), anyMap()))
                    .thenReturn(mock(RefundResponseDto.class));

            Page<RefundResponseDto> result = refundService.getRefundsForMember(userId, PageRequest.of(0, 10));
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }
}
