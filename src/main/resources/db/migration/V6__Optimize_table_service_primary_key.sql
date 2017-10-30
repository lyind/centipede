-- rename old table
ALTER TABLE service RENAME TO service_old;

-- create table with improved layout and primary key
CREATE TABLE service (
  name TEXT NOT NULL,
  generation INTEGER NOT NULL DEFAULT 0,
  retired INTEGER NOT NULL DEFAULT 0,
  ts DATETIME NOT NULL DEFAULT(strftime('%Y-%m-%d %H:%M:%f', 'now')),
  state TEXT NOT NULL DEFAULT 'UNKNOWN',
  targetState TEXT NOT NULL DEFAULT 'UNKNOWN',
  kind TEXT,
  vmArguments INTEGER,
  image INTEGER,
  arguments INTEGER,
  route TEXT,
  proxyPathPrefix TEXT,
  pid INTEGER,
  host TEXT,
  port INTEGER,
  transition INTEGER,
  CONSTRAINT service_PK PRIMARY KEY (name, generation, retired)
) WITHOUT ROWID;

-- copy previous contents
INSERT INTO service (
  name, generation, retired, ts, state, targetState, kind, vmArguments, image, arguments, route, proxyPathPrefix, pid, host, port, transition
)
  SELECT
    name, generation, retired, ts, state, targetState, kind, vmArguments, image, arguments, route, proxyPathPrefix, pid, host, port, transition
  FROM service_old;

-- drop old table
DROP TABLE service_old;