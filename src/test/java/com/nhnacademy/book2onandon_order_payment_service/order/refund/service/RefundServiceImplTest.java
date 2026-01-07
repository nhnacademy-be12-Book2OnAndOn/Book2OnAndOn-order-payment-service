package com.nhnacademy.book2onandon_order_payment_service.order.refund.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.client.UserServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundSearchCondition;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundReason;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.refund.RefundRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.impl.RefundServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {
    @InjectMocks
    RefundServiceImpl refundService;

    @Mock
    RefundRepository refundRepository;

    @Mock
    UserServiceClient userServiceClient;

    @Test
    @DisplayName("관리자 검색: 유저 키워드로 검색했지만 결과 없으면 DB 조회 없이 빈 페이지 반환해야함")
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
    @DisplayName("관리자 검색: 날짜 및 유저 키워드 정상 검색")
    void search_normal() {
        RefundSearchCondition condition = new RefundSearchCondition();
        condition.setStartDate(LocalDate.of(2025, 1, 1));
        condition.setEndDate(LocalDate.of(2025, 1, 31));
        condition.setUserKeyword("홍길동");

        List<Long> mockUserIds = List.of(1L, 2L);
        given(userServiceClient.searchUserIdsByKeyword("홍길동")).willReturn(mockUserIds);

        Refund mockRefund = mock(Refund.class);

        given(mockRefund.getRefundItems()).willReturn(Collections.emptyList());

        given(mockRefund.getRefundReason()).willReturn(RefundReason.OTHER);
        given(mockRefund.getRefundStatus()).willReturn(RefundStatus.REQUESTED);

        Page<Refund> mockPage = new PageImpl<>(List.of(mockRefund));
        given(refundRepository.searchRefunds(
                eq(null), // status
                eq(LocalDateTime.of(2025, 1, 1, 0, 0, 0)), // start
                eq(LocalDateTime.of(2025, 2, 1, 0, 0, 0)), // end (plusDays(1) 확인)
                eq(mockUserIds), // userIds 리스트 전달 확인
                eq(null),
                eq(true),
                any(Pageable.class)
        )).willReturn(mockPage);

        refundService.getRefundListForAdmin(condition, PageRequest.of(0, 10));


        verify(refundRepository, times(1)).searchRefunds(any(), any(), any(), any(), any(), anyBoolean(), any());
    }
}