-- Stores metric path, example: "api.example.com/authentication-service/statusCode"
CREATE TABLE metric_path (
  id INTEGER PRIMARY KEY NOT NULL,  -- alias for rowid
	path TEXT NOT NULL UNIQUE
);

-- Stores actual metrics as floating point values
CREATE TABLE metric (
	pathId INTEGER NOT NULL,
	ts INTEGER NOT NULL DEFAULT(replace(strftime('%Y%m%d%H%M%f', 'now'), '.', '')),
	value REAL NOT NULL,
	CONSTRAINT metric_PK PRIMARY KEY (pathId, ts, value) -- we would really need value, but ts isn't unique
) WITHOUT ROWID;