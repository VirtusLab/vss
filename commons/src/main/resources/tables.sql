create table hashed_passwords(
  uuid UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  hash_type VARCHAR NOT NULL,
  password_hash VARCHAR NOT NULL
);
