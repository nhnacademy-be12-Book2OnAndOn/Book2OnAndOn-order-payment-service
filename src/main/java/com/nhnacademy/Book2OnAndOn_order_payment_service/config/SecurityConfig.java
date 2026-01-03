package com.nhnacademy.Book2OnAndOn_order_payment_service.config;

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
     * 테스트 전용 InMemory 사용자 생성
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {

        UserDetails user = User.builder()
                .username("test_user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();

        UserDetails hacker = User.builder()
                .username("hacker_user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin_user")
                .password(passwordEncoder.encode("password"))
                .roles("ORDER_ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, hacker, admin);
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

                        .requestMatchers("/cart/guest/**").permitAll()
//                        .requestMatchers("/cart/user/**").authenticated()
                        .requestMatchers("/cart/user/**").permitAll()
                        .requestMatchers("/order/admin/**").hasRole("ORDER_ADMIN")
                        .requestMatchers("/admin/refunds/**").hasRole("ORDER_ADMIN")
                        .anyRequest().permitAll()

                );
        // ===== URL 접근 제어 끝 =====

        return http.build();
    }
}
