GRANT ALL PRIVILEGES ON *.* TO ldbc;
CREATE USER 'ldbc_mysql_native_user'@'%' IDENTIFIED WITH mysql_native_password BY 'ldbc_mysql_native_password';
GRANT ALL PRIVILEGES ON *.* TO ldbc_mysql_native_user;
CREATE USER 'ldbc_sha256_user'@'%' IDENTIFIED WITH sha256_password BY 'ldbc_sha256_password';
GRANT ALL PRIVILEGES ON *.* TO ldbc_sha256_user;
