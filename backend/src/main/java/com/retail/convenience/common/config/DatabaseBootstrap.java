package com.retail.convenience.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 首次启动执行建表与演示数据；已初始化库只执行幂等 DDL，避免每次重启重复插入种子数据。
 */
@Slf4j
@Component
public class DatabaseBootstrap implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseBootstrap(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean fresh;
        try {
            Integer count = jdbcTemplate.queryForObject("select count(*) from platform_tenant", Integer.class);
            fresh = count == null || count == 0;
        } catch (Exception e) {
            fresh = true;
        }
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/schema.sql"));
            if (fresh) {
                log.info("检测到空库，正在写入演示数据...");
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/data.sql"));
                log.info("演示数据初始化完成");
            } else {
                log.info("数据库已初始化，跳过演示数据");
            }
        }
    }
}
