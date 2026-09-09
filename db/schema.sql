-- ============================================================
-- SelfCoach 数据库结构（Phase 0）
-- 适用：PostgreSQL 16 + pgvector
-- 执行：psql -U selfcoach -d selfcoach -f db/schema.sql
-- ============================================================

-- 1) 启用向量扩展（pgvector）
CREATE EXTENSION IF NOT EXISTS vector;

-- 2) 用户画像（P0 单用户先一行；P2 多用户/登录后仍复用它）
CREATE TABLE IF NOT EXISTS user_profile (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    goal          VARCHAR(20) NOT NULL,               -- 目标：增肌 / 减脂 / 力量
    days_per_week SMALLINT    NOT NULL DEFAULT 3,     -- 每周可训练天数
    height_cm     NUMERIC(5,1),                       -- 身高(cm)，可选
    injury_tags   TEXT[]      NOT NULL DEFAULT '{}',  -- 伤病标签，如 {左膝旧伤}
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3) 训练日志（F-01）
CREATE TABLE IF NOT EXISTS training_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES user_profile(id),
    workout_date DATE         NOT NULL,               -- 训练日期
    exercise     VARCHAR(100) NOT NULL,               -- 动作名，如「杠铃卧推」
    weight_kg    NUMERIC(6,2),                        -- 重量(kg)；自重动作可为 NULL
    reps         INT          NOT NULL,               -- 每组次数
    sets         INT          NOT NULL DEFAULT 1,     -- 组数
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 查「某段日期的训练」是高频操作，建 (user_id, workout_date) 组合索引
CREATE INDEX IF NOT EXISTS idx_training_log_user_date
    ON training_log (user_id, workout_date);

-- 4) 身体指标（F-02，体重/围度趋势）
CREATE TABLE IF NOT EXISTS body_metric (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES user_profile(id),
    measure_date DATE         NOT NULL,               -- 测量日期
    weight_kg    NUMERIC(5,2),                        -- 体重(kg)
    chest_cm     NUMERIC(5,1),                        -- 胸围(cm)
    waist_cm     NUMERIC(5,1),                        -- 腰围(cm)
    hip_cm       NUMERIC(5,1),                        -- 臀围(cm)
    arm_cm       NUMERIC(5,1),                        -- 臂围(cm)
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, measure_date)                    -- 每天只存一条身体指标
);

-- 5) 知识库分块（F-03，RAG 的存储层）
CREATE TABLE IF NOT EXISTS doc_chunk (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    doc_id      VARCHAR(128) NOT NULL,                -- 文档唯一标识
    chunk_index INT          NOT NULL,                -- 分块序号（同一文档内从 0 递增）
    content     TEXT         NOT NULL,                -- 分块文本
    embedding   vector(1024),                         -- 文本向量；维度必须与 Embedding 模型一致（此处按 1024，如 Qwen / bge-m3）
    source      VARCHAR(500),                         -- 来源：文件名或 URL
    metadata    JSONB        NOT NULL DEFAULT '{}',   -- 附加信息：标题、章节等
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (doc_id, chunk_index)
);

-- 向量相似度检索索引（HNSW + 余弦距离，pgvector >= 0.5）
CREATE INDEX IF NOT EXISTS idx_doc_chunk_embedding
    ON doc_chunk USING hnsw (embedding vector_cosine_ops);

-- 注：关键词检索（BM25）由 Lucene 的倒排索引负责，不在此建 PG 全文索引。

-- 6) P0 单用户，首次使用前先插入一行画像（按你的实际情况改 goal / days_per_week）
-- INSERT INTO user_profile (goal, days_per_week, height_cm)
-- VALUES ('增肌', 4, NULL);