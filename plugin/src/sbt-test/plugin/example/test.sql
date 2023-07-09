-- MySQL dump 10.13  Distrib 8.0.22, for macos10.15 (x86_64)
--
-- Host: 127.0.0.1    Database: test
-- ------------------------------------------------------
-- Server version	5.7.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

---- Comment

/* Comment */ CREATE /* Comment */ DATABASE /* Comment */ `location`;

/* Comment */ USE /* Comment */ `location`;

DROP TABLE IF EXISTS `country`;
CREATE /* Comment */ TABLE /* Comment */ country /* Comment */ (
  /* Comment */ code1 /* Comment */ BIT /* Comment */ PRIMARY KEY /* Comment */ UNIQUE KEY /* Comment */ COMMENT 'test',
  code2 bit(24) NOT NULL,
  code3 Bit(64) DEFAULT 1
);

/* Comment */ CREATE /* Comment */ DATABASE /* Comment */ `test`;

/* Comment */ USE /* Comment */ `test`;

--
-- Table structure for table `sub_test`
--

DROP TABLE IF EXISTS `sub_test`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sub_test` (
  `id` BIGINT(64) UNSIGNED ZEROFILL NOT NULL AUTO_INCREMENT,
  `category` TINYINT(64) ZEROFILL NOT NULL,
  PRIMARY KEY(`id`, `category`)
);

--
-- Table structure for table `test`
--

DROP TABLE IF EXISTS `test`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test` (
  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT,
  `sub_id` BIGINT(64) UNSIGNED ZEROFILL NOT NULL,
  `sub_category` TINYINT(64) ZEROFILL NOT NULL,
  `p2` char(3) COLLATE utf8mb4_unicode_ci NOT NULL,
  `p3` INTEGER,
  `p4` CHAR CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `p5` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `p6` BINARY(255),
  `p7` VARBINARY(255) NOT NULL,
  `p8` TINYBLOB,
  `p9` TINYTEXT NOT NULL,
  `p10` BLOB,
  `p11` TEXT NULL DEFAULT NULL,
  `p12` MEDIUMBLOB NOT NULL,
  `p13` MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `p14` LONGBLOB,
  `p15` LONGTEXT CHARACTER SET utf8mb4 NULL,
  `p16` DATE NULL,
  `p17` DATETIME(6) NULL,
  `p18` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `p19` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `p20` TIME(6) NOT NULL,
  `p21` YEAR(4) NOT NULL,
  `p22` ENUM('Active', 'InActive') NOT NULL DEFAULT 'Active',
  PRIMARY KEY (`id`),
  INDEX (`sub_id`) KEY_BLOCK_SIZE=1,
  INDEX (`id`),
  UNIQUE KEY (`id`),
  CONSTRAINT `fk_id` FOREIGN KEY (`sub_id`, `sub_category`) REFERENCES `sub_test` (`id`, `category`) ON UPDATE NO ACTION ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
