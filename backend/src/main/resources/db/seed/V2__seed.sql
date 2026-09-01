insert into environment (name) values ('development'), ('staging'), ('production');

insert into feature_flag (name, description) values
    ('new-payment-flow', 'Toggle new checkout flow'),
    ('homepage-variant', 'Homepage variant flag');

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
