CREATE TABLE IF NOT EXISTS users
(
    id         bigint       NOT NULL AUTO_INCREMENT,
    first_name varchar(255) NOT NULL,
    last_name  varchar(255) NOT NULL,
    email      varchar(255) NOT NULL,
    full_name  varchar(255) DEFAULT NULL,
    PRIMARY KEY (id)
);
