/* Comment */

---- Comment

CREATE /* Comment */ TABLE /* Comment */ country /* Comment */ (
  /* Comment */ code1 /* Comment */ BIT /* Comment */ PRIMARY KEY /* Comment */ UNIQUE KEY /* Comment */ COMMENT 'test' ---- Comment,
  code2 bit(24) NOT NULL,
  code3 Bit(64) DEFAULT 1
);

CREATE TABLE `sub_test` (
  `id` BIGINT(64) UNSIGNED ZEROFILL NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `category` TINYINT(64) ZEROFILL NOT NULL,
  PRIMARY KEY(`id`, `category`)
);

CREATE TABLE `test` (
  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT,
  `sub_id` BIGINT(64) UNSIGNED ZEROFILL NOT NULL,
  `sub_category` TINYINT(64) ZEROFILL NOT NULL,
  `p4` CHAR CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `p5` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `p6` BINARY(255),
  `p7` VARBINARY(255) NOT NULL,
  `p8` TINYBLOB,
  `p9` TINYTEXT NOT NULL,
  `p10` BLOB,
  `p11` TEXT,
  `p12` MEDIUMBLOB NOT NULL,
  `p13` MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `p14` LONGBLOB,
  `p15` LONGTEXT CHARACTER SET utf8mb4 NULL,
  `p16` DATE NULL,
  `p17` DATETIME(6) NULL,
  `p18` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `p19` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `p20` TIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX (`sub_id`),
  INDEX (`id`),
  UNIQUE KEY (`id`),
  CONSTRAINT `fk_id` FOREIGN KEY (`sub_id`, `sub_category`) REFERENCES `sub_test` (`id`, `category`)
);
