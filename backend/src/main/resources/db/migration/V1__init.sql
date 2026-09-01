create table environment (
    id bigint auto_increment primary key,
    name varchar(100) not null unique
);

create table feature_flag (
    id bigint auto_increment primary key,
    flag_key varchar(255) not null unique,
    name varchar(25) not null,
    description text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table flag_env (
    id bigint auto_increment primary key,
    flag_id bigint not null references feature_flag(id),
    env_id bigint not null references environment(id),
    enabled boolean not null default false,
    version int not null default 1,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uq_flag_env_flag_env unique (flag_id, env_id)
);
