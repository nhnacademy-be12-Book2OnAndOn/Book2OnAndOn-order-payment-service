package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 포장지 정보 관리
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Wrapping_paper")
public class WrappingPaper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wrappingPaperId;

    private String wrappingPaperName;

    private int wrappingPaperPrice;

}