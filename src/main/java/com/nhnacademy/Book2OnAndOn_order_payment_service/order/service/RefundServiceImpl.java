package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.PointServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.StockDecreaseRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundAvailableItemDto;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final BookServiceClient bookServiceClient;
    private final UserServiceClient userServiceClient;
    private final PointServiceClient pointServiceClient;

    private RefundReason parseRefundReason(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("refundReason는 필수입니다.");
        }
        try {
            return RefundReason.valueOf(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 refundReason: " + raw);
        }
    }

    // =========================
    // 1. 회원
    // =========================
    @Override
    public RefundResponseDto createRefundForMember(Long orderId, Long userId, RefundRequestDto request) {
        Order order = loadOrder(orderId);

        // 1) 본인 주문인지 검증 (refund 변수 쓰면 안 됨: refund는 아직 없음)
        Long ownerId = order.getUserId();
        if (ownerId == null || !ownerId.equals(userId)) {
            throw new AccessDeniedException("본인의 주문에 대해서만 반품 신청이 가능합니다.");
        }

        // 2) 반품 사유 파싱 + 정책 검증
        RefundReason reason = parseRefundReason(request.getRefundReason());
        validateRefundEligibility(order, reason);

        // 3) 반품 아이템 필수
        List<RefundItemRequestDto> itemRequests = request.getRefundItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("반품할 항목이 없습니다.");
        }

        // 4) Refund 생성/저장
        Refund refund = new Refund();
        refund.setRefundReason(request.getRefundReason());
        refund.setRefundReasonDetail(request.getRefundReasonDetail());
        refund.setRefundStatus(RefundStatus.REQUESTED.getCode());
        refund.setRefundCreatedAt(LocalDateTime.now());
        refund.setOrder(order);
        refundRepository.save(refund);

        // (양방향 컬렉션이 null일 수 있는 프로젝트를 대비한 최소 널가드)
        try {
            if (order.getRefunds() != null) {
                order.getRefunds().add(refund);
            }
        } catch (Exception ignore) {
            // order 쪽 컬렉션/매핑이 없거나 LAZY 초기화 문제 등 -> 저장 자체엔 영향 없으므로 무시
        }

        // 5) RefundItem 생성/저장
        for (RefundItemRequestDto dto : itemRequests) {
            OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "주문 항목을 찾을 수 없습니다. orderItemId=" + dto.getOrderItemId()));

            // “해당 주문 소속” 검증 (중요: 다른 주문 itemId로 반품 신청하는 버그 방어)
            if (orderItem.getOrder() == null || !orderId.equals(orderItem.getOrder().getOrderId())) {
                throw new IllegalArgumentException(
                        "주문에 속하지 않는 orderItemId 입니다. orderItemId=" + dto.getOrderItemId());
            }

            validateRefundQuantity(orderItem, dto.getRefundQuantity());
            validateNoActiveRefund(orderItem);

            RefundItem refundItem = new RefundItem();
            refundItem.setRefundQuantity(dto.getRefundQuantity());
            refundItem.setRefund(refund);
            refundItem.setOrderItem(orderItem);

            refundItemRepository.save(refundItem);
            refund.getRefundItem().add(refundItem);
        }

        // 6) 주문 상태 변경
        order.updateStatus(OrderStatus.RETURN_REQUESTED);

        return toRefundResponseDto(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundDetailsForMember(Long userId, Long refundId, Long pathOrderId) {
        Refund refund = loadRefund(refundId);

        Long ownerId = refund.getOrder() != null ? refund.getOrder().getUserId() : null;
        if (ownerId == null || !ownerId.equals(userId)) {
            throw new AccessDeniedException("해당 반품 내역에 대한 접근 권한이 없습니다.");
        }

        if (refund.getOrder() == null || !pathOrderId.equals(refund.getOrder().getOrderId())) {
            throw new IllegalArgumentException("orderId가 반품 내역의 주문과 일치하지 않습니다.");
        }

        return toRefundResponseDto(refund);
    }

    @Override
    public Page<RefundResponseDto> getRefundsForMember(Long userId, Pageable pageable) {
        Page<Refund> page = refundRepository.findByOrderUserId(userId, pageable);
        return page.map(this::toRefundResponseDto);
    }

    // =========================
    // 2. 비회원
    // =========================
    @Override
    public RefundResponseDto createRefundForGuest(Long orderId, RefundRequestDto request) {
        Order order = loadOrder(orderId);

        // 비회원 주문 검증
        if (order.getUserId() != null) {
            throw new IllegalStateException("회원 주문입니다. 비회원 반품 API를 잘못 호출했습니다.");
        }
        if (order.getGuestOrder() == null) {
            throw new IllegalStateException("비회원 주문 정보가 존재하지 않습니다.");
        }

        RefundReason reason = parseRefundReason(request.getRefundReason());
        validateRefundEligibility(order, reason);

        List<RefundItemRequestDto> itemRequests = request.getRefundItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("반품할 항목이 없습니다.");
        }

        Refund refund = new Refund();
        refund.setRefundReason(request.getRefundReason());
        refund.setRefundReasonDetail(request.getRefundReasonDetail());
        refund.setRefundStatus(RefundStatus.REQUESTED.getCode());
        refund.setRefundCreatedAt(LocalDateTime.now());
        refund.setOrder(order);
        refundRepository.save(refund);

        try {
            if (order.getRefunds() != null) {
                order.getRefunds().add(refund);
            }
        } catch (Exception ignore) {}

        for (RefundItemRequestDto dto : itemRequests) {
            OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "주문 항목을 찾을 수 없습니다. orderItemId=" + dto.getOrderItemId()));

            if (orderItem.getOrder() == null || !orderId.equals(orderItem.getOrder().getOrderId())) {
                throw new IllegalArgumentException(
                        "주문에 속하지 않는 orderItemId 입니다. orderItemId=" + dto.getOrderItemId());
            }

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

    // 오늘 배포 목표: 기능 미구현이어도 “컴파일/기동”만 보장
    @Override
    public List<RefundAvailableItemDto> getRefundableItemsForMember(Long orderId, Long userId) {
        return List.of();
    }

    @Override
    public List<RefundAvailableItemDto> getRefundableItemsForGuest(Long orderId) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundDetailsForGuest(Long orderId, Long refundId) {
        Refund refund = loadRefund(refundId);

        if (refund.getOrder() == null || refund.getOrder().getUserId() != null) {
            throw new IllegalStateException("회원 주문의 반품입니다. 비회원 조회 API를 잘못 호출했습니다.");
        }
        if (refund.getOrder().getGuestOrder() == null) {
            throw new IllegalStateException("비회원 주문 정보가 존재하지 않습니다.");
        }
        if (!orderId.equals(refund.getOrder().getOrderId())) {
            throw new IllegalArgumentException("orderId가 반품 내역의 주문과 일치하지 않습니다.");
        }

        return toRefundResponseDto(refund);
    }

    // =========================
    // 3. 관리자
    // =========================
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
        LocalDateTime from = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime to = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        Long effectiveUserId = userId;
        if (effectiveUserId == null && userKeyword != null && !userKeyword.isBlank()) {
            List<Long> ids = userServiceClient.searchUserIdsByKeyword(userKeyword.trim());
            if (ids.size() == 1) {
                effectiveUserId = ids.get(0);
            }
        }

        String normalizedOrderNumber = (orderNumber != null && !orderNumber.isBlank())
                ? orderNumber.trim()
                : null;

        Page<Refund> refundPage = refundRepository.searchRefunds(
                refundStatus,
                from,
                to,
                effectiveUserId,
                normalizedOrderNumber,
                includeGuest,
                pageable
        );

        return refundPage.map(this::toRefundResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundDetailsForAdmin(Long refundId) {
        Refund refund = loadRefund(refundId);
        return toRefundResponseDto(refund);
    }

    private static final Map<RefundStatus, List<RefundStatus>> ALLOWED_TRANSITIONS = Map.of(
            RefundStatus.REQUESTED, List.of(RefundStatus.COLLECTION_PENDING, RefundStatus.REJECTED),
            RefundStatus.COLLECTION_PENDING, List.of(RefundStatus.IN_TRANSIT, RefundStatus.REJECTED),
            RefundStatus.IN_TRANSIT, List.of(RefundStatus.IN_INSPECTION, RefundStatus.REJECTED),
            RefundStatus.IN_INSPECTION, List.of(RefundStatus.INSPECTION_COMPLETED, RefundStatus.REJECTED),
            RefundStatus.INSPECTION_COMPLETED, List.of(RefundStatus.REFUND_COMPLETED, RefundStatus.REJECTED),
            RefundStatus.REFUND_COMPLETED, List.of(),
            RefundStatus.REJECTED, List.of()
    );

    @Override
    public RefundResponseDto updateRefundStatus(Long refundId, RefundStatusUpdateDto request) {
        // 동시성 방어(있다면): PESSIMISTIC_WRITE
        Refund refund = refundRepository.findByIdForUpdate(refundId);
        if (refund == null) {
            throw new RefundNotFoundException("반품 내역을 찾을 수 없습니다. id=" + refundId);
        }

        RefundStatus current = RefundStatus.fromCode(refund.getRefundStatus());
        RefundStatus next = RefundStatus.fromCode(request.getStatusCode());

        if (current == RefundStatus.REFUND_COMPLETED || current == RefundStatus.REJECTED) {
            throw new IllegalStateException("이미 종결된 반품 건은 상태 변경이 불가합니다. current=" + current);
        }

        List<RefundStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, List.of());
        if (!allowed.contains(next)) {
            throw new IllegalStateException("허용되지 않은 상태 전이입니다. " + current + " -> " + next);
        }

        refund.setRefundStatus(next.getCode());

        if (next == RefundStatus.REFUND_COMPLETED) {
            handleRefundCompleted(refund);
            refundAsPoint(refund);
        } else if (next == RefundStatus.REJECTED) {
            if (refund.getOrder() != null) {
                refund.getOrder().updateStatus(OrderStatus.DELIVERED);
            }
        }

        return toRefundResponseDto(refund);
    }

    private void refundAsPoint(Refund refund) {
        Order order = refund.getOrder();
        if (order == null) return;

        if (order.getUserId() == null) return; // 비회원 포인트 환불 없음

        RefundAmountResult amount = calculateRefundAmount(refund);

        boolean allReturned = order.getOrderItems().stream()
                .allMatch(oi -> oi.getOrderItemStatus() == OrderItemStatus.RETURN_COMPLETED);

        int usedPoint = extractUsedPoint(order);

        int pointToCredit;
        int pointToRestore;

        if (allReturned) {
            pointToCredit = amount.getFinalPointAmount();
            pointToRestore = usedPoint;
        } else {
            pointToCredit = amount.getFinalPointAmount();
            pointToRestore = 0;
        }

        RefundPointRequestDto dto = new RefundPointRequestDto(
                order.getUserId(), order.getOrderId(), pointToCredit + pointToRestore);

        pointServiceClient.refundPoint(dto);
    }

    private int extractUsedPoint(Order order) {
        return order.getPointDiscount();
    }

    // ======================================================
    // 내부 헬퍼
    // ======================================================
    private Order loadOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Refund loadRefund(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException("반품 내역을 찾을 수 없습니다. id=" + refundId));
    }

    private long getDaysAfterShipment(Order order) {
        Delivery delivery = deliveryRepository.findByOrder_OrderId(order.getOrderId())
                .orElseThrow(() -> new IllegalStateException("배송 정보가 존재하지 않습니다."));

        LocalDate baseDate = (delivery.getDeliveryStartAt() != null)
                ? delivery.getDeliveryStartAt().toLocalDate()
                : order.getOrderDateTime().toLocalDate();

        return ChronoUnit.DAYS.between(baseDate, LocalDate.now());
    }

    private void validateRefundEligibility(Order order, RefundReason reason) {
        if (order.getOrderStatus() != OrderStatus.DELIVERED &&
                order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("배송 완료 또는 주문 완료 상태에서만 반품이 가능합니다. (현재 상태: " + order.getOrderStatus() + ")");
        }

        long days = getDaysAfterShipment(order);

        if (days > 30) {
            throw new IllegalStateException("출고일 기준 30일이 경과하여 반품이 불가합니다.");
        }

        if (days > 10) {
            if (reason != RefundReason.PRODUCT_DEFECT && reason != RefundReason.WRONG_DELIVERY) {
                throw new IllegalStateException("단순 변심/기타 사유는 출고일 기준 10일 이내에만 반품 가능합니다.");
            }
        }
    }

    private void validateRefundQuantity(OrderItem orderItem, int requestQuantity) {
        int alreadyReturned = refundItemRepository.sumReturnedQuantityByOrderItemId(orderItem.getOrderItemId());
        int remainReturnable = orderItem.getOrderItemQuantity() - alreadyReturned;

        if (requestQuantity <= 0 || requestQuantity > remainReturnable) {
            throw new IllegalArgumentException(
                    "반품 가능 수량을 초과했습니다. 요청=" + requestQuantity + ", 가능=" + remainReturnable
            );
        }
    }

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

    private void handleRefundCompleted(Refund refund) {
        Order order = refund.getOrder();
        if (order == null) return;

        List<StockDecreaseRequest> restoreRequests = refund.getRefundItem().stream()
                .map(ri -> new StockDecreaseRequest(
                        ri.getOrderItem().getBookId(),
                        ri.getRefundQuantity()
                ))
                .collect(Collectors.toList());

        try {
            bookServiceClient.increaseStock(restoreRequests);
        } catch (FeignException e) {
            log.warn("재고 복구 호출 실패 refundId={} : {}", refund.getRefundId(), e.getMessage());
            throw e; // 오늘 목표가 “에러 안나게”라도, 실패를 숨기면 더 큰 데이터 불일치가 남음
        }

        for (RefundItem ri : refund.getRefundItem()) {
            OrderItem oi = ri.getOrderItem();
            oi.setOrderItemStatus(OrderItemStatus.RETURN_COMPLETED);
        }

        boolean allReturned = order.getOrderItems().stream()
                .allMatch(oi -> oi.getOrderItemStatus() == OrderItemStatus.RETURN_COMPLETED);

        boolean anyReturned = order.getOrderItems().stream()
                .anyMatch(oi -> oi.getOrderItemStatus() == OrderItemStatus.RETURN_COMPLETED);

        if (allReturned) {
            order.updateStatus(OrderStatus.RETURN_COMPLETED);
        } else if (anyReturned) {
            order.updateStatus(OrderStatus.PARTIAL_REFUND);
        }
    }

    private RefundAmountResult calculateRefundAmount(Refund refund) {
        Order order = refund.getOrder();

        int itemsAmount = refund.getRefundItem().stream()
                .mapToInt(ri -> ri.getOrderItem().getUnitPrice() * ri.getRefundQuantity())
                .sum();

        int shippingFeeDeduction = 0;

        RefundReason reason = parseRefundReason(refund.getRefundReason());
        long days = (order != null) ? getDaysAfterShipment(order) : 0;

        if (days <= 10) {
            if (reason == RefundReason.CHANGE_OF_MIND || reason == RefundReason.OTHER) {
                shippingFeeDeduction = 5000;
            }
        } else {
            shippingFeeDeduction = 0;
        }

        int finalPointAmount = Math.max(itemsAmount - shippingFeeDeduction, 0);
        return new RefundAmountResult(itemsAmount, shippingFeeDeduction, finalPointAmount);
    }

    private RefundResponseDto toRefundResponseDto(Refund refund) {
        List<Long> bookIds = refund.getRefundItem().stream()
                .map(ri -> ri.getOrderItem().getBookId())
                .distinct()
                .toList();

        Map<Long, BookOrderResponse> bookMap = Collections.emptyMap();
        if (!bookIds.isEmpty()) {
            try {
                bookMap = bookServiceClient.getBooksForOrder(bookIds).stream()
                        .collect(Collectors.toMap(BookOrderResponse::getBookId, Function.identity()));
            } catch (FeignException e) {
                log.warn("도서 정보 조회 실패 (refundId={}): {}", refund.getRefundId(), e.getMessage());
            }
        }

        Map<Long, BookOrderResponse> finalBookMap = bookMap;
        List<RefundItemDetailDto> itemDetails = refund.getRefundItem().stream()
                .map(ri -> {
                    Long bookId = ri.getOrderItem().getBookId();
                    BookOrderResponse info = finalBookMap.get(bookId);
                    String title = (info != null) ? info.getTitle() : "";
                    return new RefundItemDetailDto(
                            ri.getRefundItemId(),
                            ri.getOrderItem().getOrderItemId(),
                            title,
                            ri.getRefundQuantity()
                    );
                })
                .collect(Collectors.toList());

        return new RefundResponseDto(
                refund.getRefundId(),
                refund.getOrder() != null ? refund.getOrder().getOrderId() : null,
                refund.getRefundReason(),
                refund.getRefundReasonDetail(),
                RefundStatus.fromCode(refund.getRefundStatus()).getDescription(),
                refund.getRefundCreatedAt(),
                itemDetails
        );
    }

    private static class RefundAmountResult {
        private final int itemsAmount;
        private final int shippingFeeDeduction;
        private final int finalPointAmount;

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
