package com.selfcoach;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

/**
 * 启动时校验数据库连通（Phase 0 第 2 步）。
 * 只查 user_profile 表的行数，证明后端能连上云服务器的 PostgreSQL。
 * 用 DatabaseClient（Spring 提供的响应式查询入口）；连不上会在日志打失败原因。
 */
@Component
public class DatabaseCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCheck.class);

    private final DatabaseClient client;

    public DatabaseCheck(DatabaseClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) {
        client.sql("SELECT COUNT(*) FROM user_profile")
                .map((row, meta) -> row.get(0, Long.class))
                .one()
                .subscribe(
                        n -> log.info("数据库连通 OK —— user_profile 表当前共 {} 行", n),
                        err -> log.error("数据库连通失败：{}", err.getMessage(), err)
                );
    }
}