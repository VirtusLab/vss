CREATE TABLE hashed_passwords(
  uuid UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  hash_type VARCHAR NOT NULL,
  password_hash VARCHAR NOT NULL
);

CREATE INDEX idx_hashed_passwords_password_hash
ON hashed_passwords(password_hash);
