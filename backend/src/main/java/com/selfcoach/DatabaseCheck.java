package com.selfcoach;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.r2dbc.core.R2dbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 启动时校验数据库连通（Phase 0 第 2 步）。
 * 只查 user_profile 表的行数，证明后端能连上云服务器的 PostgreSQL。
 * 若连不上，会在日志打印失败原因，方便排查连接配置。
 */
@Component
public class DatabaseCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCheck.class);

    private final R2dbcTemplate r2dbcTemplate;

    public DatabaseCheck(R2dbcTemplate r2dbcTemplate) {
        this.r2dbcTemplate = r2dbcTemplate;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        Mono<Long> count = r2dbcTemplate
                .sql("SELECT COUNT(*) FROM user_profile")
                .map((row, meta) -> row.get(0, Long.class))
                .one();

        count.subscribe(
                n -> log.info("数据库连通 OK —— user_profile 表当前共 {} 行", n),
                err -> log.error("数据库连通失败：{}", err.getMessage(), err)
        );
    }
}