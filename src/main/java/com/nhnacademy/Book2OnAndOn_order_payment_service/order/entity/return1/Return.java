package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.converter.ReturnStatusConverter;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Return")
public class Return {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long returnId;

    @Column(name = "order_id", nullable = false)
    private Long  orderId;

    @Column(name = "return_reason", length = 20, nullable = false)
    private String returnReason;

    @Column(name = "return_reason_detail", length = 100)
    private String returnReasonDetail;

    @Convert(converter = ReturnStatusConverter.class)
    @Column(name = "return_status", columnDefinition = "TINYINT", nullable = false)
    private int returnStatus;

    @Column(name = "return_datetime", nullable = false)
    private LocalDateTime returnDatetime;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "order_id")
    private Order order;
}