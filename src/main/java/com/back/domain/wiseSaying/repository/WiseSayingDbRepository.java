package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.global.app.DbConfig;
import com.back.standard.dto.Page;
import com.back.standard.util.JsonUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL 저장소 (JDBC).
 * DB 접근 코드(JDBC·SQL)는 이 리포지터리에만 있다.
 * 검색·페이징은 SQL(WHERE LIKE, ORDER BY id DESC, LIMIT/OFFSET)로 처리한다.
 */
public class WiseSayingDbRepository implements WiseSayingRepository {
    private final DbConfig dbConfig;
    private final String buildFilePath;

    public WiseSayingDbRepository(DbConfig dbConfig, String buildFilePath) {
        this.dbConfig = dbConfig;
        this.buildFilePath = buildFilePath;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbConfig.getJdbcUrl(), dbConfig.username(), dbConfig.password());
    }

    // ===== 스키마 (sql/schema.sql 과 같은 내용) =====

    public void createTableIfNotExists() {
        update("""
                CREATE TABLE IF NOT EXISTS wise_saying (
                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    createDate DATETIME NOT NULL,
                    modifyDate DATETIME NOT NULL,
                    content TEXT NOT NULL,
                    author VARCHAR(100) NOT NULL,
                    PRIMARY KEY (id)
                ) DEFAULT CHARSET = utf8mb4
                """);
    }

    public void truncate() {
        update("TRUNCATE wise_saying");
    }

    // ===== CRUD =====

    @Override
    public WiseSaying save(WiseSaying wiseSaying) {
        if (wiseSaying.isNew()) {
            long id = insert(
                    "INSERT INTO wise_saying SET createDate = NOW(), modifyDate = NOW(), content = ?, author = ?",
                    wiseSaying.getContent(), wiseSaying.getAuthor()
            );
            wiseSaying.setId((int) id);
        } else {
            update(
                    "UPDATE wise_saying SET modifyDate = NOW(), content = ?, author = ? WHERE id = ?",
                    wiseSaying.getContent(), wiseSaying.getAuthor(), wiseSaying.getId()
            );
        }

        return wiseSaying;
    }

    @Override
    public List<WiseSaying> findAll() {
        return select("SELECT id, content, author FROM wise_saying ORDER BY id ASC");
    }

    @Override
    public Page<WiseSaying> findForList(String keywordType, String keyword, int page, int pageSize) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            switch (keywordType) {
                case "content" -> where.append(" WHERE content LIKE CONCAT('%', ?, '%')");
                case "author" -> where.append(" WHERE author LIKE CONCAT('%', ?, '%')");
                default -> {
                    where.append(" WHERE (content LIKE CONCAT('%', ?, '%') OR author LIKE CONCAT('%', ?, '%'))");
                    params.add(keyword);
                }
            }
            params.add(keyword);
        }

        int totalCount = (int) selectLong("SELECT COUNT(*) FROM wise_saying" + where, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((page - 1) * pageSize);

        List<WiseSaying> content = select(
                "SELECT id, content, author FROM wise_saying" + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                pageParams.toArray()
        );

        return new Page<>(content, totalCount, page, pageSize);
    }

    @Override
    public int count() {
        return (int) selectLong("SELECT COUNT(*) FROM wise_saying");
    }

    @Override
    public Optional<WiseSaying> findById(int id) {
        return select("SELECT id, content, author FROM wise_saying WHERE id = ?", id)
                .stream()
                .findFirst();
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM wise_saying WHERE id = ?", id) > 0;
    }

    @Override
    public void build() {
        JsonUtil.writeFile(buildFilePath, JsonUtil.toJsonArray(findAll().stream().map(WiseSaying::toMap).toList()));
    }

    // ===== JDBC =====

    private List<WiseSaying> select(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                List<WiseSaying> list = new ArrayList<>();

                while (rs.next()) {
                    list.add(new WiseSaying(rs.getInt("id"), rs.getString("content"), rs.getString("author")));
                }

                return list;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL 실행 실패: " + sql, e);
        }
    }

    private long selectLong(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL 실행 실패: " + sql, e);
        }
    }

    private long insert(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(stmt, params);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL 실행 실패: " + sql, e);
        }
    }

    private int update(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("SQL 실행 실패: " + sql, e);
        }
    }

    private static void bind(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}
