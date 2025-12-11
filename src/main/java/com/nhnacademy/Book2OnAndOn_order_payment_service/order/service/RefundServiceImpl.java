package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.PointServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.StockDecreaseRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundItemDetailDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundPointRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundStatusUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundReason;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.RefundNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.refund.RefundItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.refund.RefundRepository;
import feign.FeignException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;

    private final BookServiceClient bookServiceClient; // 도서 제목 조회
    private final UserServiceClient userServiceClient;
    private final PointServiceClient pointServiceClient;

    // =========================
    // 1. 회원
    // =========================
    // 1) 회원 반품 신청
    @Override
    public RefundResponseDto createRefundForMember(Long orderId, Long userId, RefundRequestDto request) {

        Order order = loadOrder(orderId);

        // 1) 본인 주문인지 검증
        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인의 주문에 대해서만 반품 신청이 가능합니다.");
        }

        // 2) 반품 사유 파싱
        RefundReason reason = RefundReason.valueOf(request.getRefundReason());

        // 3) 반품 가능 여부 검증
        validateRefundEligibility(order, reason);

        // 4) Refund 엔티티 생성
        Refund refund = new Refund();
        refund.setRefundReason(request.getRefundReason());
        refund.setRefundReasonDetail(request.getRefundReasonDetail());
        refund.setRefundStatus(RefundStatus.REQUESTED.getCode());
        refund.setRefundCreatedAt(LocalDateTime.now());
        refund.setOrder(order);
        refundRepository.save(refund);
        order.getRefunds().add(refund);

        // 5) RefundItem
        List<RefundItemRequestDto> itemRequests = request.getRefundItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("반품할 항목이 없습니다.");
        }

        for (RefundItemRequestDto dto : itemRequests) {
            OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                    .orElseThrow(() -> new IllegalArgumentException("주문 항목을 찾을 수 없습니다. orderItemId=" + dto.getOrderItemId()));

            // 수량 + 진행 중 반품 검증
            validateRefundQuantity(orderItem, dto.getRefundQuantity());
            validateNoActiveRefund(orderItem);

            RefundItem refundItem = new RefundItem();
            refundItem.setRefundQuantity(dto.getRefundQuantity());
            refundItem.setRefund(refund);
            refundItem.setOrderItem(orderItem);

            refundItemRepository.save(refundItem);
            refund.getRefundItem().add(refundItem);
        }

        // 5) 주문 상태 변경 (반품 신청 상태로)
        order.updateStatus(OrderStatus.RETURN_REQUESTED);

        return toRefundResponseDto(refund);
    }

    // 2) 회원 반품 상세 조회
    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundDetailsForMember(Long userId, Long refundId) {
        Refund refund = loadRefund(refundId);
        if (!refund.getOrder().getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 반품 내역에 대한 접근 권한이 없습니다.");
        }
        return toRefundResponseDto(refund);
    }

    // 3) 회원의 전체 반품 목록 조회
    @Override
    public Page<RefundResponseDto> getRefundsForMember(Long userId, Pageable pageable) {
        Page<Refund> page = refundRepository.findByOrderUserId(userId, pageable);
        return page.map(this::toRefundResponseDto);
    }

    // 4) 반품 신청 취소 -> 안 해

    // =========================
    // 2. 비회원
    // =========================
    // 1) 비회원 반품 신청
    @Override
    public RefundResponseDto createRefundForGuest(Long orderId, RefundRequestDto request) {
        Order order = loadOrder(orderId);

        // 회원주문이 아니여야 함
        if (order.getUserId() != null) {
            throw new IllegalStateException("회원 주문입니다. 비회원 반품 API를 잘못 호출했습니다.");
        }
        if (order.getGuestOrder() == null) {
            throw new IllegalStateException("비회원 주문 정보가 존재하지 않습니다.");
        }

        RefundReason reason = RefundReason.valueOf(request.getRefundReason());
        validateRefundEligibility(order, reason);

        Refund refund = new Refund();
        refund.setRefundReason(request.getRefundReason());
        refund.setRefundReasonDetail(request.getRefundReasonDetail());
        refund.setRefundStatus(RefundStatus.REQUESTED.getCode());
        refund.setRefundCreatedAt(LocalDateTime.now());
        refund.setOrder(order);
        refundRepository.save(refund);
        order.getRefunds().add(refund);

        List<RefundItemRequestDto> itemRequests = request.getRefundItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("반품할 항목이 없습니다.");
        }

        for (RefundItemRequestDto dto : itemRequests) {
            OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                    .orElseThrow(() -> new IllegalArgumentException("주문 항목을 찾을 수 없습니다. orderItemId=" + dto.getOrderItemId()));

            validateRefundQuantity(orderItem, dto.getRefundQuantity());
            validateNoActiveRefund(orderItem);

            RefundItem refundItem = new RefundItem();
            refundItem.setRefundQuantity(dto.getRefundQuantity());
            refundItem.setRefund(refund);
            refundItem.setOrderItem(orderItem);

            refundItemRepository.save(refundItem);
            refund.getRefundItem().add(refundItem);
        }

        order.updateStatus(OrderStatus.RETURN_REQUESTED);

        return toRefundResponseDto(refund);
    }

    // 2) 비회원 반품 상세 조회
    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundDetailsForGuest(Long refundId) {
        Refund refund = loadRefund(refundId);
        if (refund.getOrder().getUserId() != null) {
            throw new IllegalStateException("회원 주문의 반품입니다. 비회원 조회 API를 잘못 호출했습니다.");
        }
        if (refund.getOrder().getGuestOrder() == null) {
            throw new IllegalStateException("비회원 주문 정보가 존재하지 않습니다.");
        }
        return toRefundResponseDto(refund);
    }

    // 3) 비회원의 전체 반품 목록 조회 -> TODO 생각해보니 비회원은 반품 목록 조회는 안 될듯
//    @Override
//    @Transactional(readOnly = true)
//    public Page<RefundResponseDto> getRefundsForGuest(Long orderId, Pageable pageable) {
//        // 비회원 인증(비밀번호/이메일/전화번호 등)은 Controller/Facade 계층에서 이미 통과했다고 가정
//        Order order = loadOrder(orderId);
//
//        if (order.getUserId() != null) {
//            throw new IllegalStateException("회원 주문입니다. 비회원 반품 목록 API를 잘못 호출했습니다.");
//        }
//        if (order.getGuestOrder() == null) {
//            throw new IllegalStateException("비회원 주문 정보가 존재하지 않습니다.");
//        }
//
//        Page<Refund> page = refundRepository.findByOrderOrderId(orderId, pageable);
//        return page.map(this::toRefundResponseDto);
//    }

    // 4) 반품 신청 취소 -> dksgo

    // =========================
    // 3. 관리자
    // =========================
    // 1) 관리자 반품 목록 조회
    @Override
    @Transactional(readOnly = true)
    public Page<RefundResponseDto> getRefundListForAdmin(
            Integer refundStatus,
            LocalDate startDate,
            LocalDate endDate,
            Long userId,
            String userKeyword,
            String orderNumber,
            boolean includeGuest,
            Pageable pageable
    ) {
        // 1) LocalDate -> LocalDateTime 변환 (일 단위 기준)
        LocalDateTime from = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime to = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null; // endDate 의 "다음날 0시" 직전까지 포함시키기 위해 +1일 후 startOfDay를 upper bound 로 사용

        // 2) userKeyword 가 있으면 user-service 에서 userId 리스트 조회
        Long effectiveUserId = userId;
        if (effectiveUserId == null && userKeyword != null && !userKeyword.isBlank()) {
            List<Long> ids = userServiceClient.searchUserIdsByKeyword(userKeyword.trim());
            // 단순화를 위해 "1명만 정확히 매칭되면 그 userId로 검색"하는 전략 예시
            if (ids.size() == 1) {
                effectiveUserId = ids.get(0);
            }
        }

        // 3) orderNumber 공백 처리
        String normalizedOrderNumber = (orderNumber != null && !orderNumber.isBlank())
                ? orderNumber.trim()
                : null;

        // 4) Repository 호출
        Page<Refund> refundPage = refundRepository.searchRefunds(
                refundStatus,
                from,
                to,
                userId,
                normalizedOrderNumber,
                includeGuest,
                pageable
        );

        // 5) Refund -> RefundResponseDto 변환
        return refundPage.map(this::toRefundResponseDto);
    }

    // 2) 관리자 반품 상세 조회
    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundDetailsForAdmin(Long refundId) {
        Refund refund = loadRefund(refundId);
        return toRefundResponseDto(refund);
    }

    // 3) 관리자 반품 상태 변경
    @Override
    public RefundResponseDto updateRefundStatus(Long refundId, RefundStatusUpdateDto request) {

        Refund refund = loadRefund(refundId);
        RefundStatus newStatus = RefundStatus.fromCode(request.getStatusCode());

        // 상태 변경
        /**
         * - REFUND_COMPLETED 시:
         *   1) 재고 복구
         *   2) 주문/주문항목 상태 변경 (전체/부분 반품 반영)
         *   3) 포인트 환불 금액 계산 후 user-service에 포인트 적립 요청
         */
        refund.setRefundStatus(newStatus.getCode());

        // 환불(포인트) 완료 시 처리
        if (newStatus == RefundStatus.REFUND_COMPLETED) {
            // 재고 복구 및 주문/주문항복 상태 변경
            handleRefundCompleted(refund);
            // 포인트 환불 요청
            refundAsPoint(refund);
        }
        // 반품 거부 시 주문 상태 복원
        else if (newStatus == RefundStatus.REJECTED) {
            refund.getOrder().updateStatus(OrderStatus.DELIVERED);
        }
        return toRefundResponseDto(refund);
    }

    private void refundAsPoint(Refund refund) {
        Order order = refund.getOrder();

        // 비회원 주문은 포인트 환불 대상이 아님
        if (order.getUserId() == null) {
            return;
        }

        RefundAmountResult amountResult = calculateRefundAmount(refund);

        // 전체/부분 반품 구분은 필요하다면 여기서 로직 추가
        boolean allReturned = order.getOrderItems().stream()
                .allMatch(orderItem -> orderItem.getOrderItemStatus() == OrderItemStatus.RETURN_COMPLETED);

        RefundPointRequestDto dto = new RefundPointRequestDto();
        dto.setUserId(order.getUserId());
        dto.setOrderId(order.getOrderId());
        dto.setRefundAmount(amountResult.finalPointAmount); // 주의: 부분 반품이거나 쿠폰/포인트 사용이 있는 경우 이 값은 틀린 값이 됩니다.
//        dto.setRefundId(refund.getRefundId());
//        dto.setUsedPoint(0); // 지금은 "반품 시 사용 포인트는 복구하지 않는다" 전략(옵션3) 가정
//        dto.setReturnAmount(amountResult.getFinalPointAmount());

        pointServiceClient.refundPoint(dto);
    }

    // ======================================================
    // 내부 헬퍼 메서드들
    // ======================================================
    // 1) Order 로딩
    private Order loadOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    // 2) Refund 로딩
    private Refund loadRefund(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException("반품 내역을 찾을 수 없습니다. id=" + refundId));
    }

    // 3) 출고일 기준 경과일 계산
    private long getDaysAfterShipment(Order order) {
        Delivery delivery = deliveryRepository.findByOrder_OrderId(order.getOrderId())
                .orElseThrow(() -> new IllegalStateException("배송 정보가 존재하지 않습니다."));

        LocalDate baseDate = (delivery.getDeliveryStartAt() != null)
                ? delivery.getDeliveryStartAt().toLocalDate()
                : order.getOrderDateTime().toLocalDate();

        return ChronoUnit.DAYS.between(baseDate, LocalDate.now());
    }

    // 4) 반품 가능 여부 검증
    /**
     * - 주문 상태: DELIVERED / COMPLETED 에서만 가능
     * - 날짜/사유 정책:
     *   * 10일 이내: 사유 상관없이 가능
     *   * 10~30일: 파손/오배송(PRODUCT_DEFECT, WRONG_DELIVERY)만 가능
     *   * 30일 초과: 어떤 사유도 불가
     */
    private void validateRefundEligibility(Order order, RefundReason reason) {

        // 주문 상태 검증
        if (order.getOrderStatus() != OrderStatus.DELIVERED &&
                order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("배송 완료 또는 주문 완료 상태에서만 반품이 가능합니다. (현재 상태: " + order.getOrderStatus() + ")");
        }

        long days = getDaysAfterShipment(order);

        // 30일 초과: 어떤 사유도 불가
        if (days > 30) {
            throw new IllegalStateException("출고일 기준 30일이 경과하여 반품이 불가합니다.");
        }
        // 10일 초과 ~ 30일 이하: 파손/파본/오배송만 허용
        if (days > 10) {
            if (reason != RefundReason.PRODUCT_DEFECT && reason != RefundReason.WRONG_DELIVERY) {
                throw new IllegalStateException("단순 변심/기타 사유는 출고일 기준 10일 이내에만 반품 가능합니다.");
            }
        }
    }

    // 5) 반품 가능 수량 검증
    private void validateRefundQuantity(OrderItem orderItem, int requestQuantity) {
        int alreadyReturned = refundItemRepository.sumReturnedQuantityByOrderItemId(orderItem.getOrderItemId());
        int remainReturnable = orderItem.getOrderItemQuantity() - alreadyReturned;

        if (requestQuantity <= 0 || requestQuantity > remainReturnable) {
            throw new IllegalArgumentException(
                    "반품 가능 수량을 초과했습니다. 요청=" + requestQuantity + ", 가능=" + remainReturnable
            );
        }
    }

    // 6) 진행 중인 반품이 있는지 검증
    private void validateNoActiveRefund(OrderItem orderItem) {

        Collection<Integer> activeStatuses = List.of(
                RefundStatus.REQUESTED.getCode(),
                RefundStatus.COLLECTION_PENDING.getCode(),
                RefundStatus.IN_TRANSIT.getCode(),
                RefundStatus.IN_INSPECTION.getCode()
        );

        boolean hasActive =
                refundItemRepository.existsByOrderItemOrderItemIdAndRefundRefundStatusIn(
                        orderItem.getOrderItemId(),
                        activeStatuses
                );

        if (hasActive) {
            throw new IllegalStateException("이미 진행 중인 반품 건이 있는 주문 항목입니다. orderItemId=" + orderItem.getOrderItemId());
        }
    }

    // 7) 환불 완료 시 처리
    /**
     * - 재고 복구
     * - 주문항목 상태 RETURN_COMPLETED
     * - 주문 상태: 전체/부분 반품에 따라 RETURN_COMPLETED / PARTIAL_REFUND
     */
    private void handleRefundCompleted(Refund refund) {
        Order order = refund.getOrder();

        // 1) 재고 복구
        List<StockDecreaseRequest> restoreRequests = refund.getRefundItem().stream()
                .map(refundItem -> new StockDecreaseRequest(
                        refundItem.getOrderItem().getBookId(),
                        refundItem.getRefundQuantity()
                ))
                .collect(Collectors.toList());

        try {
            bookServiceClient.increaseStock(restoreRequests);
        } catch (FeignException e) {
            log.warn("재고 복구 호출 실패 refundId={} : {}", refund.getRefundId(), e.getMessage());
            // TODO 상황에 따라 롤백할지, 보류 상태로 둘지 정책 필요
            throw e;
        }

        // 2) 주문 항목 상태 업데이트
        for (RefundItem refundItem : refund.getRefundItem()) {
            OrderItem orderItem = refundItem.getOrderItem();
            orderItem.setOrderItemStatus(OrderItemStatus.RETURN_COMPLETED);
        }

        // 3) 전체 / 부분 반품에 따른 주문 상태 결정
        boolean allReturned = order.getOrderItems().stream()
                .allMatch(oi -> oi.getOrderItemStatus() == OrderItemStatus.RETURN_COMPLETED);

        boolean anyReturned = order.getOrderItems().stream()
                .anyMatch(oi -> oi.getOrderItemStatus() == OrderItemStatus.RETURN_COMPLETED);

        if (allReturned) {
            order.updateStatus(OrderStatus.RETURN_COMPLETED);
        } else if (anyReturned) {
            order.updateStatus(OrderStatus.PARTIAL_REFUND);
        }

        // 4) 포인트 적립/복구 로직 (user-service 연동)
        // TODO 전체 반품 vs 부분 반품에 따라 금액 계산 후 포인트 서비스 호출
        //  -> 별도 private RefundAmountResult calculateRefundAmount(...) + pointClient.refundPoint(...)
    }

    // 8) 반품된 항목들을 기준으로 "포인트로 지급할 환불 금액" 계산
    /**
     * - itemsAmount: 반품 상품 금액 합계 (unitPrice * refundQuantity)
     * - shippingFeeDeduction: 반품 택배비 (단순 예시: 5,000원)
     * - finalPointAmount: 실제로 포인트로 적립할 금액
     */
    private RefundAmountResult calculateRefundAmount(Refund refund) {

        Order order = refund.getOrder();

        int itemsAmount = refund.getRefundItem().stream()
                .mapToInt(refundItem -> refundItem.getOrderItem().getUnitPrice() * refundItem.getRefundQuantity())
                .sum();

        int shippingFeeDeduction = 0;

        RefundReason reason = RefundReason.valueOf(refund.getRefundReason());
//        RefundReason reason = parseRefundReason(refund.getRefundReason());
        long days = getDaysAfterShipment(order);

        if (days <= 10) {
            if (reason == RefundReason.CHANGE_OF_MIND || reason == RefundReason.OTHER) {
                shippingFeeDeduction = 5000; // 예시
            }
        } else {
            // 10~30일 구간: validateRefundEligibility 에서 이미 reason 검증을 했다고 가정 (파손/오배송만)
            shippingFeeDeduction = 0;
        }

        int finalPointAmount = Math.max(itemsAmount - shippingFeeDeduction, 0);

        return new RefundAmountResult(itemsAmount, shippingFeeDeduction, finalPointAmount);
    }

    // 9) Refund 엔티티를 화면 응답 DTO로 변환
    private RefundResponseDto toRefundResponseDto(Refund refund) {

        // 1) bookIds 수집
        List<Long> bookIds = refund.getRefundItem().stream()
                .map(refundItem -> refundItem.getOrderItem().getBookId())
                .collect(Collectors.toList());

        // 2) 도서 정보 조회
        Map<Long, BookOrderResponse> tempBookMap;
        try {
            List<BookOrderResponse> bookInfos = bookServiceClient.getBooksForOrder(bookIds);
            tempBookMap = bookInfos.stream()
                    .collect(Collectors.toMap(BookOrderResponse::getBookId, Function.identity()));
        } catch (FeignException e) {
            log.warn("도서 정보 조회 실패 (refundId={}): {}", refund.getRefundId(), e.getMessage());
            tempBookMap = Collections.emptyMap();
        }

        // 3) 람다에서 사용할 final map
        final Map<Long, BookOrderResponse> bookMap = tempBookMap;

        // 4) RefundItemDetailDto 리스트 구성
        List<RefundItemDetailDto> itemDetails = refund.getRefundItem().stream()
                .map(refundItem -> {
                    Long bookId = refundItem.getOrderItem().getBookId();
                    BookOrderResponse bookInfo = bookMap.get(bookId);
                    String title = (bookInfo != null) ? bookInfo.getTitle() : "";
                    return new RefundItemDetailDto(
                            refundItem.getRefundItemId(),
                            refundItem.getOrderItem().getOrderItemId(),
                            title,
                            refundItem.getRefundQuantity()
                    );
                })
                .collect(Collectors.toList());

        // 5) RefundResponseDto 생성
        return new RefundResponseDto(
                refund.getRefundId(),
                refund.getOrder().getOrderId(),
                refund.getRefundReason(),
                refund.getRefundReasonDetail(),
                RefundStatus.fromCode(refund.getRefundStatus()).getDescription(),
                refund.getRefundCreatedAt(),
                itemDetails
        );
    }

    // 10) 반품 금액 계산 결과를 담는 내부 DTO
    private static class RefundAmountResult {
        private final int itemsAmount;          // 반품 상품 금액 합계 (unitPrice * qty)
        private final int shippingFeeDeduction; // 고객이 부담하는 반품 배송비 (차감액)
        private final int finalPointAmount;     // 실제로 포인트로 적립할 금액

        public RefundAmountResult(int itemsAmount, int shippingFeeDeduction, int finalPointAmount) {
            this.itemsAmount = itemsAmount;
            this.shippingFeeDeduction = shippingFeeDeduction;
            this.finalPointAmount = finalPointAmount;
        }

        public int getItemsAmount() {
            return itemsAmount;
        }

        public int getShippingFeeDeduction() {
            return shippingFeeDeduction;
        }

        public int getFinalPointAmount() {
            return finalPointAmount;
        }
    }

}