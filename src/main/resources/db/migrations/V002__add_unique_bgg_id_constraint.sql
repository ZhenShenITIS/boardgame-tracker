ALTER TABLE board_games
    ADD CONSTRAINT uq_board_games_bgg_id UNIQUE (bgg_id);