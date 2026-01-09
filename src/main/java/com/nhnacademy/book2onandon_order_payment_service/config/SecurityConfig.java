package com.nhnacademy.book2onandon_order_payment_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@SuppressWarnings("java:S4502")
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * PasswordEncoder – BCrypt 사용
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security HTTP 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // CSRF 비활성화 (API 서버)
                .csrf(csrf -> csrf.disable())

                // Basic 인증 활성화 (WWW-Authenticate: Basic 헤더 출력)
                .httpBasic(Customizer.withDefaults())

                // ==== URL 접근 제어 시작 ====
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/order/admin/**").hasRole("ORDER_ADMIN")
                        .requestMatchers("/admin/refunds/**").hasRole("ORDER_ADMIN")
                        .anyRequest().permitAll()

                );
        // ===== URL 접근 제어 끝 =====

        return http.build();
    }
}
