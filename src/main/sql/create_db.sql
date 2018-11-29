create sequence record_change_id_seq
;

create sequence category_change_id_seq
;

create table record_types
(
	id serial not null
		constraint record_types_pkey
			primary key,
	name varchar(64) not null
)
;

create unique index record_types_id_uindex
	on record_types (id)
;

create unique index record_types_name_uindex
	on record_types (name)
;

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
	record_category_uuid text not null,
	change_id integer default nextval('record_change_id_seq'::regclass) not null
)
;

create unique index records_uuid_uindex
	on records (uuid)
;

create table record_categories
(
	uuid text not null
		constraint record_kinds_pkey
			primary key,
	name varchar(64) not null,
	record_type_id integer not null
		constraint record_kinds_record_types_id_fk
			references record_types,
	change_id integer default nextval('category_change_id_seq'::regclass) not null
)
;

create unique index record_kinds_uuid_uindex
	on record_categories (uuid)
;

create unique index record_kinds_name_record_type_id_uindex
	on record_categories (name, record_type_id)
;

alter table records
	add constraint records_record_categories_uuid_fk
		foreign key (record_category_uuid) references record_categories
;

