CREATE TABLE people (
  userId BIGINT PRIMARY KEY,
  description varchar(1000),
  contact varchar(1000)
);

CREATE TABLE likes (
  userId BIGINT,
  person BIGINT,
  likes BOOLEAN,
  PRIMARY KEY(userId, person)
);
