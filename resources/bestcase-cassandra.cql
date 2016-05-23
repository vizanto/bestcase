CREATE KEYSPACE bestcase WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': 3
};

CREATE TABLE bestcase.tests (
	test_name         text,
	active            boolean,
	descriptor        blob,
	PRIMARY KEY (test_name)
);
CREATE INDEX activeIndex ON bestcase.tests (active);


CREATE TABLE bestcase.user_alternatives (
	test_name         text,
	test_i            text,
	alternative_name  text,
	PRIMARY KEY (test_name, test_i)
);
CREATE TABLE bestcase.counts (
	test_name         text,
	alternative_name  text,
	count             counter,
	score             counter,
	PRIMARY KEY (test_name, alternative_name)
);
CREATE TABLE bestcase.user_scores (
	test_name         text,
	goal_name         text,
	test_i            text,
	score             counter,
	PRIMARY KEY (test_name, goal_name, test_i)
);
