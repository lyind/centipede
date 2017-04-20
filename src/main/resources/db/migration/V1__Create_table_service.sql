CREATE TABLE service (
	generation INTEGER NOT NULL,
	name TEXT NOT NULL,
	retired INTEGER NOT NULL DEFAULT 0,
	ts DATETIME NOT NULL DEFAULT(strftime('%Y-%m-%dT%H:%M:%f', 'now')),
	state TEXT NOT NULL,
	targetState TEXT NOT NULL,
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