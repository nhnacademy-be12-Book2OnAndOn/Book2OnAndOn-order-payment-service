package com.nhnacademy.book2onandon_order_payment_service.order.repository.refund;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    // 1. 특정 주문에 대한 모든 반품 내역 조회 (한 주문에 대해 여러 번 반품(부분반품)할 수 있으므로 List로 반환)
    List<Refund> findByOrderOrderId(Long orderId);

    // 2. 회원의 전체 반품 내역 조회
    Page<Refund> findByOrderUserId(Long orderUserId, Pageable pageable);

    // 3. 특정 주문에 대해 '진행 중' 상태의 반품 목록 조회
    List<Refund> findByOrderOrderIdAndRefundStatusIn(Long orderId, Collection<RefundStatus> refundStatuses);

    // 4. 관리자 검색용
    /**
     * 관리자용 반품 검색 (상태 + 기간 + 회원ID + 주문번호)
     * - status: null 이면 상태 필터 없음
     * - from, to: null 이면 기간 필터 없음
     * - userId: null 이면 회원ID 필터 없음 (비회원 포함)
     * - orderNumber: null 이면 주문번호 필터 없음 (부분 일치 검색)
     */
    @Query("""
    select r
    from Refund r
    join r.order o
    where (:refundStatus is null or r.refundStatus = :refundStatus)
      and (:from is null or r.refundCreatedAt >= :from)
      and (:to is null or r.refundCreatedAt < :to)
      and (:userId is null or o.userId in :userIds)
      and (:orderNumber is null or o.orderNumber like concat('%', :orderNumber, '%'))
      and (
            :includeGuest = true
            or (:includeGuest = false and o.userId is not null)
          )
    """)
    Page<Refund> searchRefunds(
            @Param("refundStatus") RefundStatus refundStatus,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("userIds") List<Long> userIds,
            @Param("orderNumber") String orderNumber,
            @Param("includeGuest") boolean includeGuest,
            Pageable pageable
    );


    // 동시성/멱등성 방어: 상태 변경 시 row lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r join fetch r.order where r.refundId = :refundId")
    Refund findByIdForUpdate(@Param("refundId") Long refundId);

    @Query("""
           select (count(r) > 0)
           from Refund r
           where r.order.orderId = :orderId
             and r.refundId <> :excludeRefundId
             and r.refundStatus in :activeStatuses
    """)
    boolean existsActiveRefundByOrderIdExcludingRefundId(
            @Param("orderId") Long orderId,
            @Param("excludeRefundId") Long excludeRefundId,
            @Param("activeStatuses") List<RefundStatus> activeStatuses
    );

    /**
     * "주문당 1회" 배송비 차감 정책을 위해:
     * 이미 완료된 반품 중 배송비 차감이 적용된 건이 있는지 확인
     */
    @Query("""
        select (count(r) > 0)
        from Refund r
        where r.order.orderId = :orderId
          and r.refundId <> :excludeRefundId
          and r.refundStatus = :completedStatus
          and coalesce(r.shippingDeductionAmount, 0) > 0
    """)
    boolean existsCompletedRefundWithShippingDeduction(
            @Param("orderId") Long orderId,
            @Param("excludeRefundId") Long excludeRefundId,
            @Param("completedStatus") RefundStatus completedStatus
    );
}

