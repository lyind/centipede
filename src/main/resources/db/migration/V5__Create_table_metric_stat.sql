-- Stores some accumulated metrics
CREATE TABLE metric_stat (
  pathPrefix TEXT NOT NULL,
  begin INTEGER NOT NULL,
  end INTEGER NOT NULL,
  total INTEGER NOT NULL,
  minTime REAL NOT NULL,
  avgTime REAL NOT NULL,
  maxTime REAL NOT NULL,
  status2xx REAL NOT NULL,
  status3xx REAL NOT NULL,
  status4xx REAL NOT NULL,
  status5xx REAL NOT NULL,
  maxHeap REAL NOT NULL,  -- Heap MB
  maxNonHeap REAL NOT NULL,  -- Non-Heap MB (buffers, metaspace...)
  CONSTRAINT metric_stat_PK PRIMARY KEY (pathPrefix, begin)
) WITHOUT ROWID;