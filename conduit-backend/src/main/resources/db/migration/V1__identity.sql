create table users (
    id            bigint generated always as identity primary key,
    username      varchar(30)  not null,
    email         varchar(254) not null,
    password_hash varchar(100) not null,
    bio           varchar(300),
    image         varchar(2048),
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create unique index ux_users_username_lower on users (lower(username));
create unique index ux_users_email_lower on users (lower(email));
