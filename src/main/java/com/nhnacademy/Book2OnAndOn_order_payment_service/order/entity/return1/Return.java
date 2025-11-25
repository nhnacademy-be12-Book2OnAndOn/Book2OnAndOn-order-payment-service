package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.converter.ReturnStatusConverter;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "Return")
public class Return {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "return_reason", length = 20)
    @NotNull
    private String returnReason;

    @Column(name = "return_reason_detail", length = 100)
    private String returnReasonDetail;

    @Column(name = "return_status", columnDefinition = "TINYINT")
    @NotNull
    private Integer returnStatus;

    @Column(name = "return_datetime")
    @NotNull
    private LocalDateTime returnDatetime = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @NotNull
    private Order order;

    @OneToMany(mappedBy = "returnEntity", cascade = CascadeType.ALL)
    private List<ReturnItem> returnItem = new ArrayList<>();
}