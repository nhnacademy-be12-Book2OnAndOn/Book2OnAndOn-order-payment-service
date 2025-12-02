package com.nhnacademy.Book2OnAndOn_order_payment_service.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // ⚠️ @EnableWebSecurity가 사용된다고 가정
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 가장 널리 사용되는 BCrypt 구현체를 반환합니다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // 1. 테스트 사용자 (주문 생성 및 조회용)
        UserDetails user = User.builder()
                .username("test_user")
                .password(passwordEncoder.encode("password")) // 비밀번호를 "password"로 가정
                .roles("USER")
                .build();

        // 2. 해커/다른 사용자 (권한 없음 검증용)
        UserDetails hacker = User.builder()
                .username("hacker_user")
                .password(passwordEncoder.encode("password")) // 비밀번호를 "password"로 가정
                .roles("USER")
                .build();

        // 3. 관리자 (Admin API용)
        UserDetails admin = User.builder()
                .username("admin_user")
                .password(passwordEncoder.encode("password"))
                .roles("ORDER_ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, hacker, admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ... (기존 인증 설정)
                .csrf(csrf -> csrf.disable()) //  CSRF 보호 기능을 비활성화합니다.
                // ... (나머지 설정)

                // Basic 인증 활성화 (이것 때문에 WWW-Authenticate: Basic이 응답됨)
                .httpBasic(Customizer.withDefaults());

        // ... (API 권한 설정)

        return http.build();
    }
}