# Database Tests

This module will run tests against selected Databases.

There is one profile for each Database supported:

- DB-derby-tests
- DB-postgres-tests
- DB-mysql-tests
- DB-mssql-tests
- DB-db2-tests
- DB-oracle-tests

To enable the testsuite to run against any of these tests you just have to enable these profiles.

# Configuring the JDBC URI

You can pass the JDBC URI as a parameter:

Supported parameters:
- derby.uri
- postgres.uri
- mysql.uri
- mssql.uri
- oracle.uri
- db2.uri

Example:

```shell
mvn -Pdb2.uri='jdbc:db2://MyDB2Server:50000/artemis:user=db2inst1;password=artemis;'
```

# Servers

There is one artemis server created for each database supported. After built they will be available under ./target/${DATABASE}

- ./target/derby
- ./target/postgres
- ./target/mysql
- ./target/mssql
- ./target/db2


# Oracle JDBC Driver

All the JDBC drivers using in this module are available as maven central repository and are being downloaded by the artemis-maven-plugin during the compilation phase of the tests.

The exception to this rule is [Oracle database](https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html) and you must provide the jar under this location:

- db-tests/jdbc-drivers/oracle


# Container Examples

You will find one example script for each database supported under db-tests/scripts.

These scripts here are provided as an example only and not meant for a production system.

You must download and provide the database before running the tests on this module, accordingly to the license of each vendor.