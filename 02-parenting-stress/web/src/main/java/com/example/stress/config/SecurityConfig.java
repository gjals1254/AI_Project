package com.example.stress.config;

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
 * 정신건강 관련 민감정보를 다루므로 메인/로그인 외 모든 화면은 로그인 필요.
 *  - COUNSELOR(상담자) : 평가 등록·조회
 *  - ADMIN            : 통계, 모델 관리, 데이터 삭제
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**", "/error").permitAll()
                .requestMatchers("/admin/**", "/assessments/*/delete").hasRole("ADMIN")
                .anyRequest().authenticated())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .httpBasic(basic -> { })        // /api/** 를 Postman 에서 Basic 인증으로 호출 가능
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
                .roles("ADMIN", "COUNSELOR").build(),
            User.withUsername(s.getUserUsername()).password(encoder.encode(s.getUserPassword()))
                .roles("COUNSELOR").build());
    }
}
