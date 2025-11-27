package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestOrderCreateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemDetailDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrappingpaper.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryAddressRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.wrapping.WrappingPaperRepository;

// 외부 모듈 관련 (컴파일을 위해 임시 주석 처리)
// import com.nhnacademy.Book2OnAndOn_order_payment_service.book.repository.BookRepository;
// import com.nhnacademy.Book2OnAndOn_order_payment_service.book.service.StockService;
// import com.nhnacademy.Book2OnAndOn_order_payment_service.book.entity.Book;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** 주문 생성, 상태 변경, 조회 등 주문 관련 핵심 비즈니스 로직 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final GuestOrderRepository guestOrderRepository;
    private final WrappingPaperRepository wrappingPaperRepository;
    private final DeliveryPolicyRepository deliveryPolicyRepository;
    private final PasswordEncoder passwordEncoder;
    // private final BookRepository bookRepository;
    // private final StockService stockService;


    // ======================================================================
    // 1. 주문 생성 API
    // ======================================================================

    /**
     * [회원] 주문을 생성하고 결제 전 필요한 정보 준비
     */
    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequestDto request) {
        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 반드시 존재해야 합니다.");
        }
        
        OrderPriceCalculationDto priceDto = calculateOrderPrices(request);
        Order order = buildAndSaveOrder(request, priceDto);
        saveOrderItems(request.getOrderItems(), order);
        saveDeliveryAddress(request.getDeliveryAddress(), order);

        return convertToOrderResponseDto(order);
    }

    /**
     * [비회원] 주문을 생성하고 결제 전 필요한 정보 준비
     */
    @Transactional
    public OrderResponseDto createGuestOrder(GuestOrderCreateDto request) {
        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 반드시 존재해야 합니다.");
        }
        
        // GuestOrderCreateDto를 OrderCreateRequestDto로 변환하여 로직 재사용
        OrderCreateRequestDto orderRequest = convertToOrderRequest(request);
        
        OrderPriceCalculationDto priceDto = calculateOrderPrices(orderRequest);
        Order order = buildAndSaveOrder(orderRequest, priceDto);
        saveOrderItems(orderRequest.getOrderItems(), order);
        saveDeliveryAddress(orderRequest.getDeliveryAddress(), order);
        
        // 비회원 정보 저장
        saveGuestOrderInfo(request, order); 

        return convertToOrderResponseDto(order);
    }


    // ======================================================================
    // 2. 주문 조회 및 목록 API
    // ======================================================================

    /**
     * [회원/관리자 공통] 주문 상세 정보를 조회
     * @param userId 조회 권한 검증용 ID (관리자는 null)
     */
    @Transactional(readOnly = true)
    public OrderResponseDto findOrderDetails(Long orderId, Long userId) {
        // TODO: N+1 문제 해결을 위해 Fetch Join 쿼리 사용 필요
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // 권한 검증 로직
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 주문에 대한 접근 권한이 없습니다.");
        }
        
        return convertToOrderResponseDto(order);
    }

    /**
     * [비회원] 주문 상세 조회 (비밀번호 검증 포함)
     */
    @Transactional(readOnly = true)
    public OrderResponseDto findGuestOrderDetails(Long orderId, String password) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        Optional<GuestOrder> guestOrderOptional = guestOrderRepository.findByOrder_OrderId(orderId);

        if (guestOrderOptional.isEmpty()) {
            throw new OrderNotFoundException("비회원 주문 정보를 찾을 수 없습니다.");
        }

        GuestOrder guestOrder = guestOrderOptional.get();
        
        // 비밀번호 검증
        if (!passwordEncoder.matches(password, guestOrder.getGuestPassword())) {
            throw new AccessDeniedException("비밀번호가 일치하지 않습니다.");
        }
        
        return convertToOrderResponseDto(order);
    }

    /**
     * [회원] 본인의 주문 목록을 조회 (Pagination 구현)
     */
    @Transactional(readOnly = true)
    public Page<OrderSimpleDto> findOrderList(Long userId, Pageable pageable) {
        //  findByUserId 메서드가 OrderRepository에 정의되어 있다고 가정
        // Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        
        // TODO: 임시 데이터 반환 (실제 Repository 호출 필요)
        return new PageImpl<>(Collections.emptyList(), pageable, 0); 
    }

    /**
     * [관리자] 모든 주문 목록을 조회(Pagination 구현)
     */
    @Transactional(readOnly = true)
    public Page<OrderSimpleDto> findAllOrderList(Pageable pageable) {
        //  OrderRepository.findAll(Pageable pageable) 호출 후 DTO로 변환
        // TODO: 임시 데이터 반환 (실제 Repository 호출 필요)
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }


    // ======================================================================
    // 3. 주문 취소 및 관리자 API
    // ======================================================================

    /**
     * [회원] 주문을 취소
     */
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, Long userId, OrderCancelRequestDto request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인의 주문만 취소할 수 있습니다.");
        }
        
        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("배송 준비 중이거나 결제 대기 중인 주문만 취소 가능합니다.");
        }
        
        order.setOrderStatus(OrderStatus.CANCELED);
        
        // TODO: 재고 복구 (stockService.increaseStock) 로직 호출 필요
        // TODO: 취소 정보를 PaymentInfo 등에 기록하는 로직 추가 필요
        
        return convertToOrderResponseDto(order);
    }

    /**
     * [비회원] 주문을 취소 (비밀번호 검증 포함)
     */
    @Transactional
    public OrderResponseDto cancelGuestOrder(Long orderId, String password) {
        // 1. 비밀번호 검증
        findGuestOrderDetails(orderId, password); 

        // 2. 취소 로직 (취소 사유 DTO가 없으므로 임시로 null 사용)
        return cancelOrder(orderId, null, new OrderCancelRequestDto(null, null, null)); 
    }
    
    /**
     * [관리자] 주문 상태를 변경
     */
    @Transactional
    public OrderResponseDto updateOrderStatusByAdmin(Long orderId, OrderStatusUpdateDto request) {
        // 1. TINYINT 코드를 OrderStatus Enum으로 변환
        OrderStatus newStatus = OrderStatus.fromCode(request.getStatusCode());
        
        // 2. 상태 변경 (유효성 검사는 updateOrderStatus에서 처리)
        updateOrderStatusInternal(orderId, newStatus);
        
        // 3. 변경된 엔티티 조회 및 DTO 변환
        Order order = orderRepository.findById(orderId).get();
        return convertToOrderResponseDto(order);
    }

    // ======================================================================
    // 4. 헬퍼 메서드 및 DTO 변환
    // ======================================================================


    @Transactional
    protected void updateOrderStatusInternal(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELED || order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("이미 최종 처리된 주문은 상태를 변경할 수 없습니다.");
        }

        if (order.getOrderStatus() == OrderStatus.SHIPPING && newStatus == OrderStatus.PENDING) {
            throw new IllegalArgumentException("배송 중 상태에서 대기 상태로 되돌릴 수 없습니다.");
        }
        order.setOrderStatus(newStatus);
    }

    /**
     * [DTO 변환] Order 엔티티를 OrderResponseDto로 변환
     */
    private OrderResponseDto convertToOrderResponseDto(Order order) {
        // TODO: OrderItem, DeliveryAddressInfo 등 연관 관계 DTO 변환 및 합치는 로직 필요
        // 현재는 필수 필드만 반환
        
        // OrderItemDetailDto 리스트는 OrderItem 엔티티를 조회하여 만들어야 함
        List<OrderItemDetailDto> itemDetails = order.getOrderItems().stream()
            .map(this::convertToOrderItemDetailDto)
            .collect(Collectors.toList());
            
        // DeliveryAddressRequestDto는 DeliveryAddress 엔티티를 조회하여 만들어야 함
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findByOrder_OrderId(order.getOrderId()).orElse(null);
        DeliveryAddressRequestDto addressDto = convertToDeliveryAddressRequestDto(deliveryAddress);

        return new OrderResponseDto(
            order.getOrderId(),
            order.getOrderNumber(),
            order.getOrderStatus(),
            order.getOrderDatetime(),
            order.getTotalAmount(),
            order.getTotalDiscountAmount(),
            order.getDeliveryFee(),
            itemDetails, 
            addressDto 
        );
    }
    
    // ----------------------------------------------------------------------
    // DTO 변환 헬퍼 (OrderResponseDto를 위한 하위 객체 생성)
    // ----------------------------------------------------------------------
    
    /**
     * OrderItem 엔티티를 OrderItemDetailDto로 변환
     */
    private OrderItemDetailDto convertToOrderItemDetailDto(OrderItem item) {
        // TODO: BookTitle, WrappingPaperName은 외부 모듈/엔티티에서 조회 필요
        String bookTitle = "Book Title (lookup needed)";
        String wrappingPaperName = item.getWrappingPaper() != null ? item.getWrappingPaper().getWrappingPaperName() : null;
        
        return new OrderItemDetailDto(
            item.getOrderItemId(),
            bookTitle,
            item.getOrderItemQuantity(),
            item.getUnitPrice(),
            item.getOrderItemQuantity() * item.getUnitPrice(), // totalPrice 계산
            wrappingPaperName,
            item.getOrderItemStatus().getDescription() // Enum to String

        );
    }
    
    /**
     * DeliveryAddress 엔티티를 DeliveryAddressRequestDto로 변환
     */
    private DeliveryAddressRequestDto convertToDeliveryAddressRequestDto(DeliveryAddress info) {
        if (info == null) return null;
        return new DeliveryAddressRequestDto(
            info.getDeliveryAddress(),
            info.getDeliveryAddressDetail(),
            info.getDeliveryMessage(),
            info.getRecipient(),
            "010-0000-0000" // 임시 전화번호
        );
    }

    /**
     * [DTO 변환] GuestOrderCreateDto를 OrderCreateRequestDto로 변환
     */
    private OrderCreateRequestDto convertToOrderRequest(GuestOrderCreateDto guestRequest) {
        return new OrderCreateRequestDto(
            null, // 비회원이므로 userId는 null
            guestRequest.getOrderItems(), 
            guestRequest.getDeliveryAddress(),
            guestRequest.getCouponDiscountAmount(),
            guestRequest.getPointDiscountAmount()
        );
    }


    /**
     * Order 엔티티를 생성 -> 초기 상태로 저장
     */
    private Order buildAndSaveOrder(OrderCreateRequestDto request, OrderPriceCalculationDto priceDto) {
        String orderNumber = "B2" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        int totalDiscount = request.getCouponDiscountAmount() + request.getPointDiscountAmount();
        
        int totalAmount = priceDto.getTotalItemPrice()
                + priceDto.getTotalWrappingFee()
                + priceDto.getDeliveryFee()
                - totalDiscount;

        Order order = Order.builder()
            .orderNumber(orderNumber)
            .orderDatetime(LocalDateTime.now())
            .orderStatus(OrderStatus.PENDING)
            .userId(request.getUserId())
            .couponDiscount(request.getCouponDiscountAmount())
            .pointDiscount(request.getPointDiscountAmount())
            .totalDiscountAmount(totalDiscount)
            .totalItemAmount(priceDto.getTotalItemPrice())
            .wrappingFee(priceDto.getTotalWrappingFee())
            .deliveryFee(priceDto.getDeliveryFee())
            .totalAmount(totalAmount)
            .build();

        return orderRepository.save(order);
    }

    /**
     * OrderItem 엔티티 리스트를 생성하여 DB에 저장-> 재고 차감
     */
    private void saveOrderItems(List<OrderItemRequestDto> itemRequests, Order order) {
        for (OrderItemRequestDto itemRequest : itemRequests) {
            
            // 1. 포장지 조회
            WrappingPaper wrappingPaper = itemRequest.getWrappingPaperId() != null ? 
                wrappingPaperRepository.findById(itemRequest.getWrappingPaperId()).orElse(null) : 
                null;
            
            // 2. OrderItem 엔티티 생성
            OrderItem orderItem = OrderItem.builder()
                .order(order)
                //.book(book)
                .orderItemQuantity((byte) itemRequest.getQuantity()) // 형 변환 유지
                .unitPrice(10000) // 임시 단가 설정 (실제로는 Book에서 조회해야 함)
                .isWrapped(itemRequest.isWrapped())
                .orderItemStatus(OrderItemStatus.PREPARING)
                .wrappingPaper(wrappingPaper)
                .build();

            orderItemRepository.save(orderItem);

            // TODO: 재고 차감 로직 (stockService.decreaseStock) 호출 필요
        }
    }

    private void saveDeliveryAddress(DeliveryAddressRequestDto addressRequest, Order order) {
        DeliveryAddress address = DeliveryAddress.builder()
            .order(order)
            .deliveryAddress(addressRequest.getDeliveryAddress())
            .deliveryAddressDetail(addressRequest.getDeliveryAddressDetail())
            .deliveryMessage(addressRequest.getDeliveryMessage())
            .recipient(addressRequest.getRecipient())
            .build();

        deliveryAddressRepository.save(address);
    }

    private void saveGuestOrderInfo(GuestOrderCreateDto guestRequest, Order order) {
        String encryptedPassword = passwordEncoder.encode(guestRequest.getGuestPassword());

        GuestOrder guestOrder = GuestOrder.builder()
            .order(order)
            .guestName(guestRequest.getGuestName())
            .guestPhonenumber(guestRequest.getGuestPhoneNumber())
            .guestPassword(encryptedPassword)
            .build();

        guestOrderRepository.save(guestOrder);
    }

    /** OrderItem, WrappingPaper 정보를 조회하여 모든 금액 계산 */
    private OrderPriceCalculationDto calculateOrderPrices(OrderCreateRequestDto request) {
        int totalItemPrice = 0;
        int totalWrappingFee = 0;

        for (OrderItemRequestDto itemRequest : request.getOrderItems()) {
            // TODO: 실제 BookRepository에서 가격 정보를 조회해야 함
            int bookPrice = 10000;
            totalItemPrice += (bookPrice * itemRequest.getQuantity());

            if (itemRequest.isWrapped()) {
                // TODO: 실제 WrappingPaperRepository에서 가격 정보를 조회해야 함
                int wrappingPrice = 2000;
                totalWrappingFee += wrappingPrice;
            }
        }

        // 2. 배송비 정책 적용
        DeliveryPolicy defaultPolicy = deliveryPolicyRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("기본 배송 정책을 찾을 수 없습니다."));

        int deliveryFee = calculateDeliveryCost(totalItemPrice, defaultPolicy);

        return new OrderPriceCalculationDto(totalItemPrice, totalWrappingFee, deliveryFee);
    }

    /**총 상품 가격과 정책 기반 배송비 계산*/
    private int calculateDeliveryCost(int totalItemPrice, DeliveryPolicy policy) {
        if (totalItemPrice >= policy.getFreeDeliveryThreshold()) {
            return 0;
        }
        return policy.getDeliveryFee();
    }

    // 이 DTO는 Service 내부에서만 사용
    @Getter
    @AllArgsConstructor
    private static class OrderPriceCalculationDto {
        private final int totalItemPrice;
        private final int totalWrappingFee;
        private final int deliveryFee;
    }
}