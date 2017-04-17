CREATE TABLE service (
	generation INTEGER NOT NULL,
	name TEXT NOT NULL,
	isRetired INTEGER NOT NULL DEFAULT 0,
	ts DATETIME NOT NULL DEFAULT('NOW'),
	kind TEXT NOT NULL,
	state TEXT NOT NULL,
	targetState TEXT NOT NULL,
	vmArguments INTEGER,
	image INTEGER,
	arguments INTEGER,
	route TEXT,
	proxyPathPrefix TEXT,
	pid INTEGER,
	socketAddress TEXT,
	CONSTRAINT service_PK PRIMARY KEY (generation, name)
) WITHOUT ROWID;