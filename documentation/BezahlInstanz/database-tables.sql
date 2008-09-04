-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
-- these database tables are needed by the BI for storing accounts, cost 
-- confirmations etc. 
--
-- Before you start the BI for the first time, you must create the 
-- following tables in the database. To do this you can log in to postgresql 
-- using the following shell command:
--
-- # psql -U <PayUser> -d <PayDB>
--
-- (Note that you should create the user <PayUser> and the database <PayDB> 
-- before using 'createuser' and 'createdb' commands from the command line)
--
-- You should then get the following Postgresql Prompt:
--
-- PayDB =>
--
-- On this prompt you can type "\i tables.sql" and hit enter. The tables are 
-- then created for you.
--
-- (c) 2oo4 Bastian Voigt <bavoigt@inf.fu-berlin.de>
-------------------------------------------------------------------------------
-------------------------------------------------------------------------------


DROP TABLE RATES;
DROP SEQUENCE RATE_ID_SEQ;
DROP TABLE COSTCONFIRMATIONS;
DROP TABLE TRANSFERS;
DROP TABLE ACCOUNTS;
DROP TABLE PASSIVEPAYMENT;


CREATE TABLE ACCOUNTS (
	ACCOUNTNUMBER BIGINT PRIMARY KEY,
	XMLPUBLICKEY VARCHAR(1000),
	DEPOSIT BIGINT,
	DEPOSITVALIDTIME TIMESTAMP (0),
	SPENT BIGINT,
	CREATION_TIME TIMESTAMP (0),
	ACCOUNTCERT VARCHAR(2000),
	CHECK (DEPOSIT >= 0),
	CHECK (SPENT >= 0),
	CHECK (DEPOSIT >= SPENT)
);

CREATE TABLE PASSIVEPAYMENT (
		ID BIGSERIAL PRIMARY KEY,
		TRANSFERNUMBER BIGINT,
		AMOUNT BIGINT,
		CURRENCY VARCHAR(10),
		DATA VARCHAR(2000),
		CHARGED BOOLEAN,
		TYPE VARCHAR(30)
);


CREATE TABLE COSTCONFIRMATIONS (
	AiID VARCHAR(128),
	ACCOUNTNUMBER BIGINT REFERENCES ACCOUNTS ON DELETE CASCADE,
	TRANSFERREDBYTES BIGINT,
	XMLCC VARCHAR(1024),
	CHECK (TRANSFERREDBYTES >= 0)
);


CREATE TABLE TRANSFERS (
	TRANSFERNUMBER BIGINT PRIMARY KEY,
	ACCOUNTNUMBER BIGINT REFERENCES ACCOUNTS ON DELETE CASCADE,
	DEPOSIT BIGINT,
	VALIDTIME TIMESTAMP (0),
	USED BOOLEAN,
	USEDTIME BIGINT,
	AMOUNT BIGINT
);


CREATE TABLE RATES (
	ID SERIAL PRIMARY KEY,
	NAME VARCHAR(32),
	AMOUNT NUMERIC, 
	FIXED_AMOUNT BOOLEAN,
	MBYTES INTEGER,
	VALID_DAYS INTEGER, 
	VALID_MONTHS INTEGER
);


