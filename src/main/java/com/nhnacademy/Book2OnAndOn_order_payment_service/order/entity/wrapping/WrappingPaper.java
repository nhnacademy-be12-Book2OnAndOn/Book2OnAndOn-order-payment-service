package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrapping;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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
@Table(name = "WrappingPaper")
public class WrappingPaper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wrappingPaperId;

    @Column(name = "wrapping_papper_name", length = 20, nullable = false)
    private String wrappingPaperName;

    @Column(name = "wrapping_paper_price", nullable = false)
    private Integer wrappingPaperPrice;

    @Column(name = "wrapping_paper_path", length = 200, nullable = false)
    private String wrappingPaperPath;

    @OneToOne(mappedBy = "wrappingPaper")
    private OrderItem orderItem;
}