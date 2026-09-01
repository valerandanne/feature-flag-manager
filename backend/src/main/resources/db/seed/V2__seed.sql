insert into environment (name) values ('development'), ('staging'), ('production');

insert into feature_flag (flag_key, name, description) values
    ('new-payment-flow', 'New Payment Flow', 'Toggle new checkout flow'),
    ('homepage-variant', 'Homepage Variant', 'Homepage variant flag');

insert into flag_env (flag_id, env_id, enabled)
select f.id, e.id, false
from feature_flag f join environment e on e.name = 'production'
where f.flag_key = 'new-payment-flow';

insert into flag_env (flag_id, env_id, enabled)
select f.id, e.id, true
from feature_flag f join environment e on e.name = 'staging'
where f.flag_key = 'new-payment-flow';

insert into flag_env (flag_id, env_id, enabled)
select f.id, e.id, false
from feature_flag f join environment e on e.name = 'production'
where f.flag_key = 'homepage-variant';

insert into flag_env (flag_id, env_id, enabled)
select f.id, e.id, true
from feature_flag f join environment e on e.name = 'staging'
where f.flag_key = 'homepage-variant';
