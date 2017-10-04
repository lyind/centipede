CREATE TABLE dependency (
	source TEXT NOT NULL,
	target TEXT NOT NULL,
	CONSTRAINT dependency_PK PRIMARY KEY (source, target)
) WITHOUT ROWID;