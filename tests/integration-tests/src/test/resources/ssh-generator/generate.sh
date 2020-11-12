
openssl genrsa -out ca.key 2048
openssl req -new -x509 -days 1826 -key ca.key -out ca.crt
touch certindex
echo 01 > certserial
echo 01 > crlnumber

openssl genrsa -out keystore1.key 2048
openssl req -new -key keystore1.key -out keystore1.csr
openssl ca -batch -config ca.conf -notext -in keystore1.csr -out keystore1.crt
openssl genrsa -out client_revoked.key 2048
openssl req -new -key client_revoked.key -out client_revoked.csr
openssl ca -batch -config ca.conf -notext -in client_revoked.csr -out client_revoked.crt
openssl genrsa -out client_not_revoked.key 2048
openssl req -new -key client_not_revoked.key -out client_not_revoked.csr
openssl ca -batch -config ca.conf -notext -in client_not_revoked.csr -out client_not_revoked.crt
openssl ca -config ca.conf -gencrl -keyfile ca.key -cert ca.crt -out root.crl.pem
openssl ca -config ca.conf -revoke client_revoked.crt -keyfile ca.key -cert ca.crt

openssl ca -config ca.conf -gencrl -keyfile ca.key -cert ca.crt -out root.crl.pem
openssl pkcs12 -export -name client_revoked -in client_revoked.crt -inkey client_revoked.key -out client_revoked.p12
keytool -importkeystore -destkeystore client_revoked.jks -srckeystore client_revoked.p12 -srcstoretype pkcs12 -alias client_revoked


openssl pkcs12 -export -name client_not_revoked -in client_not_revoked.crt -inkey client_not_revoked.key -out client_not_revoked.p12
keytool -importkeystore -destkeystore client_not_revoked.jks -srckeystore client_not_revoked.p12 -srcstoretype pkcs12 -alias client_not_revoked

openssl pkcs12 -export -name keystore1 -in keystore1.crt -inkey keystore1.key -out keystore1.p12
keytool -importkeystore -destkeystore keystore1.jks -srckeystore keystore1.p12 -srcstoretype pkcs12 -alias keystore1

keytool -import -trustcacerts -alias trust_key -file ca.crt -keystore truststore.jks
