/* Comment */

---- Comment

CREATE /* Comment */ TABLE /* Comment */ country /* Comment */ (
  /* Comment */ code1 /* Comment */ BIT(1) /* Comment */ PRIMARY KEY /* Comment */ UNIQUE KEY /* Comment */ COMMENT 'test' ---- Comment,
  code2 bit(24) NOT NULL,
  code3 Bit(64) DEFAULT 1
);

CREATE TABLE country1 (
  code2 TINYINT(3) UNSIGNED NULL
);
