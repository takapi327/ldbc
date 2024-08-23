CREATE DATABASE `benchmark` DEFAULT CHARACTER SET utf8mb4;

USE `benchmark`;

CREATE TABLE `jdbc_statement_test` (
  `c0` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `c1` TINYINT NOT NULL,
  `c2` SMALLINT NOT NULL,
  `c3` MEDIUMINT NOT NULL,
  `c4` INT NOT NULL,
  `c5` BIGINT NOT NULL,
  `c6` FLOAT NOT NULL,
  `c7` DOUBLE NOT NULL,
  `c8` DECIMAL NOT NULL,
  `c9` VARCHAR(255) NOT NULL,
  `c10` TEXT NOT NULL,
  `c11` BOOLEAN NOT NULL,
  `c12` DATE NOT NULL,
  `c13` TIME NOT NULL,
  `c14` DATETIME NOT NULL,
  `c15` TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `jdbc_prepare_statement_test` (
  `c0` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `c1` TINYINT NOT NULL,
  `c2` SMALLINT NOT NULL,
  `c3` MEDIUMINT NOT NULL,
  `c4` INT NOT NULL,
  `c5` BIGINT NOT NULL,
  `c6` FLOAT NOT NULL,
  `c7` DOUBLE NOT NULL,
  `c8` DECIMAL NOT NULL,
  `c9` VARCHAR(255) NOT NULL,
  `c10` TEXT NOT NULL,
  `c11` BOOLEAN NOT NULL,
  `c12` DATE NOT NULL,
  `c13` TIME NOT NULL,
  `c14` DATETIME NOT NULL,
  `c15` TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `ldbc_statement_test` (
  `c0` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `c1` TINYINT NOT NULL,
  `c2` SMALLINT NOT NULL,
  `c3` MEDIUMINT NOT NULL,
  `c4` INT NOT NULL,
  `c5` BIGINT NOT NULL,
  `c6` FLOAT NOT NULL,
  `c7` DOUBLE NOT NULL,
  `c8` DECIMAL NOT NULL,
  `c9` VARCHAR(255) NOT NULL,
  `c10` TEXT NOT NULL,
  `c11` BOOLEAN NOT NULL,
  `c12` DATE NOT NULL,
  `c13` TIME NOT NULL,
  `c14` DATETIME NOT NULL,
  `c15` TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `ldbc_prepare_statement_test` (
  `c0` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `c1` TINYINT NOT NULL,
  `c2` SMALLINT NOT NULL,
  `c3` MEDIUMINT NOT NULL,
  `c4` INT NOT NULL,
  `c5` BIGINT NOT NULL,
  `c6` FLOAT NOT NULL,
  `c7` DOUBLE NOT NULL,
  `c8` DECIMAL NOT NULL,
  `c9` VARCHAR(255) NOT NULL,
  `c10` TEXT NOT NULL,
  `c11` BOOLEAN NOT NULL,
  `c12` DATE NOT NULL,
  `c13` TIME NOT NULL,
  `c14` DATETIME NOT NULL,
  `c15` TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `ldbc_wrapper_query_test` (
  `c0` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `c1` INT NOT NULL,
  `c2` VARCHAR(255) NOT NULL
);

CREATE TABLE `ldbc_wrapper_dsl_test` (
  `c0` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `c1` INT NOT NULL,
  `c2` VARCHAR(255) NOT NULL
);

CREATE TABLE `doobie_wrapper_test` (
   `c0` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   `c1` INT NOT NULL,
   `c2` VARCHAR(255) NOT NULL
);

CREATE TABLE `slick_wrapper_test` (
  `c0` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `c1` INT NOT NULL,
  `c2` VARCHAR(255) NOT NULL
);
