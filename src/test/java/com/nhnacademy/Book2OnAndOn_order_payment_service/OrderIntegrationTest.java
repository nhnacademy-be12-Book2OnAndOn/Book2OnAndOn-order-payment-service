package com.nhnacademy.Book2OnAndOn_order_payment_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.wrapping.WrappingPaperRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf; // ⬅️ CSRF 오류 해결!
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // Spring Boot 컨텍스트 전체 로딩
@AutoConfigureMockMvc // MockMvc 자동 설정
@Transactional // 테스트 후 DB 롤백
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // 가상 HTTP 요청 도구
    @Autowired
    private ObjectMapper objectMapper; // DTO를 JSON으로 변환
    @Autowired
    private OrderRepository orderRepository; // DB 상태 확인용 Repository
    @Autowired
    private DeliveryPolicyRepository deliveryPolicyRepository;
    // ⬇️ 추가: EntityManager와 TransactionTemplate 주입
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    // ⚠️ 주의: MockMvc는 Spring Security를 우회하므로, 실제 권한 검증은 별도 설정이 필요합니다.
    @Autowired
    private WrappingPaperRepository wrappingPaperRepository;

    private OrderCreateRequestDto validRequest;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_DELIVERY_POLICY_ID = 1L;
    private final Long TEST_WRAPPING_PAPER_ID = 5L;
    private final Long TEST_BOOK_ID = 20L;

    @BeforeEach
    void setUp() {
        // ⬇️ 🚨 최종 해결: TransactionTemplate 내부에서 Native SQL 사용 + Detach
        transactionTemplate.executeWithoutResult(status -> {

            // 1. 기존 데이터 삭제 (StaleObjectStateException 방지)
            // OrderService가 의존하는 필수 연관관계 엔티티도 함께 삭제합니다.
            // ⚠️ TRUNCATE TABLE이 더 확실하지만, DELETE 사용
            entityManager.createNativeQuery("DELETE FROM DELIVERY_POLICY WHERE delivery_policy_id = 1").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM WRAPPING_PAPER WHERE wrapping_paper_id IN (5)").executeUpdate();
//            entityManager.createNativeQuery("DELETE FROM BOOK WHERE book_id IN (20)").executeUpdate();

            // 2. DeliveryPolicy 삽입 (OrderService가 의존하는 필수 데이터)
            entityManager.createNativeQuery(
                    "INSERT INTO DELIVERY_POLICY (delivery_policy_id, delivery_policy_name, delivery_fee, free_delivery_threshold) VALUES (?, '기본 정책', 3000, 30000)"
            ).setParameter(1, TEST_DELIVERY_POLICY_ID).executeUpdate();

            // 3. WrappingPaper 삽입
            entityManager.createNativeQuery(
                    "INSERT INTO WRAPPING_PAPER (wrapping_paper_id, wrapping_paper_name, wrapping_paper_price, wrapping_paper_path) VALUES (?, '고급 포장', 2000, '/path/to/img')"
            ).setParameter(1, TEST_WRAPPING_PAPER_ID).executeUpdate();

//            // 4. Book 엔티티 삽입
//            entityManager.createNativeQuery(
//                    "INSERT INTO BOOK (book_id, price, stock) VALUES (?, 10000, 100)"
//            ).setParameter(1, TEST_BOOK_ID).executeUpdate();

            // 5. 💡 핵심: 영속성 컨텍스트 초기화 및 커밋 강제
            entityManager.flush();
            // ⚠️ clear()를 호출하여 테스트 컨텍스트가 이 엔티티들을 추적하지 않도록 합니다.
            // 이로써 다음 테스트에서 StaleObjectStateException이 발생하지 않습니다.
            entityManager.clear();
        });

        // 5. 유효한 주문 요청 DTO 생성 (DB에 삽입된 ID 사용)
        validRequest = new OrderCreateRequestDto(
                TEST_USER_ID,
                List.of(new OrderItemRequestDto(TEST_BOOK_ID, 2, TEST_WRAPPING_PAPER_ID, true)),
                new DeliveryAddressRequestDto("서울시", "강남구", "문 앞", "김철수", "01012345678"),
                1000,
                500
        );

    }

    // ----------------------------------------------------------------------
    // 2. 주문 생성 API 통합 테스트
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/orders 호출 시 주문이 생성되고 201 응답을 반환해야 한다")
    void createOrder_shouldCreateOrderAndReturn201() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(validRequest);
        long initialOrderCount = orderRepository.count();

        // When & Then
        // 1. MockMvc를 이용해 POST 요청 수행
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        // ⬇️ 🚨 수정: .with() 구문을 perform 내부의 요청 빌더 체인에 넣어야 합니다.
                        .with(user("test_user").roles("USER")) // 💡 요청에 인증 정보 추가
                        .with(csrf()))                          // 💡 요청에 CSRF 토큰 추가

                // 2. HTTP 응답 검증 (201 Created 확인)
                .andExpect(status().isCreated())

                // 3. 응답 DTO 필드 검증
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").isNumber());

        // 4. 실제 DB 상태 검증
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount + 1);
    }

    // ----------------------------------------------------------------------
    // 3. 주문 조회 API 통합 테스트
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/orders/{orderId} 호출 시 200 응답과 상세 주문 정보가 반환되어야 한다")
    void getOrderDetails_shouldReturn200AndDetails() throws Exception {
        // Given: 주문 생성 테스트를 먼저 실행하고 ID를 추출합니다.
        String requestBody = objectMapper.writeValueAsString(validRequest);

        // 1. 주문 생성 및 응답 ID 추출
        String responseContent = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user("test_user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // 2. 🚨 수정: 생성된 주문의 ID 추출
        Long existingOrderId = objectMapper.readTree(responseContent).get("orderId").asLong();

        // When & Then
        mockMvc.perform(get("/api/orders/{orderId}", existingOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("test_user").roles("USER")))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(existingOrderId));
    }

    // ----------------------------------------------------------------------
    // 4. 항목 누락 테스트 (400 오류도 403을 피하기 위해 인증 필요)
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("주문 항목이 누락된 경우 400 Bad Request가 발생해야 한다")
    void createOrder_shouldReturn400_whenOrderItemsAreMissing() throws Exception {
        // Given
        // 1. 유효하지 않은 요청 DTO 생성 (주문 항목 List<OrderItemRequestDto>가 비어 있음)
        OrderCreateRequestDto invalidRequest = new OrderCreateRequestDto(
                TEST_USER_ID,
                Collections.emptyList(), // ⚠️ 주문 항목 누락 (유효성 검사 실패 예상)
                new DeliveryAddressRequestDto("서울시", "강남구", "문 앞", "김철수", "01012345678"),
                0,
                0
        );

        // ⬇️ 🚨 수정: requestBody 변수를 메서드 내에서 생성합니다.
        String requestBody = objectMapper.writeValueAsString(invalidRequest);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user("test_user").roles("USER"))
                        .with(csrf()))

                // 400 Bad Request 확인 (비즈니스 로직에 도달하여 유효성 검사 실패)
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().string("주문 항목은 반드시 존재해야 합니다."));
    }
}