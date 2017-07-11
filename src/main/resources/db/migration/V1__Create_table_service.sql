CREATE TABLE service (
	generation INTEGER NOT NULL DEFAULT 0,
	name TEXT NOT NULL,
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
	CONSTRAINT service_PK PRIMARY KEY (generation, name)
) WITHOUT ROWID;