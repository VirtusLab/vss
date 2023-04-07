create table hashed_passwords(
  uuid UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  password VARCHAR NOT NULL,
  hash_type VARCHAR NOT NULL,
  password_hash VARCHAR NOT NULL,
  UNIQUE (hash_type, password)
);

create table pwned(
  uuid UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  email VARCHAR NOT NULL UNIQUE,
  breaches_no INT NOT NULL
);

-- Populate some data to pwned

INSERT INTO pwned(email, breaches_no) VALUES ('sherlock.holmes@gmail.com', 2);
INSERT INTO pwned(email, breaches_no) VALUES ('joan.watson@yahoo.com', 1);
INSERT INTO pwned(email, breaches_no) VALUES ('marcus.bell@hotmail.com', 3);
INSERT INTO pwned(email, breaches_no) VALUES ('jamie.moriarty@gmail.com', 7);
INSERT INTO pwned(email, breaches_no) VALUES ('michael.rowan@gmail.com', 1);
INSERT INTO pwned(email, breaches_no) VALUES ('kitty.winter@yahoo.com', 2);
INSERT INTO pwned(email, breaches_no) VALUES ('mycroft.holmes@hotmail.com', 3);
INSERT INTO pwned(email, breaches_no) VALUES ('irene.adler@gmail.com', 5);
INSERT INTO pwned(email, breaches_no) VALUES ('gregsonnypd@yahoo.com', 1);
INSERT INTO pwned(email, breaches_no) VALUES ('shinwell.johnson@gmail.com', 2);
