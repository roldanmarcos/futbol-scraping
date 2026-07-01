package com.futbol.scraping.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConditionalOnProperty("DATABASE_URL")
    public DataSource dataSource(Environment env) {
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (!databaseUrl.startsWith("jdbc:")) {
            databaseUrl = "jdbc:" + databaseUrl;
        }
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(databaseUrl)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
