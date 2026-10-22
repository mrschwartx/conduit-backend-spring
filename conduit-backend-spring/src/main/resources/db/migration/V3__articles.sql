create table articles (
    id          bigint generated always as identity primary key,
    slug        varchar(120) not null,
    title       varchar(100) not null,
    description varchar(200) not null,
    body        text         not null,
    author_id   bigint       not null references users (id) on delete cascade,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now()
);

create unique index ux_articles_slug on articles (slug);
create index ix_articles_created on articles (created_at desc, id desc);
create index ix_articles_author on articles (author_id, created_at desc, id desc);

-- tag order is preserved through tag_position (first-seen order)
create table article_tags (
    article_id   bigint      not null references articles (id) on delete cascade,
    tag_position integer     not null,
    tag          varchar(30) not null,
    primary key (article_id, tag_position)
);

create index ix_article_tags_tag on article_tags (tag);

create table favorites (
    user_id    bigint      not null references users (id) on delete cascade,
    article_id bigint      not null references articles (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, article_id)
);

create index ix_favorites_article on favorites (article_id);
