create sequence record_change_id_seq;

alter sequence record_change_id_seq owner to postgres;

create sequence category_change_id_seq;

alter sequence category_change_id_seq owner to postgres;

create table record_types
(
	id serial not null
		constraint record_types_pkey
			primary key,
	name varchar(64) not null
);

alter table record_types owner to postgres;

create unique index record_types_id_uindex
	on record_types (id);

create unique index record_types_name_uindex
	on record_types (name);

create table users
(
	id serial not null
		constraint users_pk
			primary key,
	login varchar(64) not null
);

alter table users owner to postgres;

create unique index users_id_uindex
	on users (id);

create unique index users_login_uindex
	on users (login);

create table record_categories
(
	uuid text not null
		constraint record_kinds_pkey
			primary key,
	name varchar(64) not null,
	record_type_id integer not null
		constraint record_kinds_record_types_id_fk
			references record_types,
	change_id integer default nextval('category_change_id_seq'::regclass) not null,
	user_id integer not null
		constraint record_categories_users_id_fk
			references users
);

alter table record_categories owner to postgres;

create unique index record_kinds_uuid_uindex
	on record_categories (uuid);

create unique index record_kinds_name_record_type_id_uindex
	on record_categories (name, record_type_id, user_id);

create index record_categories_user_id_index
	on record_categories (user_id);

create unique index record_categories_change_id_uindex
	on record_categories (change_id);

create table records
(
	uuid text not null
		constraint records_pkey
			primary key,
	date date not null,
	kind varchar(64) not null,
	value integer not null,
	deleted boolean default false not null,
	time time,
	record_category_uuid text not null
		constraint records_record_categories_uuid_fk
			references record_categories,
	change_id integer default nextval('record_change_id_seq'::regclass) not null
);

alter table records owner to postgres;

create unique index records_uuid_uindex
	on records (uuid);

create unique index records_change_id_uindex
	on records (change_id);

create table tokens
(
	user_id integer not null
		constraint tokens_users_id_fk
			references users,
	token varchar(64) not null
);

alter table tokens owner to postgres;

create index tokens_token_hash_index
	on tokens (token);



INSERT INTO record_types (id, name) VALUES (1, 'расход'), (2, 'доход');

-- ALTER SEQUENCE category_change_id_seq RESTART WITH 42;
-- ALTER SEQUENCE record_change_id_seq RESTART WITH 1918;