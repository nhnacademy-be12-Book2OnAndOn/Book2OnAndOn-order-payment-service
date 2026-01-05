package com.nhnacademy.Book2OnAndOn_order_payment_service.config;

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
    @DisplayName("PasswordEncoder 빈이 정상 로드되고 동작하는지 확인한다")
    void passwordEncoder_Bean_Test() {
        assertThat(passwordEncoder).isNotNull();
        String rawPassword = "password";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    @DisplayName("UserDetailsService에 설정된 하드코딩된 사용자들이 정상 로드된다")
    void userDetailsService_Users_Exist() {
        // test_user 확인
        UserDetails user = userDetailsService.loadUserByUsername("test_user");
        assertThat(user).isNotNull();
        assertThat(user.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

        // hacker_user 확인
        UserDetails hacker = userDetailsService.loadUserByUsername("hacker_user");
        assertThat(hacker).isNotNull();

        // admin_user 확인
        UserDetails admin = userDetailsService.loadUserByUsername("admin_user");
        assertThat(admin).isNotNull();
        assertThat(admin.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ORDER_ADMIN"));
    }

    @Test
    @DisplayName("URL 보안: 관리자 API는 권한(ORDER_ADMIN)이 없으면 403 Forbidden")
    @WithMockUser(username = "user", roles = "USER")
    void adminApi_AccessDenied_RoleUser() throws Exception {
        mockMvc.perform(get("/order/admin/some-resource"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("URL 보안: 관리자 API는 익명 사용자일 경우 401 Unauthorized (Basic Auth)")
    void adminApi_AccessDenied_Anonymous() throws Exception {
        mockMvc.perform(get("/order/admin/some-resource"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("URL 보안: 관리자 API는 올바른 권한(ORDER_ADMIN)이 있으면 통과 (컨트롤러가 없어 404 반환)")
    @WithMockUser(username = "admin", roles = "ORDER_ADMIN")
    void adminApi_AccessGranted() throws Exception {
        // 403이나 401이 아니면 보안 통과 의미
        mockMvc.perform(get("/order/admin/some-resource"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("URL 보안: 환불 관리자 API도 권한 제어가 적용된다")
    @WithMockUser(username = "admin", roles = "ORDER_ADMIN")
    void refundAdminApi_AccessGranted() throws Exception {
        mockMvc.perform(get("/admin/refunds/some-resource"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("URL 보안: 게스트 장바구니 경로는 인증 없이 접근 가능")
    void guestCart_AccessGranted() throws Exception {
        mockMvc.perform(get("/cart/guest/test"))
                .andExpect(status().isNotFound()); // 401, 403이 아니어야 함
    }

    @Test
    @DisplayName("URL 보안: 회원 장바구니 경로는 현재 permitAll 설정되어 있다")
    void userCart_AccessGranted() throws Exception {
        // SecurityConfig 설정상 .requestMatchers("/cart/user/**").permitAll() 상태임
        mockMvc.perform(get("/cart/user/test"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("URL 보안: 그 외 나머지 요청(anyRequest)은 permitAll 설정되어 있다")
    void anyRequest_AccessGranted() throws Exception {
        mockMvc.perform(get("/some/random/url"))
                .andExpect(status().isNotFound());
    }
}