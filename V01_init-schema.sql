CREATE DATABASE online_tracker;

\c online_tracker;

CREATE TABLE character (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR UNIQUE,
  created TIMESTAMPTZ,
  current_name_since TIMESTAMPTZ
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

CREATE INDEX online_history_character_id ON online_history(character_id);
CREATE INDEX online_history_logout_time ON online_history(logout_time);
CREATE INDEX online_history_login_time ON online_history(login_time);

-- Insert initial data

INSERT INTO world
  (id, name) VALUES
  (DEFAULT, 'Nefera');
