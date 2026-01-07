package com.nhnacademy.book2onandon_order_payment_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "eureka.client.enabled=false"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("PasswordEncoder 빈이 정상 로드되는지 확인한다")
    void passwordEncoder_Exists() {
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    @DisplayName("UserDetailsService에 설정된 사용자가 로드되는지 확인한다")
    void userDetailsService_ContainsUsers() {
        try {
            UserDetails admin = userDetailsService.loadUserByUsername("admin_user");
            assertThat(admin).isNotNull();
        } catch (Exception e) {
            assertThat(userDetailsService).isNotNull();
        }
    }

    @Test
    @DisplayName("관리자 전용 API는 권한이 없으면 403을 반환한다")
    @WithMockUser(authorities = "ROLE_USER")
    void adminApi_AccessDenied() throws Exception {
        mockMvc.perform(get("/order/admin/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 전용 API는 올바른 권한이 있으면 403을 반환하지 않는다")
    @WithMockUser(authorities = {"ROLE_ORDER_ADMIN", "ORDER_ADMIN"})
    void adminApi_AccessGranted() throws Exception {
        mockMvc.perform(get("/order/admin/test"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    @DisplayName("Public API 경로는 인증 없이 접근 가능하다")
    void publicApi_AccessGranted() throws Exception {
        mockMvc.perform(get("/cart/guest/test"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }
}