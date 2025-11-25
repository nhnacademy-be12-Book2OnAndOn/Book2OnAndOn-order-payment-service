package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrappingpaper;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "WrappingPaper")
public class WrappingPaper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wrapping_paper_id")
    private Long wrappingPaperId;

    @Column(name = "wrapping_paper_name", length = 20)
    @NotNull
    private String wrappingPaperName;

    @Column(name = "wrapping_paper_price")
    @NotNull
    private Integer wrappingPaperPrice;

    @Column(name = "wrapping_paper_path", length = 200)
    @NotNull
    private String wrappingPaperPath;

    @OneToOne(mappedBy = "wrappingPaper")
    private OrderItem orderItem;
}