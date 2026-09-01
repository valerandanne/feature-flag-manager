create table environment (
    id uuid default random_uuid() primary key,
    name varchar(100) not null unique
);

create table feature_flag (
    id uuid default random_uuid() primary key,
    name varchar(255) not null unique,
    description text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table flag_env (
    id uuid default random_uuid() primary key,
    flag_id uuid not null references feature_flag(id),
    env_id uuid not null references environment(id),
    enabled boolean not null default false,
    rollout int not null default 100,
    version int not null default 1,
    updated_at timestamp not null default now(),
    constraint uq_flag_env_flag_env unique (flag_id, env_id)
);
