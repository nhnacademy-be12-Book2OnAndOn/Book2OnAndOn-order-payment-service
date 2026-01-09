package com.nhnacademy.book2onandon_order_payment_service.order.entity.wrappingpaper;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperResponseDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    // 생성자
    private WrappingPaper(String wrappingPaperName, int wrappingPaperPrice, String wrappingPaperPath) {
        this.wrappingPaperName = wrappingPaperName;
        this.wrappingPaperPrice = wrappingPaperPrice;
        this.wrappingPaperPath = wrappingPaperPath;
    }

    public static WrappingPaper create(String name, int price, String path) {
        return new WrappingPaper(name, price, path);
    }

    // 비즈니스 로직
    public void update(String name, int price, String path) {
        this.wrappingPaperName = name;
        this.wrappingPaperPrice = price;
        this.wrappingPaperPath = path;
    }

    // DTO 변환
    public WrappingPaperResponseDto toResponseDto() {
        return new WrappingPaperResponseDto(
                this.wrappingPaperId,
                this.wrappingPaperName,
                this.wrappingPaperPrice,
                this.wrappingPaperPath
        );
    }
}

