CREATE TABLE IF NOT EXISTS road_continuation
(
    file_hash  VARCHAR PRIMARY KEY,
    bucket_key TEXT,
    status     progression_status NOT NULL DEFAULT 'PROCESSING'
);
