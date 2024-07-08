
create table topicos(

    id bigint not null auto_increment,
    titulo varchar(100) not null,
    autor varchar(100) not null,
    mensaje varchar(100) not null,
    curso varchar(20) not null,
    fecha_creacion timestamp default current_timestamp,

    primary key(id)

);