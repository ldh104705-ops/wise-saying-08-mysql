-- 개발용 DB / 테스트용 DB 를 분리한다.
CREATE DATABASE IF NOT EXISTS wiseSaying__dev CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS wiseSaying__test CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 명언 테이블 (두 DB 에 모두 만든다. 앱도 시작할 때 CREATE TABLE IF NOT EXISTS 로 만든다.)
USE wiseSaying__dev;

CREATE TABLE IF NOT EXISTS wise_saying (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    createDate DATETIME NOT NULL,
    modifyDate DATETIME NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4;

USE wiseSaying__test;

CREATE TABLE IF NOT EXISTS wise_saying (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    createDate DATETIME NOT NULL,
    modifyDate DATETIME NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4;
