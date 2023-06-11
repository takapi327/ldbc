/* コメント */

---- コメント

CREATE TABLE country (
  code1 BIT(1) PRIMARY KEY UNIQUE KEY COMMENT 'test',
  code2 bit(24) NOT NULL,
  code3 Bit(64) DEFAULT 1
);

CREATE TABLE country1 (
  code2 TINYINT(3) UNSIGNED NULL
);
