create table follows (
    follower_id bigint      not null references users (id) on delete cascade,
    followee_id bigint      not null references users (id) on delete cascade,
    created_at  timestamptz not null default now(),
    primary key (follower_id, followee_id),
    check (follower_id <> followee_id)
);

create index ix_follows_followee on follows (followee_id);
