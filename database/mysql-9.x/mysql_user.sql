GRANT ALL PRIVILEGES ON *.* TO ldbc;
CREATE USER 'ldbc_sha256_user'@'%' IDENTIFIED WITH sha256_password BY 'ldbc_sha256_password';
GRANT ALL PRIVILEGES ON *.* TO ldbc_sha256_user;

-- Never logged in successfully, so caching_sha2_password always asks it for full authentication
CREATE USER 'ldbc_cold_sha2_user'@'%' IDENTIFIED WITH caching_sha2_password BY 'ldbc_cold_sha2_password';

-- Create user requesting SSL connection
CREATE USER 'ldbc_ssl_user'@'%' IDENTIFIED BY 'securepassword' REQUIRE SSL;
GRANT ALL PRIVILEGES ON *.* TO 'ldbc_ssl_user'@'%';
