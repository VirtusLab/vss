create table hashed_passwords(
  uuid UUID default gen_random_uuid() primary key,
  hash_type varchar not null,
  password_hash varchar not null
);

create index idx_hashed_passwords_password_hash 
on hashed_passwords(password_hash);
