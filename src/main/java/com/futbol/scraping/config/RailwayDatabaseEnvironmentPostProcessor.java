package com.futbol.scraping.config;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class RailwayDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "railwayDatabaseOverrides";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> overrides = new HashMap<>();

        String datasourceUrl = resolveDatasourceUrl(environment);
        if (StringUtils.hasText(datasourceUrl)) {
            overrides.put("spring.datasource.url", datasourceUrl);
        }

        String username = resolveDatasourceUsername(environment);
        if (StringUtils.hasText(username)) {
            overrides.put("spring.datasource.username", username);
        }

        String password = resolveDatasourcePassword(environment);
        if (StringUtils.hasText(password)) {
            overrides.put("spring.datasource.password", password);
        }

        if (StringUtils.hasText(datasourceUrl)) {
            overrides.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveDatasourceUrl(ConfigurableEnvironment environment) {
        String springDatasourceUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        if (StringUtils.hasText(springDatasourceUrl)) {
            return springDatasourceUrl;
        }

        String jdbcDatabaseUrl = environment.getProperty("JDBC_DATABASE_URL");
        if (StringUtils.hasText(jdbcDatabaseUrl)) {
            return jdbcDatabaseUrl;
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (!StringUtils.hasText(databaseUrl)) {
            return null;
        }

        if (databaseUrl.startsWith("jdbc:")) {
            return databaseUrl;
        }

        return toJdbcPostgresUrl(databaseUrl);
    }

    private String resolveDatasourceUsername(ConfigurableEnvironment environment) {
        String username = environment.getProperty("SPRING_DATASOURCE_USERNAME");
        if (StringUtils.hasText(username)) {
            return username;
        }

        username = environment.getProperty("JDBC_DATABASE_USERNAME");
        if (StringUtils.hasText(username)) {
            return username;
        }

        username = environment.getProperty("PGUSER");
        if (StringUtils.hasText(username)) {
            return username;
        }

        username = extractUserInfo(environment.getProperty("DATABASE_URL"), true);
        if (StringUtils.hasText(username)) {
            return username;
        }

        return environment.getProperty("DB_USER");
    }

    private String resolveDatasourcePassword(ConfigurableEnvironment environment) {
        String password = environment.getProperty("SPRING_DATASOURCE_PASSWORD");
        if (StringUtils.hasText(password)) {
            return password;
        }

        password = environment.getProperty("JDBC_DATABASE_PASSWORD");
        if (StringUtils.hasText(password)) {
            return password;
        }

        password = environment.getProperty("PGPASSWORD");
        if (StringUtils.hasText(password)) {
            return password;
        }

        password = extractUserInfo(environment.getProperty("DATABASE_URL"), false);
        if (StringUtils.hasText(password)) {
            return password;
        }

        return environment.getProperty("DB_PASSWORD");
    }

    private String toJdbcPostgresUrl(String databaseUrl) {
        try {
            URI uri = new URI(databaseUrl);
            String scheme = uri.getScheme();
            if (!"postgres".equalsIgnoreCase(scheme) && !"postgresql".equalsIgnoreCase(scheme)) {
                return databaseUrl;
            }

            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();

            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://");
            jdbcUrl.append(host == null ? "localhost" : host);
            if (port > 0) {
                jdbcUrl.append(':').append(port);
            }
            if (StringUtils.hasText(path)) {
                jdbcUrl.append(path);
            }
            if (StringUtils.hasText(query)) {
                jdbcUrl.append('?').append(query);
            }

            return jdbcUrl.toString();
        } catch (URISyntaxException ex) {
            return databaseUrl;
        }
    }

    private String extractUserInfo(String databaseUrl, boolean username) {
        if (!StringUtils.hasText(databaseUrl)) {
            return null;
        }

        try {
            URI uri = new URI(databaseUrl);
            String userInfo = uri.getUserInfo();
            if (!StringUtils.hasText(userInfo)) {
                return null;
            }

            String[] parts = userInfo.split(":", 2);
            if (username) {
                return decode(parts[0]);
            }

            if (parts.length < 2) {
                return null;
            }

            return decode(parts[1]);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String decode(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}