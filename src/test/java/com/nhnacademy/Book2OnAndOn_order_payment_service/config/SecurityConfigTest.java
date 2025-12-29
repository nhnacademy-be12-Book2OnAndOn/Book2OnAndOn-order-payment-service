package com.nhnacademy.Book2OnAndOn_order_payment_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(SecurityConfig.class)
// [추가] Config Server 관련 설정을 테스트 시 무시하도록 설정
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("PasswordEncoder 빈이 BCryptPasswordEncoder 인스턴스인지 확인한다")
    void passwordEncoder_IsBCrypt() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("InMemoryUserDetailsService에 설정된 사용자들이 정상 로드되는지 확인한다")
    void userDetailsService_ContainsUsers() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin_user");
        UserDetails user = userDetailsService.loadUserByUsername("test_user");

        assertThat(admin).isNotNull();
        assertThat(user).isNotNull();
        assertThat(passwordEncoder.matches("password", admin.getPassword())).isTrue();
    }

    @Test
    @DisplayName("관리자 전용 API는 ORDER_ADMIN 권한이 없으면 403을 반환한다")
    @WithMockUser(roles = "USER")
    void adminApi_AccessDenied_ForUser() throws Exception {
        mockMvc.perform(get("/order/admin/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 전용 API는 ORDER_ADMIN 권한이 있으면 정상 접근된다")
    @WithMockUser(roles = "ORDER_ADMIN")
    void adminApi_AccessGranted_ForAdmin() throws Exception {
        mockMvc.perform(get("/order/admin/test"))
                // 401/403이 아닌 404면 시큐리티 권한 필터는 통과했음을 의미
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Public API(/cart/guest/**)는 인증 없이 접근 가능하다")
    void publicApi_AccessGranted() throws Exception {
        mockMvc.perform(get("/cart/guest/test"))
                .andExpect(status().isNotFound());
    }
}