#!/bin/bash

# Creation of directories for SSL-related files
mkdir -p ./database/ssl
cd ./database/ssl

RESOURCES_DIR="../../module/ldbc-connector/shared/src/test/resources"
mkdir -p "$RESOURCES_DIR"

# CA certificate and key generation
openssl genrsa 2048 > ca-key.pem
openssl req -new -x509 -nodes -days 3650 -key ca-key.pem -out ca.pem -subj "/CN=MySQL_CA"

# Server Certificate and Key Generation
openssl req -newkey rsa:2048 -days 3650 -nodes -keyout server-key.pem -out server-req.pem -subj "/CN=MySQL_Server"
openssl rsa -in server-key.pem -out server-key.pem
openssl x509 -req -in server-req.pem -days 3650 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out server-cert.pem

# Client Certificate and Key Generation
openssl req -newkey rsa:2048 -days 3650 -nodes -keyout client-key.pem -out client-req.pem -subj "/CN=MySQL_Client"
openssl rsa -in client-key.pem -out client-key.pem
openssl x509 -req -in client-req.pem -days 3650 -CA ca.pem -CAkey ca-key.pem -set_serial 02 -out client-cert.pem

# Appropriate authorization settings
chmod 600 *.pem

# Import CA certificates into keystore
keytool -import -file ca.pem -alias mysqlCA -keystore "$RESOURCES_DIR/keystore.jks" -storepass password -noprompt

# Make the generated files available to the MySQL container
cp ca.pem server-cert.pem server-key.pem .

echo "SSL certificate generation is complete!"
echo "keystore.jks was generated at the following location："
echo "$RESOURCES_DIR"
