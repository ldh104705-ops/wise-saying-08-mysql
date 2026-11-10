package com.back.global.app;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * DB 접속 정보. 소스에 하드코딩하지 않고 아래 순서로 읽는다.
 * 1. 환경변수 (DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD, DB_NAME, DB_TEST_NAME)
 * 2. 프로젝트 루트의 db.properties (git 에 올리지 않음, db.properties.example 참고)
 */
public record DbConfig(
        String host,
        int port,
        String username,
        String password,
        String dbName
) {
    private static final String PROPERTIES_FILE = "db.properties";

    public String getJdbcUrl() {
        return "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&connectionTimeZone=LOCAL&forceConnectionTimeZoneToSession=true"
                .formatted(host, port, dbName);
    }

    /** 개발용(prod) 또는 테스트용 DB 접속 정보를 읽는다. */
    public static DbConfig load(boolean testMode) {
        Properties props = readPropertiesFile();

        String host = get(props, "DB_HOST", "db.host", "localhost");
        int port = Integer.parseInt(get(props, "DB_PORT", "db.port", "3306"));
        String username = get(props, "DB_USERNAME", "db.username", "root");
        String password = get(props, "DB_PASSWORD", "db.password", null);
        String dbName = testMode
                ? get(props, "DB_TEST_NAME", "db.testName", "wiseSaying__test")
                : get(props, "DB_NAME", "db.name", "wiseSaying__dev");

        if (password == null) {
            throw new IllegalStateException("DB 비밀번호가 없습니다. 환경변수 DB_PASSWORD 또는 db.properties 의 db.password 를 설정하세요.");
        }

        return new DbConfig(host, port, username, password, dbName);
    }

    private static String get(Properties props, String envKey, String propKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env;

        return props.getProperty(propKey, defaultValue);
    }

    private static Properties readPropertiesFile() {
        Properties props = new Properties();
        Path path = Path.of(PROPERTIES_FILE);

        if (!Files.exists(path)) return props;

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return props;
    }
}
