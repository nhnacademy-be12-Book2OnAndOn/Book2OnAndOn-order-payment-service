package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.*;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 1. N+1 해결을 위한 상세 조회 (Order, OrderItems, DeliveryAddress를 Fetch Join)
    // DTO 변환 시 사용하므로 이들이 N+1 문제의 핵심
    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems oi JOIN FETCH o.deliveryAddress da WHERE o.orderId = :orderId")
    Optional<Order> findOrderWithDetails(Long orderId);
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // 해당아이디에 해당 주문이 있는지
    boolean existsByOrderNumberAndUserId(String orderNumber, Long userId);
    // 결제 검증용
    @Query("SELECT o.totalAmount FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Integer> findTotalAmount(String orderNumber);

    // OrderNumber 존재하는지
    boolean existsByOrderNumber(String orderNumber);


    /*
        TODO 새로 만들 작업
     */

    /*
        일반 사용자
     */

    // 일반 사용자 주문 리스트 조회용
    @Query("""
        SELECT new com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto(
                o.orderId, o.orderNumber, o.orderStatus, o.orderDateTime, o.totalAmount, o.orderTitle
            )
        FROM Order o
        WHERE o.userId = :userId
    """)
    Page<OrderSimpleDto> findAllByUserId(Long userId, Pageable pageable);

    // 주문 번호로 조회
    @EntityGraph(attributePaths = {"orderItems", "orderItems.wrappingPaper", "deliveryAddress", "delivery"})
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.orderNumber = :orderNumber")
    Optional<Order> findByUserIdAndOrderNumber(Long userId, String orderNumber);

    /*
        스케쥴러 작업
     */
    
    // 주문 정크 데이터 조회
    @Query(value = """
        SELECT o.order_id
        FROM Orders o
        WHERE order_status = :status
          AND order_date_time < :thresholdTime
          AND order_id > :lastId
        ORDER BY order_id ASC
        LIMIT :batchSize
    """, nativeQuery = true)
    List<Long> findNextBatch(
            @Param("status") Integer status,
            @Param("thresholdTime") LocalDateTime thresholdTime,
            @Param("lastId") Long lastId,
            @Param("batchSize") int batchSize
    );

    // 주문 정크 데이터 삭제
    @Modifying
    @Query(value = """
        DELETE FROM Orders
        WHERE order_id In :ids
    """, nativeQuery = true)
    int deleteByIds(List<Long> ids);

    /*
        API 호출
     */

    // Book Service 사용자 책 구매 여부
    @Query("""
        SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
        FROM Order o
        JOIN o.orderItems oi
        WHERE o.userId = :userId AND oi.bookId = :bookId AND oi.orderItemStatus = :orderItemStatus
    """)
    boolean existsPurchase(Long userId, Long bookId, OrderItemStatus orderItemStatus);

    @Query("""
        SELECT SUM(o.totalItemAmount)
        FROM Order o
        WHERE o.userId = :userId
        AND o.orderDateTime BETWEEN :fromDate AND :toDate
    """)
    Optional<Long> sumTotalItemAmountByUserIdAndOrderDateTimeBetween(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
        SELECT oi.bookId
        FROM Order o
        JOIN o.orderItems oi
        WHERE o.orderStatus = :orderStatus AND oi.orderItemStatus = :orderItemStatus
        AND o.orderDateTime BETWEEN :start AND :end
        GROUP BY oi.bookId
        ORDER BY SUM(oi.orderItemQuantity) DESC
    """)
    List<Long> findTopBestSellerBookIds(LocalDate start,
                                        LocalDate end,
                                        OrderStatus orderStatus,
                                        OrderItemStatus orderItemStatus,
                                        Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);
}

