package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemStatusUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/orders") // Base Path
@Slf4j
@PreAuthorize("hasRole('ORDER_ADMIN')") // 1차적으로 게이트웨이에서 방어 2차적으로 api에서 방어
public class OrderAdminController {

    private final OrderService orderService;

    // GET /admin/orders (관리자 전체 주문 목록 조회)
    @GetMapping
    public ResponseEntity<PageResponse<OrderSimpleDto>> findAllOrderList(Pageable pageable) {
        log.info("[ADMIN-API] GET /admin/orders page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        // Service에서 모든 주문 목록을 조회
        Page<OrderSimpleDto> orderSimpleDtoPage = orderService.getOrderListWithAdmin(pageable);

        PageResponse<OrderSimpleDto> resp = new PageResponse<>(
                orderSimpleDtoPage.getContent(),
                orderSimpleDtoPage.getNumber(),
                orderSimpleDtoPage.getSize(),
                orderSimpleDtoPage.getTotalPages(),
                orderSimpleDtoPage.getTotalElements(),
                orderSimpleDtoPage.isLast()
        );

        return ResponseEntity.ok(resp);
    }

    // GET /admin/orders/{orderId} (관리자 특정 주문 상세 조회)
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderDetailResponseDto> findAdminOrderDetails(@PathVariable("orderNumber") String orderNumber) {
        log.info("[ADMIN-API] GET /admin/orders/{}", orderNumber);

        // 관리자용 상세 조회는 사용자 ID 검증 없이 주문 ID만으로 조회
        OrderDetailResponseDto response = orderService.getOrderDetailWithAdmin(orderNumber);
        return ResponseEntity.ok(response);
    }

    // PATCH /admin/orders/{orderId} (관리자 주문 상태 변경)
    @PatchMapping("/{orderNumber}")
    public ResponseEntity<Void> updateOrderStatusByAdmin(@PathVariable("orderNumber") String orderNumber,
                                                         @RequestBody OrderStatusUpdateDto req) {
        log.info("[ADMIN-API] GET /admin/orders/{}", orderNumber);

        orderService.setOrderStatus(orderNumber, req);

        return ResponseEntity.noContent().build();
    }

    // PATCH /admin/orders/{orderId} (관리자 주문항목 상태 변경)
    @PatchMapping("/{orderNumber}/order-items")
    public ResponseEntity<Void> updateOrderItemStatusByAdmin(@PathVariable("orderNumber") String orderNumber,
                                                             @RequestBody OrderItemStatusUpdateDto req) {

        log.info("[ADMIN-API] PATCH /admin/orders/{}/order-items - itemId={}, status={}",
                orderNumber, req.orderItemId(), req.orderItemStatus());

        orderService.setOrderItemStatus(orderNumber, req);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{orderNumber}/cancel")
    public ResponseEntity<Void> cancelOrderByAdmin(@PathVariable("orderNumber") String orderNumber){
        log.info("[ADMIN-API] PATCH /admin/orders/{}/cancel", orderNumber);

        orderService.cancelOrderByAdmin(orderNumber);

        return ResponseEntity.noContent().build();
    }
}