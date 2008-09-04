#!/bin/bash

SQL_USER="pay"
SQL_PASSWORD="dito"
SQL_HOST="127.0.0.1"
SQL_PORT="5432"
SQL_DBNAME="paydb"

psql -U $SQL_USER -d $SQL_DBNAME -p $SQL_PORT -h $SQL_HOST

