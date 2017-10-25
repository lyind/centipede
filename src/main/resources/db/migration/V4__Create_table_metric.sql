-- Stores metric path, example: "api.example.com/authentication-service/statusCode"
CREATE TABLE metric_path (
  id INTEGER PRIMARY KEY NOT NULL,  -- alias for rowid
	path TEXT NOT NULL
);

-- Index path
CREATE UNIQUE INDEX idx_path  ON metric_path (path);

-- Stores actual metrics as floating point values
CREATE TABLE metric (
	-- ts is the nanoseconds since the epoch with millisecond precision (the micro- and nanosecond part is random)
	ts INTEGER NOT NULL DEFAULT(CAST(((julianday('now') - 2440587.5) * 86400000000000) + abs(random() % 1000000) AS INTEGER)),
	pathId INTEGER NOT NULL,
	value REAL NOT NULL,
	CONSTRAINT metric_PK PRIMARY KEY (ts)
) WITHOUT ROWID;

-- Index for lookups by pathId
CREATE INDEX idx_metric_path_id ON metric (pathId);