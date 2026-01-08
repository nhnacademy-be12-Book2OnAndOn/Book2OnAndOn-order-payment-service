package com.nhnacademy.book2onandon_order_payment_service.order.service.impl;

import com.nhnacademy.book2onandon_order_payment_service.client.BookServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.UserServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.RefundPointInternalRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.StockDecreaseRequest;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.RefundCompletedEvent;
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
import com.nhnacademy.book2onandon_order_payment_service.order.exception.RefundOrderMismatchException;
import com.nhnacademy.book2onandon_order_payment_service.order.provider.GuestTokenProvider;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.refund.RefundItemRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.refund.RefundRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.RefundService;
import feign.FeignException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private final ApplicationEventPublisher applicationEventPublisher;
    private static final int DEFAULT_RETURN_SHIPPING_FEE = 3000;

    private final GuestTokenProvider guestTokenProvider;

    /**
     * “진행 중(Active)” 반품 상태 목록
     * - 이 상태들은 "이미 반품 프로세스에 올라탄 수량"으로 간주하여,
     *   다시 반품 신청할 때 returnableQuantity 계산에서 차감하는 용도로 사용.
     */
    private static final List<RefundStatus> ACTIVE_REFUND_STATUSES =
            List.of(RefundStatus.REQUESTED,
                    RefundStatus.IN_INSPECTION,
                    RefundStatus.APPROVED);

    /**
     * 취소 가능 상태(정책)
     * - 자동 ACCEPT가 IN_INSPECTION으로 들어가므로, 최소한 IN_INSPECTION까지는 취소 허용
     * - APPROVED 이후 취소 허용은 정책 논쟁이 크므로 제외(원하면 추가)
     */
    private static final EnumSet<RefundStatus> CANCELABLE_STATUSES =
            EnumSet.of(RefundStatus.REQUESTED,
                    RefundStatus.IN_INSPECTION);

    /**
     * 관리자 상태 전이 규칙
     */
    private static final Map<RefundStatus, List<RefundStatus>> ALLOWED_TRANSITIONS = Map.of(
            RefundStatus.REQUESTED, List.of(RefundStatus.IN_INSPECTION, RefundStatus.REJECTED, RefundStatus.REQUEST_CANCELED),
            RefundStatus.IN_INSPECTION, List.of(RefundStatus.APPROVED, RefundStatus.REJECTED),
            RefundStatus.APPROVED, List.of(RefundStatus.REFUND_COMPLETED, RefundStatus.REJECTED),
            RefundStatus.REFUND_COMPLETED, List.of(),
            RefundStatus.REJECTED, List.of(),
            RefundStatus.REQUEST_CANCELED, List.of()
    );

    /**
     * 추가반품 허용을 위해 “반품 신청/폼 진입”에 허용할 주문 상태 집합
     * - 1차 신청 후 orderStatus가 RETURN_REQUESTED가 되더라도 2차 신청 폼 진입이 막히지 않게 함
     * - 실제 반품 가능 여부는 returnableQuantity(주문수량-완료-진행중)로 최종 방어
     */
    private static final EnumSet<OrderStatus> REFUND_FLOW_ALLOWED_ORDER_STATUSES =
            EnumSet.of(OrderStatus.DELIVERED,
                    OrderStatus.COMPLETED,
                    OrderStatus.RETURN_REQUESTED,
                    OrderStatus.PARTIAL_REFUND);



    // =========================
    // 0. 공통 유틸
    // =========================
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

    /**
     * 요청 refundItems를 (orderItemId -> quantity 합)으로 집계한다.
     * - 같은 orderItemId가 중복으로 들어오는 케이스에서 수량 검증이 뚫리는 문제를 방지한다.
     */
    private Map<Long, Integer> aggregateRequestedQuantities(List<RefundItemRequestDto> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("반품 항목이 비어있습니다.");
        }

        Map<Long, Integer> map = items.stream()
                .collect(Collectors.toMap(
                        RefundItemRequestDto::getOrderItemId,
                        RefundItemRequestDto::getRefundQuantity,
                        Integer::sum
                ));

        // 유효성(0 이하) 방어
        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("orderItemId가 null인 반품 항목이 존재합니다.");
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("반품 수량은 1 이상이어야 합니다. orderItemId=" + entry.getKey());
            }
        }
        return map;
    }


    // =========================
    // 1. 회원 전용
    // =========================
    @Override
    @Transactional(readOnly = true)
    public Page<RefundResponseDto> getRefundsForMember(Long userId, Pageable pageable) {
        Page<Refund> page = refundRepository.findByOrderUserId(userId, pageable);

        List<Long> bookIds = page.stream()
                .flatMap(refund -> refund.getRefundItems().stream())
                .map(ri -> ri.getOrderItem().getBookId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> titleMap = fetchBookTitleMap(bookIds);

        return page.map(refund -> RefundResponseDto.from(refund, titleMap));
    }


    // 회원/비회원 공용
    @Override
    public RefundResponseDto createRefund(Long orderId, Long userId, RefundRequestDto request, String guestToken) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        validateOrderAuthority(order, userId, guestToken);

        RefundReason reason = parseRefundReason(request.getRefundReason());
        validateRefundEligibility(order, reason);

        Map<Long, Integer> requestedQtyMap = aggregateRequestedQuantities(request.getRefundItems());
        List<Long> itemIds = requestedQtyMap.keySet().stream().toList();

        long days = getDaysAfterShipment(order);
        RefundStatus initialStatus = determineInitialStatus(reason, days);

        Refund refund = Refund.create(order, reason, request.getRefundReasonDetail());
        refund.setRefundStatus(initialStatus);

        // 원래 주문상태 저장 (RETURN_REQUESTED로 바꾸기 전에)
        if (order.getOrderStatus() != null) {
            refund.setOriginalOrderStatus(order.getOrderStatus().getCode());
        }

        order.addRefund(refund);

        // 자동 REJECT는 "반품 프로세스에 올라탄 것"이 아니므로 주문/아이템 상태를 건드리지 않는다.
        if (initialStatus == RefundStatus.REJECTED) {
            refundRepository.save(refund);
            return toRefundResponseDto(refund);
        }

        // 주문 소속 + 락(한 번에)
        List<OrderItem> lockedItems = orderItemRepository.findByOrderIdAndItemIdsForUpdate(orderId, itemIds);
        Map<Long, OrderItem> itemMap = lockedItems.stream()
                .collect(Collectors.toMap(OrderItem::getOrderItemId, oi -> oi));

        for (Long id : itemIds) {
            if (!itemMap.containsKey(id)) {
                throw new IllegalArgumentException("주문에 속하지 않는 orderItemId 입니다. orderItemId=" + id);
            }
        }

        // 완료/진행중 집계(주문 단위 1회)
        ReturnableSnapshot snap = loadReturnableSnapshot(orderId);

        // 검증 + RefundItem 생성(집계된 수량 기준으로 1개씩 생성)
        for (Map.Entry<Long, Integer> entry : requestedQtyMap.entrySet()) {
            Long orderItemId = entry.getKey();
            int reqQty = entry.getValue();

            OrderItem oi = itemMap.get(orderItemId);
            int ordered = oi.getOrderItemQuantity() == null ? 0 : oi.getOrderItemQuantity();
            int completed = snap.completedMap.getOrDefault(orderItemId, 0);
            int active = snap.activeMap.getOrDefault(orderItemId, 0);

            int remain = ordered - completed - active;
            if (reqQty > remain) {
                throw new IllegalArgumentException("반품 가능 수량 초과. 요청=" + reqQty + ", 가능=" + remain + ", orderItemId=" + orderItemId);
            }

            // 정책: 한 item에 진행중 반품 1건만 허용
            if (active > 0) {
                throw new IllegalStateException("이미 진행 중인 반품 건이 있는 주문 항목입니다. orderItemId=" + orderItemId);
            }

            RefundItem refundItem = RefundItem.create(refund, oi, reqQty);
            refund.addRefundItem(refundItem);


            oi.setOrderItemStatus(OrderItemStatus.RETURN_REQUESTED);
        }

        order.updateStatus(OrderStatus.RETURN_REQUESTED);
        refundRepository.save(refund);

        return toRefundResponseDto(refund);
    }


    @Override
    public RefundResponseDto cancelRefund(Long orderId, Long refundId, Long userId, String guestToken) {
        Refund refund = refundRepository.findByIdForUpdate(refundId);
        if (refund == null) {
            throw new RefundNotFoundException(refundId);
        }

        Order order = refund.getOrder();
        if (order == null || !orderId.equals(order.getOrderId())) {
            throw new RefundOrderMismatchException(orderId, refundId,
                    (order != null ? order.getOrderId() : null));
        }

        validateOrderAuthority(order, userId, guestToken);

        RefundStatus status = refund.getRefundStatus();
        if (!CANCELABLE_STATUSES.contains(status)) {
            throw new RefundNotCancelableException(refundId, status, CANCELABLE_STATUSES);
        }

        refund.setRefundStatus(RefundStatus.REQUEST_CANCELED);

        // 주문아이템 상태 롤백
        rollbackOrderItemStatus(refund);

        // 주문 상태 롤백(다른 진행중 반품 존재 여부에 따라)
        boolean hasOtherActiveRefund = refundRepository.existsActiveRefundByOrderIdExcludingRefundId(
                orderId, refundId, ACTIVE_REFUND_STATUSES
        );
        restoreOrderStatusAfterRollback(order, refund, hasOtherActiveRefund);

        return toRefundResponseDto(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundDetails(Long orderId, Long refundId, Long userId, String guestToken) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        Order order = refund.getOrder();

        if(order == null || !orderId.equals(order.getOrderId())) {
            throw new IllegalArgumentException("주문 정보가 일치하지 않습니다.");
        }

        validateOrderAuthority(order, userId, guestToken);

        return toRefundResponseDto(refund);
    }

    @Override
    public List<RefundAvailableItemResponseDto> getRefundableItems(Long orderId, Long userId, String guestToken) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        validateOrderAuthority(order, userId, guestToken);
        validateOrderStatusForRefundForm(order);

        return buildRefundableItems(order);
    }

    // =========================
    // 3. 관리자
    // =========================
    @Override
    public RefundResponseDto updateRefundStatus(Long refundId, RefundStatusUpdateRequestDto request) {
        Refund refund = refundRepository.findByIdForUpdate(refundId);
        if (refund == null) {
            throw new RefundNotFoundException("반품 내역을 찾을 수 없습니다. id=" + refundId);
        }

        RefundStatus current = refund.getRefundStatus();
        RefundStatus next = RefundStatus.fromCode(request.getStatusCode());

        // 종결 상태면 변경 불가
        if (current == RefundStatus.REFUND_COMPLETED) {
            return toRefundResponseDto(refund);
        }

        List<RefundStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, List.of());
        if (!allowed.contains(next)) {
            throw new IllegalStateException("허용되지 않은 상태 전이입니다. " + current + " -> " + next);
        }

        refund.setRefundStatus(next);

        // REJECT / CANCELED 시: 주문/아이템 상태 롤백(요구사항/UX 정합성)
        if (next == RefundStatus.REJECTED || next == RefundStatus.REQUEST_CANCELED) {
            rollbackOrderItemStatus(refund);

            Order order = refund.getOrder();
            if (order != null) {
                boolean hasOtherActiveRefund =
                        refundRepository.existsActiveRefundByOrderIdExcludingRefundId(
                                order.getOrderId(), refund.getRefundId(), ACTIVE_REFUND_STATUSES
                        );
                restoreOrderStatusAfterRollback(order, refund, hasOtherActiveRefund);
            }
            return toRefundResponseDto(refund);
        }

        if (next == RefundStatus.REFUND_COMPLETED) {
            // 재고 복구 + 주문/아이템 상태 변경(실패 시 롤백)
            handleRefundCompleted(refund);

            // AFTER_COMMIT에서 포인트 환불(외부 호출)
            // -> 포인트 환불 계산에 필요한 배송비 차감액을 "완료 시점에 확정/저장"
            RefundCalcResult calc = calculateRefundPointAmountProRate(refund);
            refund.setShippingDeductionAmount(calc.shippingDeduction());

            applicationEventPublisher.publishEvent(new RefundCompletedEvent(refund.getRefundId()));
        }

        return toRefundResponseDto(refund);
    }

    @Override
    @Transactional(readOnly = true) //읽기전용으로 전환 (이렇게 하면 DB 변경감지(dirty checking)을 안하기 때문에 성능이 빨라짐 굿굿!
    public Page<RefundResponseDto> getRefundListForAdmin(RefundSearchCondition condition, Pageable pageable) {
        //검색 조건 준비
        //1. 날짜 조건 변환
        LocalDateTime from = (condition.getStartDate() != null) ? condition.getStartDate().atStartOfDay() : null;
        LocalDateTime to = (condition.getEndDate() != null) ? condition.getEndDate().plusDays(1).atStartOfDay() : null;

        //2. 유저 검색 조건 처리
        List<Long> userIds = null;

        //2.1 userId 하나가 직접 들어온 경우는 우선순위 높음 처리해야함
        if(condition.getUserId() != null){
            userIds = List.of(condition.getUserId());
        }else if(StringUtils.hasText(condition.getUserKeyword())){ //2.2 userKeyword(이름/이메일 등)이 들어온 경우 Feign Client호출
            userIds = userServiceClient.searchUserIdsByKeyword(condition.getUserKeyword());
            if(userIds == null || userIds.isEmpty()){ //키워드로 검색했는데 유저가 한 명도 없으면 -> 결과도 없어야함 (DB 조회 불필요)
                return Page.empty(pageable);
            }
        }
        // 주의: orderNumber 부분일치(like)는 repository query에서 구현되어야 함
        // 3. Repository 호출
        Page<Refund> refundPage = refundRepository.searchRefunds(
                condition.getRefundStatusEnum(),
                from,
                to,
                userIds,  //List<Long> 으로 변경
                condition.getOrderNumber(),
                condition.isIncludeGuest(),
                pageable
        );

        List<Long> bookIds = refundPage.stream()
                .flatMap(refund -> refund.getRefundItems().stream())
                .map(ri -> ri.getOrderItem().getBookId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> titleMap = fetchBookTitleMap(bookIds);

        return refundPage.map(refund -> RefundResponseDto.from(refund, titleMap));
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundDetailsForAdmin(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException("반품 내역을 찾을 수 없습니다. id=" + refundId));
        return toRefundResponseDto(refund);
    }


    // =========================
    // 내부: form 구성
    // =========================
    private List<RefundAvailableItemResponseDto> buildRefundableItems(Order order) {
        List<OrderItem> items = order.getOrderItems();
        if (items == null || items.isEmpty()) return List.of();

        Long orderId = order.getOrderId();
        ReturnableSnapshot snapshot = loadReturnableSnapshot(orderId);

        List<Long> bookIds = items.stream()
                .map(OrderItem::getBookId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> titleMap = fetchBookTitleMap(bookIds);

        return items.stream().map(orderItem -> {
            int orderedQuantity = (orderItem.getOrderItemQuantity() == null) ? 0 : orderItem.getOrderItemQuantity();

            int completedReturn = snapshot.completedMap.getOrDefault(orderItem.getOrderItemId(), 0);
            int activeReturn = snapshot.activeMap.getOrDefault(orderItem.getOrderItemId(), 0);

            int returnable = Math.max(orderedQuantity - completedReturn - activeReturn, 0);
            // 진행중 반품이 있으면 disable
            boolean activeRefundExists = activeReturn > 0;

            // 안전장치: 주문아이템 상태가 RETURN_REQUESTED면 disable
            if (orderItem.getOrderItemStatus() == OrderItemStatus.RETURN_REQUESTED) {
                activeRefundExists = true;
            }

            boolean refundable = returnable > 0 && isRefundableByStatusPolicy(order);
            int unitPrice = (orderItem.getUnitPrice() == null) ? 0 : orderItem.getUnitPrice();

            return new RefundAvailableItemResponseDto(
                    orderItem.getOrderItemId(),
                    orderItem.getBookId(),
                    titleMap.getOrDefault(orderItem.getBookId(), ""),
                    orderedQuantity,
                    completedReturn,
                    returnable,
                    activeRefundExists,
                    refundable,
                    unitPrice
            );
        }).toList();
    }

    private Map<Long, String> fetchBookTitleMap(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) return Collections.emptyMap();

        try {
            List<BookOrderResponse> responses = bookServiceClient.getBooksForOrder(bookIds);
            if (responses == null) return Collections.emptyMap();

            return responses.stream()
                    .filter(Objects::nonNull)
                    .filter(r -> r.getBookId() != null)
                    .collect(Collectors.toMap(
                            BookOrderResponse::getBookId,
                            r -> r.getTitle() != null ? r.getTitle() : "",
                            (a, b) -> a
                    ));
        } catch (FeignException e) {
            log.warn("book-service 제목 조회 실패: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 폼/버튼 노출 및 최소 정책:
     * - 주문 상태는 DELIVERED/COMPLETED 뿐 아니라, 1차 반품 이후 RETURN_REQUESTED/PARTIAL_REFUND도 허용
     * - 기간은 30일 이내
     */
    private boolean isRefundableByStatusPolicy(Order order) {
        if (order == null) {
            return false;
        }
        if (!REFUND_FLOW_ALLOWED_ORDER_STATUSES.contains(order.getOrderStatus())) {
            return false;
        }
        return getDaysAfterShipment(order) <= 30;
    }

    private void validateOrderStatusForRefundForm(Order order) {
        if (!isRefundableByStatusPolicy(order)) {
            throw new IllegalStateException("반품 가능 상태가 아닙니다.");
        }
    }


    // ======================================================
    // 내부 헬퍼
    // ======================================================
    private long getDaysAfterShipment(Order order) {
        Delivery delivery = deliveryRepository.findByOrder_OrderId(order.getOrderId())
                .orElseThrow(() -> new IllegalStateException("배송 정보가 존재하지 않습니다."));

        LocalDate baseDate = (delivery.getDeliveryStartAt() != null)
                ? delivery.getDeliveryStartAt().toLocalDate()
                : order.getOrderDateTime().toLocalDate();

        return ChronoUnit.DAYS.between(baseDate, LocalDate.now());
    }

    /**
     * 반품 가능 정책 검증(요구사항 반영)
     * - 주문 상태: REFUND_FLOW_ALLOWED_ORDER_STATUSES 내에서 허용 (추가반품 폼/신청 막힘 방지)
     * - 기간: 30일 이내
     * - 10일 이내 단순변심/기타: 미사용만 허용(미사용 판단은 시스템 신뢰도/정책에 의존)
     *
     * 주의:
     * - “특정 아이템이 현재 RETURN_REQUESTED라서 전체를 막아버리는” 식의 검증은 하지 않는다.
     *   (아이템 단위 가능수량/active 여부는 createRefund에서 snapshot으로 방어)
     */
    private void validateRefundEligibility(Order order, RefundReason reason) {
        if (order == null) throw new IllegalStateException("주문이 존재하지 않습니다.");

        if (!REFUND_FLOW_ALLOWED_ORDER_STATUSES.contains(order.getOrderStatus())) {
            throw new IllegalStateException("현재 주문 상태에서는 반품 신청이 불가합니다. (현재 상태: " + order.getOrderStatus() + ")");
        }

        long days = getDaysAfterShipment(order);
        if (days > 30) {
            throw new IllegalStateException("출고일 기준 30일이 경과하여 반품이 불가합니다.");
        }

        // 단순변심/기타는 10일 이내만 가능한데, 자동 REJECT 요구사항 때문에 "신청 자체를 막지"는 않는다? 무슨 말?
        if (days <= 10 && (reason == RefundReason.CHANGE_OF_MIND || reason == RefundReason.OTHER)) {
            boolean usedItemExists = order.getOrderItems().stream()
                    .anyMatch(oi -> oi.getOrderItemStatus() == OrderItemStatus.USED);

            if (usedItemExists) {
                throw new IllegalStateException("단순 변심 반품은 미사용 상품에 한해 가능합니다.");
            }
        }
    }

    private RefundStatus determineInitialStatus(RefundReason reason, long daysAfterShipment) {
        if (daysAfterShipment > 30) {
            throw new IllegalStateException("출고일 기준 30일이 경과하여 반품이 불가합니다.");
        }

        if (reason == RefundReason.CHANGE_OF_MIND || reason == RefundReason.OTHER) {
            if (daysAfterShipment <= 10) {
                return RefundStatus.IN_INSPECTION; // 자동 ACCEPT
            }
            return RefundStatus.REJECTED; // 자동 REJECT
        }

        return RefundStatus.REQUESTED;
    }


    /**
     * 환불 완료 처리 / 롤백
     */
    private void rollbackOrderItemStatus(Refund refund) {
        if (refund.getRefundItems() == null) return;

        for (RefundItem ri : refund.getRefundItems()) {
            OrderItem oi = ri.getOrderItem();
            if (oi == null) continue;

            // 신청 시점에 저장한 originalStatus가 있으면 그걸로 원복
            OrderItemStatus original = ri.getOriginalStatus();
            if (original != null) {
                oi.setOrderItemStatus(original);
            } else {
                // fallback
                Order order = refund.getOrder();
                if (order != null && order.getOrderStatus() == OrderStatus.COMPLETED) {
                    oi.setOrderItemStatus(OrderItemStatus.ORDER_COMPLETE);
                } else {
                    oi.setOrderItemStatus(OrderItemStatus.DELIVERED);
                }
            }
        }
    }

    private void handleRefundCompleted(Refund refund) {
        Order order = refund.getOrder();
        if (order == null) return;

        for (RefundItem ri : refund.getRefundItems()) {
            OrderItem oi = ri.getOrderItem();
            if (oi != null) {
                oi.setOrderItemStatus(OrderItemStatus.RETURN_COMPLETED);
            }
        }

        boolean allReturned = order.getOrderItems().stream()
                .allMatch(oi -> oi.getOrderItemStatus() == OrderItemStatus.RETURN_COMPLETED);

        if (allReturned) {
            order.updateStatus(OrderStatus.RETURN_COMPLETED);
        } else {
            order.updateStatus(OrderStatus.PARTIAL_REFUND);
        }
    }


    /**
     * 취소/거절 롤백 로직 교체: “DELIVERED 고정 복구” 제거
     */
    private void restoreOrderStatusAfterRollback(Order order, Refund refund, boolean hasOtherActiveRefund) {
        if (order == null) return;

        // 다른 진행중 반품이 남아있으면 주문은 계속 RETURN_REQUESTED로 유지
        if (hasOtherActiveRefund) {
            order.updateStatus(OrderStatus.RETURN_REQUESTED);
            return;
        }

        // 이미 반품완료된 아이템이 존재하면 PARTIAL_REFUND / 전부면 RETURN_COMPLETED
        boolean anyReturned = order.getOrderItems().stream()
                .anyMatch(oi -> oi.getOrderItemStatus() == OrderItemStatus.RETURN_COMPLETED);

        boolean allReturned = !order.getOrderItems().isEmpty() && order.getOrderItems().stream()
                .allMatch(oi -> oi.getOrderItemStatus() == OrderItemStatus.RETURN_COMPLETED);

        if (allReturned) {
            order.updateStatus(OrderStatus.RETURN_COMPLETED);
            return;
        }
        if (anyReturned) {
            order.updateStatus(OrderStatus.PARTIAL_REFUND);
            return;
        }

        // 그 외에는 "원래 상태"로 복구 (없으면 DELIVERED fallback)
        OrderStatus original = (refund != null) ? refund.getOriginalOrderStatusEnum() : null;
        order.updateStatus(original != null ? original : OrderStatus.DELIVERED);
    }

    // =========================
    // DTO 변환
    // =========================
    private RefundResponseDto toRefundResponseDto(Refund refund) {
        List<Long> bookIds = refund.getRefundItems().stream()
                .map(ri -> ri.getOrderItem().getBookId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> titleMap = fetchBookTitleMap(bookIds);

        // 2. 변환은 DTO에게 위임 (서비스 코드 깔끔해짐)
        return RefundResponseDto.from(refund, titleMap);
    }

    // =========================
    // 내부 DTO / 집계
    // =========================
    private record ReturnableSnapshot(
            Map<Long, Integer> completedMap,
            Map<Long, Integer> activeMap
    ) {}

    private ReturnableSnapshot loadReturnableSnapshot(Long orderId) {
        Map<Long, Integer> completed = refundItemRepository
                .sumByOrderIdAndStatus(orderId, RefundStatus.REFUND_COMPLETED)
                .stream()
                .collect(Collectors.toMap(
                        RefundItemRepository.OrderItemQtyAggregate::getOrderItemId,
                        a -> a.getQuantity() == null ? 0 : a.getQuantity(),
                        Integer::sum
                ));

        Map<Long, Integer> active = refundItemRepository
                .sumByOrderIdAndStatuses(orderId, ACTIVE_REFUND_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        RefundItemRepository.OrderItemQtyAggregate::getOrderItemId,
                        a -> a.getQuantity() == null ? 0 : a.getQuantity(),
                        Integer::sum
                ));

        return new ReturnableSnapshot(completed, active);
    }

    private record RefundCalcResult(
            int refundPayAsPoint,
            int restoreUsedPoint,
            int shippingDeduction
    ) {}

    // 쿠폰은 한 번 사용하면 반품해도 복구 안해줌
    private RefundCalcResult calculateRefundPointAmountProRate(Refund refund) {
        Order order = refund.getOrder();
        if (order == null) return new RefundCalcResult(0, 0, 0);

        int usedPoint = order.getPointDiscount() == null ? 0 : order.getPointDiscount();
        int couponDiscount = order.getCouponDiscount() == null ? 0 : order.getCouponDiscount();

        int orderBase = order.getOrderItems().stream()
                .mapToInt(oi -> (oi.getUnitPrice() == null ? 0 : oi.getUnitPrice())
                        * (oi.getOrderItemQuantity() == null ? 0 : oi.getOrderItemQuantity()))
                .sum();
        if (orderBase <= 0) return new RefundCalcResult(0, 0, 0);

        int thisBase = refund.getRefundItems().stream()
                .mapToInt(ri -> ri.getOrderItem().getUnitPrice() * ri.getRefundQuantity())
                .sum();

        int completedBase = refundRepository.findByOrderOrderId(order.getOrderId()).stream()
                .filter(r -> !Objects.equals(r.getRefundId(), refund.getRefundId()))
                .filter(r -> r.getRefundStatus() == RefundStatus.REFUND_COMPLETED)
                .flatMap(r -> r.getRefundItems().stream())
                .mapToInt(ri -> ri.getOrderItem().getUnitPrice() * ri.getRefundQuantity())
                .sum();

        int usedPointAllocatedBefore = (int) Math.floor((double) usedPoint * completedBase / orderBase);
        int usedPointAllocatedTarget = (int) Math.floor((double) usedPoint * (completedBase + thisBase) / orderBase);
        int restoreUsedPoint = Math.max(usedPointAllocatedTarget - usedPointAllocatedBefore, 0);

        int couponAllocatedBefore = (int) Math.floor((double) couponDiscount * completedBase / orderBase);
        int couponAllocatedTarget = (int) Math.floor((double) couponDiscount * (completedBase + thisBase) / orderBase);
        int couponAllocated = Math.max(couponAllocatedTarget - couponAllocatedBefore, 0);

        int shippingDeduction = calculateShippingDeduction(refund);

        int refundPayAsPoint = Math.max(thisBase - couponAllocated - shippingDeduction, 0);

        return new RefundCalcResult(refundPayAsPoint, restoreUsedPoint, shippingDeduction);
    }

    private int calculateShippingDeduction(Refund refund) {
        Order order = refund.getOrder();
        if (order == null) return 0;

        long days = getDaysAfterShipment(order);
        RefundReason reason = refund.getRefundReason();

        // 파손/오배송은 차감 없음
        if (reason == RefundReason.PRODUCT_DEFECT || reason == RefundReason.WRONG_DELIVERY) {
            return 0;
        }
        // 단순변심/기타 + 10일 이내면 차감 대상
        if (days <= 10 && (reason == RefundReason.CHANGE_OF_MIND || reason == RefundReason.OTHER)) {

            // 주문당 1회 차감: 이미 완료된 반품 중 차감 적용이 있으면 이번엔 0
            boolean alreadyDeducted = refundRepository.existsCompletedRefundWithShippingDeduction(
                    order.getOrderId(),
                    refund.getRefundId() == null ? -1L : refund.getRefundId(),
                    RefundStatus.REFUND_COMPLETED
            );

            if (alreadyDeducted) {
                return 0;
            }

            // 주문 배송비가 있으면 그 값을 쓰고, 없으면 기본 3000원(<- 반품에서도 일단 설정해둠)
            Integer fee = order.getDeliveryFee();
            return (fee != null && fee > 0) ? fee : DEFAULT_RETURN_SHIPPING_FEE;
        }

        return 0;
    }
    /**
     * 회원(userId) 또는 비회원(guestToken) 권한을 검증하는 공통 메서드
     */
    private void validateOrderAuthority(Order order, Long userId, String guestToken) {
        Long targetOrderId = order.getOrderId();

        // 회원 검증
        if (userId != null) {
            if (!userId.equals(order.getUserId())) {
                log.debug("회원 검증 실패");
                throw new AccessDeniedException("본인의 주문만 반품할 수 있습니다.");
            }
            return;
        }

        // 비회원 검증
        if (guestToken != null) {
            if (order.getUserId() != null) {
                log.debug("비회원 검증 실패");
                throw new AccessDeniedException("회원 주문은 비회원 토큰으로 접근할 수 없습니다.");
            }
            Long tokenOrderId = guestTokenProvider.validateTokenAndGetOrderId(guestToken);

            if (!tokenOrderId.equals(targetOrderId)) {
                throw new AccessDeniedException("접근 권한이 없는 주문입니다. (토큰 불일치)");
            }
            return;
        }
        throw new AccessDeniedException("로그인이 필요하거나 잘못된 접근입니다.");
    }
}
