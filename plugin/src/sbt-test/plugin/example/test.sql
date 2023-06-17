/* Comment */

---- Comment

CREATE /* Comment */ TABLE /* Comment */ country /* Comment */ (
  /* Comment */ code1 /* Comment */ BIT /* Comment */ PRIMARY KEY /* Comment */ UNIQUE KEY /* Comment */ COMMENT 'test' ---- Comment,
  code2 bit(24) NOT NULL,
  code3 Bit(64) DEFAULT 1
);

CREATE TABLE `sub_test` (
  `id` BIGINT(64) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `category` TINYINT(64) NOT NULL,
  PRIMARY KEY(`id`, `category`)
);

CREATE TABLE `test` (
  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT,
  `sub_id` TINYINT UNSIGNED NOT NULL,
  `sub_category` BIGINT(64) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX (`sub_id`),
  INDEX (`id`),
  UNIQUE KEY (`id`),
  CONSTRAINT `fk_id` FOREIGN KEY (`sub_id`, `sub_category`) REFERENCES `sub_test` (`id`, `category`)
);
