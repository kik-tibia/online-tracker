DROP DATABASE test1;

CREATE DATABASE test1;

\c test1;

CREATE TABLE character (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR UNIQUE,
  created TIMESTAMPTZ
);

CREATE TABLE character_name_history (
  id BIGSERIAL PRIMARY KEY,
  character_id BIGINT REFERENCES character(id),
  name VARCHAR,
  from_date TIMESTAMPTZ,
  until_date TIMESTAMPTZ
);

CREATE TABLE world (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR
);

CREATE TABLE world_save_time (
  id BIGSERIAL PRIMARY KEY,
  world_id BIGINT REFERENCES world(id),
  sequence_id BIGINT,
  time TIMESTAMPTZ
);

CREATE TABLE currently_online (
  id BIGSERIAL PRIMARY KEY,
  character_id BIGINT REFERENCES character(id),
  world_id BIGINT REFERENCES world(id),
  login_time BIGINT REFERENCES world_save_time(id)
);

CREATE TABLE online_history (
  id BIGSERIAL PRIMARY KEY,
  character_id BIGINT REFERENCES character(id),
  login_time BIGINT REFERENCES world_save_time(id),
  logout_time BIGINT REFERENCES world_save_time(id)
);


-- Insert some test data

INSERT INTO character
  (id, name, created) VALUES
  (DEFAULT, 'Test One', NOW() - INTERVAL '48 hours'),
  (DEFAULT, 'Test Two', NOW() - INTERVAL '8 hours');

INSERT INTO character_name_history
  (id, character_id, name, from_date, until_date) VALUES
  (DEFAULT, 1, 'Test', NOW() - INTERVAL '48 hours', NOW() - INTERVAL '24 hours');

INSERT INTO world
  (id, name) VALUES
  (DEFAULT, 'Testworld1'),
  (DEFAULT, 'Testworld2');
  (DEFAULT, 'Nefera');

INSERT INTO world_save_time
  (id, world_id, sequence_id, time) VALUES
  (DEFAULT, 1, 1, NOW() - INTERVAL '8 hours'),
  (DEFAULT, 2, 1, NOW() - INTERVAL '8 hours'),
  (DEFAULT, 1, 2, NOW() - INTERVAL '7 hours'),
  (DEFAULT, 2, 2, NOW() - INTERVAL '7 hours'),
  (DEFAULT, 1, 3, NOW() - INTERVAL '6 hours'),
  (DEFAULT, 2, 3, NOW() - INTERVAL '6 hours'),
  (DEFAULT, 1, 4, NOW() - INTERVAL '5 hours'),
  (DEFAULT, 2, 4, NOW() - INTERVAL '5 hours'),
  (DEFAULT, 1, 5, NOW() - INTERVAL '4 hours'),
  (DEFAULT, 2, 5, NOW() - INTERVAL '4 hours'),
  (DEFAULT, 1, 6, NOW() - INTERVAL '3 hours'),
  (DEFAULT, 2, 6, NOW() - INTERVAL '3 hours'),
  (DEFAULT, 1, 7, NOW() - INTERVAL '2 hours'),
  (DEFAULT, 2, 7, NOW() - INTERVAL '2 hours'),
  (DEFAULT, 1, 8, NOW() - INTERVAL '1 hours'),
  (DEFAULT, 2, 8, NOW() - INTERVAL '1 hours'),
  (DEFAULT, 1, 9, NOW()),
  (DEFAULT, 2, 9, NOW());

INSERT INTO currently_online
  (id, character_id, world_id, login_time) VALUES
  (DEFAULT, 2, 1, 15);

INSERT INTO online_history
  (id, character_id, login_time, logout_time) VALUES
  (DEFAULT, 1, 1, 3),
  (DEFAULT, 1, 5, 9),
  (DEFAULT, 2, 11, 13);

