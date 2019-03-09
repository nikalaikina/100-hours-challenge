CREATE TABLE tasks (
  id BIGINT PRIMARY KEY,
  chatId BIGINT,
  tag varchar(250),
  description varchar(1000),
  spent INTEGER
);
