package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundStatusUpdateDto;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RefundService {

    // 1) 회원 반품 신청
    RefundResponseDto createRefundForMember(Long orderId, Long userId, RefundRequestDto request);

    // 2) 회원 반품 상세 조회
    RefundResponseDto getRefundDetailsForMember(Long userId, Long refundId);

    // 3) 회원의 전체 반품 목록 조회
    Page<RefundResponseDto> getRefundsForMember(Long userId, Pageable pageable);

    // 4) 비회원 반품 신청
    RefundResponseDto createRefundForGuest(Long orderId, RefundRequestDto request);

    // 5) 비회원 반품 상세 조회
    RefundResponseDto getRefundDetailsForGuest(Long refundId);

    // 6) 비회원의 전체 반품 목록 조회
//    Page<RefundResponseDto> getRefundsForGuest(Long orderId, Pageable pageable);

    // 7) 관리자 반품 목록 조회
    Page<RefundResponseDto> getRefundListForAdmin(Integer refundStatus,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  Long userId,
                                                  String userKeyword,
                                                  String orderNumber,
                                                  boolean includeGuest,
                                                  Pageable pageable
    );

    // 8) 관리자 반품 상세 조회
    RefundResponseDto getRefundDetailsForAdmin(Long refundId);

    // 9) 관리자 반품 상태 변경
    RefundResponseDto updateRefundStatus(Long refundId, RefundStatusUpdateDto request);

}
