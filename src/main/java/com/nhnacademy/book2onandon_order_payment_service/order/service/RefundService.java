package com.nhnacademy.book2onandon_order_payment_service.order.service;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundSearchCondition;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.response.RefundAvailableItemResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundStatusUpdateRequestDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RefundService {

    // 1) 반품 신청
    RefundResponseDto createRefund(Long orderId, Long userId, RefundRequestDto request, String guestToken);

    // 2) 반품 신청 취소
    RefundResponseDto cancelRefund(Long orderId, Long refundId, Long userId, String guestToken);

    // 3) 반품 가능 품목
    List<RefundAvailableItemResponseDto> getRefundableItems(Long orderId, Long userId, String guestToken);

    // 4) 반품 상세 조회
    RefundResponseDto getRefundDetails(Long orderId, Long refundId, Long userId, String guestToken);

    // 5) 회원의 전체 반품 목록 조회 (회원전용)
    Page<RefundResponseDto> getRefundsForMember(Long userId, Pageable pageable);

    // 8) 관리자 반품 목록 조회
    Page<RefundResponseDto> getRefundListForAdmin(RefundSearchCondition condition,
                                                  Pageable pageable
    );

    // 9) 관리자 반품 상세 조회
    RefundResponseDto getRefundDetailsForAdmin(Long refundId);

    // 10) 관리자 반품 상태 변경
    RefundResponseDto updateRefundStatus(Long refundId, RefundStatusUpdateRequestDto request);

}
