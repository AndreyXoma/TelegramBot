-- liquibase formatter sql
--changeset a.khomyakov:feature-1 context:master
create table if not exists users
(
    id bigint PRIMARY KEY NOT NULL ,
    name varchar(60) NOT NULL,
    last_message_at varchar(1000) not null
    );

create table if not exists message
(
    id_message BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    incoming_mess varchar(1000) not null,
    outgoing_mess varchar(100) not null,
    id_user bigint references users(id)
    );

create table if not exists daily_domains
(
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    domainname varchar(100),
    hotness int,
    price bigint ,
    x_value int,
    yandex_tic int ,
    links int ,
    visitors int ,
    registrar varchar(50),
    old int,
    delete_date date ,
    rkn boolean ,
    judicial boolean,
    block boolean
    )

--rollback drop table users
--rollback drop table message
--rollback drop table daily_domains
