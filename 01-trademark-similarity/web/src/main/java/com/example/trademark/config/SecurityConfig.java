package com.example.trademark.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 권한 정책
 *  - 누구나     : 메인, 상표 목록/상세, 유사 상표 검색
 *  - USER 이상  : 상표 등록/수정/삭제
 *  - ADMIN      : /admin/** (데이터 관리, 검색 이력, 시스템 운영)
 * 학습용으로 계정은 메모리에 둔다. 운영 시 DB 사용자 테이블로 교체한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/trademarks/new", "/trademarks/*/edit", "/trademarks/*/delete")
                    .hasAnyRole("USER", "ADMIN")
                .anyRequest().permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppProperties props, PasswordEncoder encoder) {
        AppProperties.Security s = props.getSecurity();
        return new InMemoryUserDetailsManager(
            User.withUsername(s.getAdminUsername())
                .password(encoder.encode(s.getAdminPassword()))
                .roles("ADMIN", "USER").build(),
            User.withUsername(s.getUserUsername())
                .password(encoder.encode(s.getUserPassword()))
                .roles("USER").build());
    }
}
