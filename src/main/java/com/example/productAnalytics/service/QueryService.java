package com.example.productAnalytics.service;

import com.example.productAnalytics.cache.SimpleCache;
import com.example.productAnalytics.factory.CacheFactory;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.sql.ResultSetMetaData;
import java.util.*;

@Service
public class QueryService {
    private final RestClient llm;
    private final JdbcTemplate jdbcTemplate;
    private final SimpleCache<String, Object> cache;

    public QueryService(RestClient llm, JdbcTemplate jdbcTemplate, CacheFactory cacheFactory) {
        this.llm = llm;
        this.jdbcTemplate = jdbcTemplate;
        this.cache = cacheFactory.get("cache" + getClass().getSimpleName());
    }

    @Value("${spring.llm.model}")
    private String model;

    private static final String CONTEXT = """
            You are a SQL generator for my PostgreSQL database.
            
            Schema:
            1) product_bangalore_hyderabad(id UUID, product_id VARCHAR, name VARCHAR, price DOUBLE, description TEXT, image VARCHAR, created TIMESTAMP)
            2) product_view_bangalore_hyderabad(id UUID, product_id VARCHAR, user_ip VARCHAR, view_count INT, last_updated TIMESTAMP)
            Join:
             - product_bangalore_hyderabad.product_id = product_view_bangalore_hyderabad.product_id
            
            STRICT OUTPUT RULES (mandatory):
             - Return EXACTLY ONE SQL query only.
             - Do NOT return multiple SQL statements.
             - Do NOT use more than one semicolon. (Prefer zero semicolons or only one at the end.)
             - Do NOT include explanations, comments, markdown, or extra text.
             - If the request needs multiple results, use JOINs / subqueries / CTEs to keep it ONE statement.
             - Return SQL in a single line. Do NOT include \\n.
            
            Return only the SQL.
            """;

    public String generate(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", "\"" + prompt + "\"\n" + CONTEXT))
        );

        Map res = llm.post()
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) res.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        return (String) message.get("content");
    }

    public List<Map<String, Object>> execute(String sql) {
        if (!isSafeDql(sql)) return List.of();

        Object cached = cache.get(sql).join();
        if (cached != null)
            return (List<Map<String, Object>>) cached;

        List<Map<String, Object>> rows = jdbcTemplate.query(sql, rs -> {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            List<String> colNames = new ArrayList<>(cols);
            for (int i = 1; i <= cols; i++) {
                colNames.add(meta.getColumnLabel(i));
            }

            List<Map<String, Object>> result = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(colNames.get(i - 1), rs.getObject(i));
                }
                result.add(row);
            }
            return result;
        });

        cache.put(sql, rows).join();
        return rows;
    }

    public Set<String> history() {
        return cache.getCache().keySet();
    }

    public static boolean isSafeDql(String sql) {
        Statement stmt = null;
        try {
            stmt = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new RuntimeException(e);
        }

        if (!(stmt instanceof Select)) return false;

        Select select = (Select) stmt;

        String normalized = select.toString().toUpperCase();

        return !normalized.contains("INTO") && !normalized.contains("FOR UPDATE");
    }
}
