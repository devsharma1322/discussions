package com.anoncircles.discussions.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * Datasource & JdbcTemplate config.
 *
 * <p>HikariCP and {@link JdbcTemplate}/{@link NamedParameterJdbcTemplate} beans are
 * auto-configured by Spring Boot from {@code spring.datasource.*} in application.yml.
 * This class exists so all datasource concerns (pool sizing, slow-query logging,
 * future read-replica routing) live in one obvious place.
 */
@Slf4j
@Configuration
public class DatasourceConfig {

    private final DataSource dataSource;

    public DatasourceConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    void logPoolInfo() {
        if (dataSource instanceof HikariDataSource hikari) {
            log.info("HikariCP pool='{}' maxPoolSize={} jdbcUrl={}",
                    hikari.getPoolName(),
                    hikari.getMaximumPoolSize(),
                    hikari.getJdbcUrl());
        }
    }
}
