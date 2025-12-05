package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.StockDecreaseRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1.ReturnItemDetailDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1.ReturnItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1.ReturnRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1.ReturnResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1.ReturnStatusUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1.Return;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1.ReturnItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1.ReturnStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.return1.ReturnItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.return1.ReturnRepository;
import feign.FeignException;
import java.util.Collections;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnService {

    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookServiceClient bookServiceClient; // 도서 제목 조회를 위해 필요

    /**
     * [회원] 반품 신청
     */
    @Transactional
    public ReturnResponseDto createReturn(Long orderId, Long userId, ReturnRequestDto request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인의 주문에 대해서만 반품을 신청할 수 있습니다.");
        }

        // 반품 가능 상태 확인: 배송 완료(DELIVERED) 또는 주문 완료(COMPLETED) 상태만 허용
        if (order.getOrderStatus() != OrderStatus.DELIVERED && order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("배송 완료된 주문만 반품 신청이 가능합니다. (현재 상태: " + order.getOrderStatus().getDescription() + ")");
        }

        // 1. Return 엔티티 생성
        Return returnEntity = new Return(
                null,
                request.getReturnReason(),
                request.getReturnReasonDetail(),
                ReturnStatus.REQUESTED.getCode(), // 0: 반품 신청
                null, // returnDatetime (Entity에서 자동 설정)
                order,
                null // returnItem 리스트는 아래에서 채움
        );
        returnEntity = returnRepository.save(returnEntity);
        
        // 2. ReturnItem 생성 및 연관관계 설정
        for (ReturnItemRequestDto itemDto : request.getReturnItems()) {
            OrderItem orderItem = orderItemRepository.findById(itemDto.getOrderItemId())
                    .orElseThrow(() -> new IllegalArgumentException("주문 항목(OrderItemId: " + itemDto.getOrderItemId() + ")을 찾을 수 없습니다."));

            if (itemDto.getReturnQuantity() <= 0 || itemDto.getReturnQuantity() > orderItem.getOrderItemQuantity()) {
                throw new IllegalArgumentException("반품 수량이 올바르지 않습니다. (요청: " + itemDto.getReturnQuantity() + ", 최대: " + orderItem.getOrderItemQuantity() + ")");
            }

            ReturnItem returnItem = new ReturnItem(
                    null,
                    itemDto.getReturnQuantity(),
                    returnEntity,
                    orderItem
            );
            returnItemRepository.save(returnItem);
            returnEntity.getReturnItem().add(returnItem);
        }
        
        // 3. 주문 상태를 반품 신청(RETURN_REQUESTED)으로 변경
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);

        return convertToReturnResponseDto(returnEntity);
    }

    /**
     * [회원/관리자] 반품 상세 정보 조회 (관리자는 userId=null)
     */
    @Transactional(readOnly = true)
    public ReturnResponseDto getReturnDetails(Long returnId, Long userId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new ReturnNotFoundException("반품 내역을 찾을 수 없습니다. (ID: " + returnId + ")"));

        // 회원 권한 검증 (관리자(userId=null)는 검증 건너뜀)
        if (userId != null && !returnEntity.getOrder().getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 반품 내역에 대한 접근 권한이 없습니다.");
        }

        return convertToReturnResponseDto(returnEntity);
    }

    /**
     * [관리자] 반품 상태 변경
     */
    @Transactional
    public ReturnResponseDto updateReturnStatusByAdmin(Long returnId, ReturnStatusUpdateDto request) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new ReturnNotFoundException("반품 내역을 찾을 수 없습니다. (ID: " + returnId + ")"));

        ReturnStatus newStatus = ReturnStatus.fromCode(request.getStatusCode());

        // 1. 상태 업데이트
        returnEntity.setReturnStatus(newStatus.getCode());

        if (newStatus == ReturnStatus.REFUND_COMPLETED) {
            // [환불 완료] - 필수 로직: 재고 복구 및 주문 상태 확정

            // 1) 재고 복구 (BookClient 호출)
            List<StockDecreaseRequest> stockRestoreRequests = returnEntity.getReturnItem().stream()
                    .map(item -> new StockDecreaseRequest(item.getOrderItem().getBookId(), item.getReturnQuantity()))
                    .collect(Collectors.toList());
            bookServiceClient.increaseStock(stockRestoreRequests);

            // 2) 주문 상태 최종 변경
            returnEntity.getOrder().setOrderStatus(OrderStatus.RETURN_COMPLETED);

        } else if (newStatus == ReturnStatus.REJECTED) {
            // [반품 거부] - 필수 로직: 주문 상태 원복
            returnEntity.getOrder().setOrderStatus(OrderStatus.DELIVERED); // 배송 완료 상태로 원복 가정
        }

        // 2. 엔티티 저장
        returnRepository.save(returnEntity);

        return convertToReturnResponseDto(returnEntity);
    }

    // --- 헬퍼 메서드 ---
    /**
     * Return 엔티티를 ReturnResponseDto로 변환
     */
    private ReturnResponseDto convertToReturnResponseDto(Return returnEntity) {

        // 1. OrderItem ID 리스트 추출
        List<Long> bookIds = returnEntity.getReturnItem().stream()
                .map(item -> item.getOrderItem().getBookId())
                .collect(Collectors.toList());

        // 2. BookServiceClient를 사용하여 도서 제목 조회
        Map<Long, BookOrderResponse> tempBookMap; // 임시 변수 선언

        try {
            List<BookOrderResponse> bookInfos = bookServiceClient.getBooksForOrder(bookIds);
            tempBookMap = bookInfos.stream().collect(Collectors.toMap(BookOrderResponse::getBookId, Function.identity()));
        } catch (FeignException e) {
            tempBookMap = Collections.emptyMap(); // 빈 Map 할당
            System.err.println("WARN: Failed to fetch book titles for return: " + e.getMessage());
        }

        // 람다 내부에서 참조할 final 변수를 선언하고 할당
        final Map<Long, BookOrderResponse> finalBookMap = tempBookMap;

        // 3. ReturnItem 상세 정보 DTO 변환
        List<ReturnItemDetailDto> itemDetails = returnEntity.getReturnItem().stream()
                .map(item -> {
                    Long bookId = item.getOrderItem().getBookId();

                    // finalBookMap을 사용하여 안전하게 참조
                    String bookTitle = finalBookMap.getOrDefault(bookId, new BookOrderResponse()).getTitle();

                    return new ReturnItemDetailDto(
                            item.getReturnItemId(),
                            item.getOrderItem().getOrderItemId(),
                            bookTitle,
                            item.getReturnQuantity()
                    );
                })
                .collect(Collectors.toList());

        return new ReturnResponseDto(
                returnEntity.getReturnId(),
                returnEntity.getOrder().getOrderId(),
                returnEntity.getReturnReason(),
                returnEntity.getReturnReasonDetail(),
                ReturnStatus.fromCode(returnEntity.getReturnStatus()).getDescription(),
                returnEntity.getReturnDatetime(),
                itemDetails
        );
    }

    // 커스텀 예외 정의: ReturnNotFoundException (NotFoundException을 상속받도록 정의 필요)
    public static class ReturnNotFoundException extends RuntimeException {
        public ReturnNotFoundException(String message) {
            super(message);
        }
    }
}