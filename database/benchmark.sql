CREATE DATABASE `benchmark` DEFAULT CHARACTER SET utf8mb4;

USE `benchmark`;

CREATE TABLE `jdbc_statement_test` (
  `c1` INT NOT NULL,
  `c2` VARCHAR(255) NOT NULL
);

CREATE TABLE `jdbc_prepare_statement_test` (
  `c1` INT NOT NULL,
  `c2` VARCHAR(255) NOT NULL
);

CREATE TABLE `ldbc_statement_test` (
  `c1` INT NOT NULL,
  `c2` VARCHAR(255) NOT NULL
);

CREATE TABLE `ldbc_prepare_statement_test` (
  `c1` INT NOT NULL,
  `c2` VARCHAR(255) NOT NULL
);
