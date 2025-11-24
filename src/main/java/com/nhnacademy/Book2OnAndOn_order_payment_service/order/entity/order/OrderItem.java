package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.converter.OrderItemStatusConverter;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrapping.WrappingPaper;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "OrderItem")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_item_quantity", columnDefinition = "TINYINT", nullable = false)
    private int orderItemQuantity;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "wrapping_paper_id", nullable = false)
    private int wrappingPaperId;

    @Column(name = "is_wrapped", nullable = false)
    private boolean isWrapped;

    @Convert(converter = OrderItemStatusConverter.class)
    @Column(name = "order_item_status", columnDefinition = "TINYINT", nullable = false)
    private OrderItemStatus orderItemStatus;

    @Column(name = "book_id", nullable = false)
    private Long bookId;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wrapping_paper_id")
    private WrappingPaper wrappingPaper;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "book_id")
//    private Book book;
}