insert into environment (name) values ('development'), ('staging'), ('production');

insert into project ("key", name) values
    ('payments', 'Payments Service'),
    ('web-frontend', 'Web Frontend');

insert into api_key ("key", project_id, description)
select 'payments-mvp-key', id, 'MVP key for payments' from project where "key" = 'payments';

insert into api_key ("key", project_id, description)
select 'search-mvp-key', id, 'MVP key for search/web' from project where "key" = 'web-frontend';

insert into feature_flag (project_id, name, description)
select id, 'new-payment-flow', 'Toggle new checkout flow' from project where "key" = 'payments';

insert into feature_flag (project_id, name, description)
select id, 'homepage-variant', 'Homepage variant flag' from project where "key" = 'web-frontend';

insert into flag_env (flag_id, env_id, enabled, rollout)
select f.id, e.id, false, 100
from feature_flag f join environment e on e.name = 'production'
where f.name = 'new-payment-flow';

insert into flag_env (flag_id, env_id, enabled, rollout)
select f.id, e.id, true, 100
from feature_flag f join environment e on e.name = 'staging'
where f.name = 'new-payment-flow';

insert into flag_env (flag_id, env_id, enabled, rollout)
select f.id, e.id, false, 30
from feature_flag f join environment e on e.name = 'production'
where f.name = 'homepage-variant';

insert into flag_env (flag_id, env_id, enabled, rollout)
select f.id, e.id, true, 100
from feature_flag f join environment e on e.name = 'staging'
where f.name = 'homepage-variant';
