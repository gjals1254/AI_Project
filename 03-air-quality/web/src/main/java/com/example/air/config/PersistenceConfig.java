package com.example.air.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 프로파일별 영속 계층 활성화.
 * - jpa     : repository.jpa 패키지의 Spring Data JPA 리포지토리만 스캔
 * - mybatis : repository.mybatis 패키지의 @Mapper 인터페이스만 스캔
 * 서비스 계층은 TrademarkStore / SearchHistoryStore 인터페이스에만 의존하므로
 * 프로파일만 바꾸면 코드 수정 없이 두 방식이 교체된다.
 */
public class PersistenceConfig {

    @Configuration
    @Profile("jpa")
    @EnableJpaRepositories(basePackages = "com.example.air.repository.jpa")
    public static class JpaConfig {
    }

    @Configuration
    @Profile("mybatis")
    @MapperScan(basePackages = "com.example.air.repository.mybatis")
    public static class MybatisConfig {
    }
}
