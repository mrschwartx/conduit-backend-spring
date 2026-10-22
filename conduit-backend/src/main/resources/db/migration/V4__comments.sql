create table comments (
    id         bigint generated always as identity primary key,
    article_id bigint        not null references articles (id) on delete cascade,
    author_id  bigint        not null references users (id) on delete cascade,
    body       varchar(1000) not null,
    created_at timestamptz   not null default now(),
    updated_at timestamptz   not null default now()
);

create index ix_comments_article on comments (article_id, created_at desc, id desc);
