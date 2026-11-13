package com.example.air.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 *  - 누구나   : 대시보드, 측정소 상세, 예측 이력 조회, 예측 API
 *  - OPERATOR : 측정 데이터 등록 (등록 시 자동 예측)
 *  - ADMIN    : 측정소 관리, CSV 업로드, 데이터 삭제, 모델 재학습
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**", "/measurements/*/delete").hasRole("ADMIN")
                .requestMatchers("/measurements/new", "/measurements/*/repredict").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/measurements").hasAnyRole("OPERATOR", "ADMIN")
                .anyRequest().permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .httpBasic(basic -> { })
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", false).permitAll())
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
            User.withUsername(s.getAdminUsername()).password(encoder.encode(s.getAdminPassword()))
                .roles("ADMIN", "OPERATOR").build(),
            User.withUsername(s.getUserUsername()).password(encoder.encode(s.getUserPassword()))
                .roles("OPERATOR").build());
    }
}
