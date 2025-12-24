package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundGuestRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundAvailableItemResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundStatusUpdateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RefundService {

    // 1) 회원 반품 신청
    RefundResponseDto createRefundForMember(Long orderId, Long userId, RefundRequestDto request);

    // 2) 회원 반품 신청 취소
    RefundResponseDto cancelRefundForMember(Long orderId, Long refundId, Long userId);

    // 3) 회원 반품 가능 품목
    List<RefundAvailableItemResponseDto> getRefundableItemsForMember(Long orderId, Long userId);

    // 5) 회원 반품 상세 조회
    RefundResponseDto getRefundDetailsForMember(Long orderId, Long refundId, Long userId);

    // 7) 회원의 전체 반품 목록 조회
    Page<RefundResponseDto> getRefundsForMember(Long userId, Pageable pageable);



    // 2) 비회원 반품 신청
    RefundResponseDto createRefundForGuest(Long orderId, RefundGuestRequestDto request);

    // 2) 비회원 반품 신청 취소
    RefundResponseDto cancelRefundForGuest(Long orderId, Long refundId, String guestPassword);

    // 4) 비회원 반품 가능 품목
    List<RefundAvailableItemResponseDto> getRefundableItemsForGuest(Long orderId);

    // 6) 비회원 반품 상세 조회
    RefundResponseDto getRefundDetailsForGuest(Long orderId, Long refundId);



    // 8) 관리자 반품 목록 조회
    Page<RefundResponseDto> getRefundListForAdmin(RefundStatus refundStatus,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  Long userId,
                                                  String userKeyword,
                                                  String orderNumber,
                                                  boolean includeGuest,
                                                  Pageable pageable
    );

    // 9) 관리자 반품 상세 조회
    RefundResponseDto getRefundDetailsForAdmin(Long refundId);

    // 10) 관리자 반품 상태 변경
    RefundResponseDto updateRefundStatus(Long refundId, RefundStatusUpdateRequestDto request);

}
