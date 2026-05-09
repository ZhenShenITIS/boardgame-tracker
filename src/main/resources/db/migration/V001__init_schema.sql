CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100)        NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS roles
(
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);
CREATE TABLE IF NOT EXISTS users_roles
(
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    UNIQUE (user_id, role_id)
);
CREATE TABLE IF NOT EXISTS board_games
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bgg_id          BIGINT,
    type            VARCHAR(50)  NOT NULL    DEFAULT 'BOARDGAME',
    original_name   VARCHAR(500) NOT NULL,
    display_name    VARCHAR(500) NOT NULL,
    complexity      NUMERIC(3, 2),
    min_players     INTEGER,
    max_players     INTEGER,
    playing_time    INTEGER,
    min_play_time   INTEGER,
    max_play_time   INTEGER,
    min_age         INTEGER,
    year_published  INTEGER,
    s3_image_key    VARCHAR(500),
    s3_preview_key  VARCHAR(500),
    bgg_image_url   TEXT,
    bgg_preview_url TEXT,
    is_custom       BOOLEAN      NOT NULL    DEFAULT FALSE,
    created_by      BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS tags
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS board_games_tags
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_game_id BIGINT NOT NULL REFERENCES board_games (id) ON DELETE CASCADE,
    tag_id        BIGINT NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    UNIQUE (board_game_id, tag_id)
);
CREATE TABLE IF NOT EXISTS collection_items
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    board_game_id  BIGINT      NOT NULL REFERENCES board_games (id) ON DELETE RESTRICT,
    date_purchased DATE,
    sum_in_rubles  DECIMAL(10, 2),
    status         VARCHAR(50) NOT NULL     DEFAULT 'IN_COLLECTION',
    play_count     INTEGER     NOT NULL     DEFAULT 0,
    comment        TEXT,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS play_sessions
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collection_item_id BIGINT                   NOT NULL REFERENCES collection_items (id) ON DELETE CASCADE,
    date_start         TIMESTAMP WITH TIME ZONE NOT NULL,
    date_end           TIMESTAMP WITH TIME ZONE,
    comment            TEXT,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT                   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255)             NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE          DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   BIGINT,
    details     JSONB,
    ip_address  INET,
    user_agent  TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX IF NOT EXISTS idx_board_games_bgg_id ON board_games (bgg_id);
CREATE INDEX IF NOT EXISTS idx_board_games_is_custom_created_by ON board_games (is_custom, created_by);

CREATE INDEX IF NOT EXISTS idx_board_games_display_name_trgm ON board_games USING GIN (display_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_board_games_original_name_trgm ON board_games USING GIN (original_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_collection_items_user_id ON collection_items (user_id);
CREATE INDEX IF NOT EXISTS idx_collection_items_board_game_id ON collection_items (board_game_id);
CREATE INDEX IF NOT EXISTS idx_collection_items_status ON collection_items (status);
CREATE INDEX IF NOT EXISTS idx_collection_items_user_play_count ON collection_items (user_id, play_count) WHERE play_count = 0;

CREATE INDEX IF NOT EXISTS idx_play_sessions_collection_item_id ON play_sessions (collection_item_id);
CREATE INDEX IF NOT EXISTS idx_play_sessions_date_start ON play_sessions (date_start);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at);


CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_board_games_updated_at ON board_games;
CREATE TRIGGER update_board_games_updated_at
    BEFORE UPDATE
    ON board_games
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


DROP TRIGGER IF EXISTS update_collection_items_updated_at ON collection_items;
CREATE TRIGGER update_collection_items_updated_at
    BEFORE UPDATE
    ON collection_items
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


DROP TRIGGER IF EXISTS update_play_sessions_updated_at ON play_sessions;
CREATE TRIGGER update_play_sessions_updated_at
    BEFORE UPDATE
    ON play_sessions
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


CREATE OR REPLACE FUNCTION update_collection_item_play_count()
    RETURNS TRIGGER AS
$$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE collection_items
        SET play_count = play_count + 1
        WHERE id = NEW.collection_item_id;
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE collection_items
        SET play_count = play_count - 1
        WHERE id = OLD.collection_item_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_play_count_trigger ON play_sessions;
CREATE TRIGGER update_play_count_trigger
    AFTER INSERT OR DELETE
    ON play_sessions
    FOR EACH ROW
EXECUTE FUNCTION update_collection_item_play_count();