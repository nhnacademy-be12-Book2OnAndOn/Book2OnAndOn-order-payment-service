package com.nhnacademy.Book2OnAndOn_order_payment_service.order.wrapping.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.wrapping.WrappingPaperRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.util.AesUtils;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import(AesUtils.class) // Converter에서 필요한 AesUtils 주입
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "encryption.secret-key=12345678901234567890123456789012"
})
class WrappingPaperRepositoryTest {

    @Autowired
    private WrappingPaperRepository wrappingPaperRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("새로운 포장지를 저장하고 ID로 조회한다.")
    void saveAndFindTest() {
        // given
        WrappingPaper paper = WrappingPaper.builder()
                .wrappingPaperName("크리스마스 에디션")
                .wrappingPaperPrice(2000)
                .wrappingPaperPath("/images/wrapping/x-mas.jpg")
                .build();

        // when
        WrappingPaper savedPaper = wrappingPaperRepository.save(paper);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<WrappingPaper> result = wrappingPaperRepository.findById(savedPaper.getWrappingPaperId());
        
        assertThat(result).isPresent();
        assertThat(result.get().getWrappingPaperName()).isEqualTo("크리스마스 에디션");
        assertThat(result.get().getWrappingPaperPrice()).isEqualTo(2000);
    }

    @Test
    @DisplayName("모든 포장지 목록을 조회한다.")
    void findAllTest() {
        // given
        WrappingPaper paper1 = WrappingPaper.create("기본 포장", 1000, "/base.jpg");
        WrappingPaper paper2 = WrappingPaper.create("고급 포장", 3000, "/premium.jpg");
        
        wrappingPaperRepository.save(paper1);
        wrappingPaperRepository.save(paper2);
        entityManager.flush();
        entityManager.clear();

        // when
        List<WrappingPaper> list = wrappingPaperRepository.findAll();

        // then
        // [중요] Assertions.assertThat을 썼으므로 hasSize를 바로 사용할 수 있습니다.
        assertThat(list).hasSize(2);
        assertThat(list).extracting("wrappingPaperName")
                .containsExactlyInAnyOrder("기본 포장", "고급 포장");
    }

    @Test
    @DisplayName("포장지 정보를 수정한다.")
    void updateTest() {
        // given
        WrappingPaper paper = WrappingPaper.builder()
                .wrappingPaperName("변경 전")
                .wrappingPaperPrice(500)
                .wrappingPaperPath("/old.jpg")
                .build();
        WrappingPaper saved = wrappingPaperRepository.save(paper);

        // when
        saved.update("변경 후", 1500, "/new.jpg");
        entityManager.flush();
        entityManager.clear();

        // then
        WrappingPaper updated = wrappingPaperRepository.findById(saved.getWrappingPaperId()).orElseThrow();
        assertThat(updated.getWrappingPaperName()).isEqualTo("변경 후");
        assertThat(updated.getWrappingPaperPrice()).isEqualTo(1500);
    }
}