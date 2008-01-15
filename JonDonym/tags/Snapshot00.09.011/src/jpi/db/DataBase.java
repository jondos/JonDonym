/*
 Copyright (c) 2000 - 2004, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package jpi.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import anon.pay.xml.XMLEasyCC;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLPriceCertificate;
import java.util.Enumeration;
import anon.pay.xml.XMLBankAccount;
import anon.pay.xml.XMLTransferRequest;
import anon.crypto.XMLSignature;
import anon.crypto.JAPCertificate;
import anon.util.Base64;
import java.util.Hashtable;
import java.lang.reflect.Method;
import jpi.Configuration;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLErrorMessage;
import java.text.SimpleDateFormat;
import anon.pay.xml.XMLPaymentOption;
import java.util.StringTokenizer;
import anon.pay.xml.XMLPaymentSettings;
import java.text.DecimalFormat;
import org.w3c.dom.Document;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.math.BigDecimal;
import anon.pay.xml.XMLVolumePlans;
import anon.pay.xml.XMLVolumePlan;

/**
 * Implements {@link DBInterface} for Postgresql
 *
 * @author Andreas Mueller, Bastian Voigt, Elmar Schraml
 * @version 1.2
 */
public class DataBase extends DBInterface
{
	private Connection m_dbConn;
	public DataBase(Connection c)
	{
		m_dbConn = c;
		this.initPaymentSettings();
	}

	// random numbers for generating account / transfer numbers
	private Random rnd = new Random(System.currentTimeMillis());

	//settings affecting the flatrate(s) offered, make changes in this.setPaymentSettings()
	private Hashtable paymentSettings = new Hashtable();

	//scale of the decimal columns used in the database = scale of the BigDecimal Java objects
	private static final int DECIMAL_SCALE = 8;

	// Names of objects in the DB, used by createTables()
	private String[] db_tables =
		{
		"TABLE ACCOUNTS", "TABLE COSTCONFIRMATIONS",
		"TABLE TRANSFERS",
		"TABLE PASSIVEPAYMENTS", "TABLE MIXOPERATORS","TABLE MIXACCOUNTS", "TABLE PRICECERTS",
		 "TABLE CLEARINGS",
		"TABLE PAYMENTOPTIONS", "TABLE PAYMENTOPTIONSTRINGS", "TABLE ACTIVEPAYMENTSTRINGS", "TABLE PASSIVEPAYMENTSTRINGS",
		"TABLE USERS", "TABLE PAYMENTSETTINGS", "TABLE MIXTRAFFIC", "TABLE JAPTRAFFIC",
		"TABLE PAYSAFECARDPAYMENTS","TABLE COUPONS", "TABLE VOLUMEPLANS"
		//the table USERS is used by the BI-GUI only, dropping it will delete webapp accounts
		//so exclude it here if you want to recreate the BI tables without affecting the BI-GUI
	};

	// sql statements for creating the DB tables, used by createTAbles()
	private String[] create_statements =
		{
		"CREATE TABLE ACCOUNTS (" +
		"ACCOUNTNUMBER BIGSERIAL PRIMARY KEY," +
		"XMLPUBLICKEY VARCHAR(1000)," +
		"DEPOSIT DECIMAL(15,8) default 0 CHECK (DEPOSIT >=0),"+
		"DEPOSITVALIDTIME TIMESTAMP (0)," +
		"SPENT BIGINT default 0 CHECK (SPENT >= 0)," +
		"BALANCE DECIMAL(15,8) default 0 CHECK (BALANCE >= 0)," +
		"FLAT_ENDDATE TIMESTAMP(0) default '1970-01-01 10:10:10',"+
		"VOLUME_BYTESLEFT BIGINT default 0,"+
		"CREATION_TIME TIMESTAMP (0)," +
		"ACCOUNTCERT VARCHAR(2000), " +
		"BLOCKED INTEGER DEFAULT 0" +
		");"
		,

		"CREATE TABLE COSTCONFIRMATIONS (" +
		"id serial primary key," +
		"CASCADE_ID VARCHAR(50)," +
		"ACCOUNT_ACCOUNTNUMBER BIGINT REFERENCES ACCOUNTS(ACCOUNTNUMBER)," +
		"TRANSFERREDBYTES BIGINT CHECK (TRANSFERREDBYTES >= 0)," +
		"XMLCC VARCHAR(1024));"
		,
		"CREATE TABLE TRANSFERS (" +
		"TRANSFERNUMBER BIGSERIAL PRIMARY KEY," +
		"ACCOUNT_ACCOUNTNUMBER BIGINT REFERENCES ACCOUNTS ON DELETE CASCADE," +
		"DEPOSIT BIGINT," +
		"VALIDTIME TIMESTAMP (0)," +
		"USED BOOLEAN," +
		"USEDTIME BIGINT," +
	    "AMOUNT BIGINT);"
		,
		"CREATE TABLE PASSIVEPAYMENTS (" +
		"ID BIGSERIAL PRIMARY KEY," +
		"TRANSFER_TRANSFERNUMBER BIGINT REFERENCES TRANSFERS," +
		"AMOUNT BIGINT," +
		"CURRENCY VARCHAR(10)," +
		"DATA VARCHAR(2000)," +
		"CHARGED BOOLEAN, " +
		"PAYMENTTYPE VARCHAR(30));"
		,
		"CREATE TABLE MIXOPERATORS ( "+
		"ID SERIAL PRIMARY KEY, "+
		"OPERATORCERT VARCHAR(2000),"+
		"TRANSFERTYPE VARCHAR(30),"+
		"TRANSFERDETAILS VARCHAR(300),"+
		"TRANSFERREQUEST INTEGER"+
		");"
		,
	    "CREATE TABLE MIXACCOUNTS (" +
		"ID SERIAL PRIMARY KEY,"+
		"SUBJECTKEYIDENTIFIER VARCHAR(50),"+
		"MIXCERT VARCHAR(2000),"+
		"BALANCE DECIMAL(15,8) NOT NULL DEFAULT 0,"+
		"TRAFFIC_SINCE_CLEARING bigint default 0,"+
		"UPDATED_ON TIMESTAMP(0),"+
		"MIXOPERATOR_ID INTEGER REFERENCES MIXOPERATORS(ID)"+
		");"
     	,
		"CREATE TABLE PRICECERTS (" +
		"ID SERIAL PRIMARY KEY, " +
		"MIXACCOUNT_ID INTEGER REFERENCES MIXACCOUNTS,"+
		"SUBJECTKEYIDENTIFIER VARCHAR(50) NOT NULL,"+
		"RATE DOUBLE PRECISION,"+
		"BIID VARCHAR(50),"+
		"SIGNATURETIME TIMESTAMP(0),"+
		"HASH VARCHAR(100),"+
		"BLOCKED BOOLEAN," +
		"SIGNEDXML VARCHAR(3000)" +
		");"
		,
		"CREATE TABLE CLEARINGS ("+
		"id SERIAL NOT NULL UNIQUE PRIMARY KEY," +
		"amount integer,"+
		"traffic bigint,"+
		"date timestamp,"+
		"mixaccount_id integer references mixaccounts"+
		");"
		,
		"CREATE TABLE PAYMENTOPTIONS ("+
		"id SERIAL NOT NULL UNIQUE PRIMARY KEY," +
		"name varchar(50)," +
		"paymenttype varchar(30) not null,"+
		"generic boolean,"+
		"japversion varchar(20)"+
		");"
		,
		"CREATE TABLE PAYMENTOPTIONSTRINGS ("+
		"id SERIAL NOT NULL UNIQUE PRIMARY KEY," +
		"paymentoption_id integer references paymentoptions on delete cascade,"+
		"language varchar(20) default 'en',"+
		"heading varchar(50) default 'n/a',"+
		"detailedinfo varchar(200) default 'n/a' "+
		");"
		,
		"CREATE TABLE ACTIVEPAYMENTSTRINGS ("+
		"id SERIAL NOT NULL UNIQUE PRIMARY KEY," +
		"paymentoption_id integer references paymentoptions on delete cascade," +
		"language varchar(20) not null," +
		"extrainfolabel varchar(200) not null," +
		"labeltype varchar(30) not null" +
		");"
		,
		"CREATE TABLE PASSIVEPAYMENTSTRINGS ("+
		"id SERIAL NOT NULL UNIQUE PRIMARY KEY," +
		"paymentoption_id integer references paymentoptions on delete cascade,"+
		"language varchar(20) not null,"+
		"inputlabel varchar(200) not null,"+
		"ref varchar(30)"+
		");"
		,

		"CREATE TABLE USERS ( " +
		"id SERIAL NOT NULL UNIQUE PRIMARY KEY," +
		"login VARCHAR(80)," +
		"password VARCHAR(50), " +
		"salt varchar(40)," +
		"is_admin boolean" +
		") WITH OIDS;"
		,
		"CREATE TABLE PAYMENTSETTINGS (" +
		"ID SERIAL NOT NULL UNIQUE PRIMARY KEY," +
		"NAME VARCHAR(100),"+
		"CURVALUE VARCHAR(100),"+
		"DATATYPE VARCHAR(20)"+
		");"
		,
		"CREATE TABLE MIXTRAFFIC (" +
		"ID SERIAL NOT NULL UNIQUE PRIMARY KEY,"+
		"MIXACCOUNT_ID INTEGER REFERENCES MIXACCOUNTS (ID),"+
		"MONTH INTEGER,"+
		"YEAR INTEGER,"+
		"TRAFFIC BIGINT,"+
		"NUMBER_OF_COSTCONFIRMATIONS BIGINT"+
		");"
		,
		"CREATE TABLE JAPTRAFFIC (" +
		"ID SERIAL NOT NULL UNIQUE PRIMARY KEY,"+
		"ACCOUNT_ACCOUNTNUMBER BIGINT REFERENCES ACCOUNTS," +
		"CASCADE_ID varchar(50),"+
		"MONTH INTEGER,"+
		"YEAR INTEGER,"+
		"TRAFFIC BIGINT,"+
		"NUMBER_OF_COSTCONFIRMATIONS BIGINT"+
		");"
		,
		"CREATE TABLE PAYSAFECARDPAYMENTS (" +
		"ID BIGSERIAL PRIMARY KEY,"+
		"MERCHANT_ID VARCHAR(20),"+
		"TRANSACTION_ID VARCHAR(50),"+
		"AMOUNT VARCHAR(30),"+
		"CURRENCY VARCHAR(10)," +
		"TRANSFERS_TRANSFERNUMBER BIGINT,"+
		"CREATIONTIME TIMESTAMP,"+
		"STATE VARCHAR(50) DEFAULT 'CREATED'"+
		");"
		,
		"CREATE TABLE COUPONS ( "+
		"ID SERIAL PRIMARY KEY,"+
		"COUPONCODE VARCHAR(50),"+
		"AMOUNT INTEGER,"+
		"CREATED_ON TIMESTAMP,"+
		"ACCOUNTS_ACCOUNTNUMBER BIGINT REFERENCES ACCOUNTS,"+
		"REDEEMED_ON TIMESTAMP,"+
		"VALID_UNTIL TIMESTAMP"+
		");"
		,
		"CREATE TABLE VOLUMEPLANS (" +
		"ID SERIAL PRIMARY KEY,"+
		"NAME VARCHAR(50) UNIQUE,"+
		"PRICE INTEGER NOT NULL,"+
		"VOLUME_LIMITED BOOLEAN,"+
		"DURATION_LIMITED BOOLEAN,"+
		"VOLUME_KBYTES BIGINT,"+
		"DURATION INTEGER,"+
		"DURATION_UNIT VARCHAR(50)"+
		");"



	}; //end of array of sql strings

	private String[] create_indexes = {
		"create index cc_fk on  costconfirmations(account_accountnumber);" ,
		"create index pos_fk on  paymentoptionstrings(paymentoption_id);" ,
		"create index aps_fk on  activepaymentstrings(paymentoption_id);" ,
		"create index pps_fk on  passivepaymentstrings(paymentoption_id);" ,
		"create index pc_fk on  pricecerts(mixaccount_id)" ,
		"create index pc_ski on  pricecerts(subjectkeyidentifier)" , //important since we often search on a Mix's ski rather than the id
		"create index mix_ski on  mixaccounts(subjectkeyidentifier)" ,
		"create index mix_fk on  mixaccounts(mixoperator_id)" ,
		"create index mixop_cert on  mixoperators(operatorcert)" , //big varchar, so slow to insert, but there are far more selects than inserts
		"create index cc_idx on  costconfirmations(account_accountnumber, cascade_id)" , //very important for performance while settling
		"create index cc_idx2 on  costconfirmations(cascade_id,account_accountnumber)" , //just to be sure there's no huge performance drop just because the order of arguments gets mixed up
		"create index mixtraffic_idx on  mixtraffic(mixaccount_id,year,month)" ,
		"create index japtraffic_idx on  japtraffic(cascade_id, account_accountnumber, year, month)" ,
		"create index pp_charged on  passivepayments(charged)" ,
		"create index pp_fk on  passivepayments(transfer_transfernumber)" ,
		"create index psc_tan on paysafecardpayments(transaction_id)" ,
		"create index psc_state on paysafecardpayments(state)" ,
		"create index psc_time on paysafecardpayments(creationtime)",
		"create index cp_code on coupons(couponcode)",
		"create index cp_valid on coupons(valid_until)",
		"create index cp_account on coupons(accounts_accountnumber)",
		"create index vp_name on volumeplans(name)"
	};

	private void initPaymentSettings()
	{
		paymentSettings = new Hashtable();
		//flatrate options
		paymentSettings.put("FlatEnabled", "Boolean");
		paymentSettings.put("DurationLimited", "Boolean");
		paymentSettings.put("VolumeLimited", "Boolean");
		paymentSettings.put("FlatrateDuration", "Integer");
		paymentSettings.put("FlatrateDurationUnit", "String");
		paymentSettings.put("VolumeAmount", "Long");
		paymentSettings.put("FlatratePrice","Integer");
		paymentSettings.put("LogPaymentStatsEnabled","Boolean");
		paymentSettings.put("SignatureOnPriceRequired","Boolean");
		//paysafecard options
		paymentSettings.put("OkUrl","String");
		paymentSettings.put("NokUrl","String");
		paymentSettings.put("ConfirmUrl","String");
		paymentSettings.put("DispositionTimeout","Integer");
		paymentSettings.put("CleanupInterval","Integer");
		paymentSettings.put("LogExpiration","Integer");
		//logging options
		paymentSettings.put("LogPaymentStatsEnabled","Boolean");
		//credit cards
		paymentSettings.put("AcceptedCreditCards","String");
	}
	// Documentation see DBInterface / wird hoffentlich nicht mehr gebraucht /
	/*	public String getCert(IMyPublicKey pubkey) throws RequestException
	 {
	  LogHolder.log(LogLevel.DEBUG, LogType.PAY,"DataBase.getCert() called.");
	  String cert = null;
	  String exponent = pubkey.getPublicExponent().toString();
	  String modulus = pubkey.getModulus().toString();
	  try
	  {
	   Statement stmt = con.createStatement();
	   ResultSet rs =
	 stmt.executeQuery("SELECT ACCOUNTCERT FROM ACCOUNTS WHERE EXPONENT='" + exponent +
	 "' AND MODULUS='" + modulus + "'");
	   if (rs.next())
	   {
	 cert = rs.getString(1);
	   }
	   rs.close();
	   stmt.close();
	  }
	  catch (SQLException e)
	  {
	   LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
	   throw new RequestException(500);
	  }
	  return cert;
	 }*/

	// Documentation see DBInterface
	/*	public long getAccountNumber(IMyPublicKey pubkey) throws Exception
	 {
	  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "DataBase.getAccountNumber() called.");
	  long accountnumber = 0;
	  String exponent = pubkey.getPublicExponent().toString();
	  String modulus = pubkey.getModulus().toString(); ;
	  try
	  {
	   Statement stmt = con.createStatement();
	   ResultSet rs =
	 stmt.executeQuery("SELECT ACCOUNTNUMBER FROM ACCOUNTS WHERE EXPONENT='" +
	 exponent + "' AND MODULUS= '" + modulus + "'");
	   if (rs.next())
	   {
	 accountnumber = rs.getLong(1);
	   }
	   rs.close();
	   stmt.close();
	  }
	  catch (Exception e)
	  {
	   LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
	   throw e;
	  }
	  return accountnumber;
	 }*/

	// Documentation see DBInterface
	 // Elmar: deprecated, will NOT work with the new account format!!
	public Balance getBalance(long accountnumber) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "DataBase.getBalance() called for account " +
					  accountnumber + ".");
		Balance bal = null;
		long deposit;
		long spent;
		java.sql.Timestamp timestamp;
		java.sql.Timestamp validTime;

		Vector confirms = new Vector();
		try
		{
			// get balance and max balance
			Statement stmt = m_dbConn.createStatement();
			ResultSet rs =
				stmt.executeQuery("SELECT DEPOSIT,SPENT,DEPOSITVALIDTIME " +
								  "FROM ACCOUNTS WHERE ACCOUNTNUMBER=" + accountnumber);
			if (rs.next())
			{
				BigDecimal decimalDeposit = rs.getBigDecimal("deposit");
				deposit = DbUtil.intFromDecimal(decimalDeposit);
				spent = rs.getLong("spent");
				//util function returns int, not long, but using long is a holdover from when
				//we were saving balances in bytes; now we're using euros, so int should be plenty
				validTime = (java.sql.Timestamp) rs.getObject(3); //Elmar: why not rs.getTimestamp() directly?
			}
			else
			{
				throw new Exception("account no. " + accountnumber + " is not in database");
			}
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetched deposit " + deposit + ", spent " + spent +
						  ", validTime " + validTime.toString() + " from DB.");

			// get user cost confirmations
			Statement stmt2 = m_dbConn.createStatement();
			ResultSet rs2 = stmt.executeQuery("SELECT XMLCC FROM COSTCONFIRMATIONS WHERE ACCOUNT_ACCOUNTNUMBER=" +
											  accountnumber);
			while (rs2.next())
			{
				confirms.add(rs2.getString(1));
			}

			bal = new Balance(deposit, spent,
							  new java.sql.Timestamp(System.currentTimeMillis()),
							  validTime, confirms);
			rs2.close();
			stmt2.close();
			rs.close();
			stmt.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			throw e;
		}
		return bal;
	}

	// Documentation see DBInterface
	public XMLBalance getXmlBalance(long accountnumber)
	{
		long deposit = 0;
		long spent = 0;
		int balance = 0;
		long volume_bytesleft = 0;
		java.sql.Timestamp flat_enddate = null;
		java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
		java.sql.Timestamp validTime = null;
		try {
			Statement stmt = m_dbConn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM ACCOUNTS WHERE ACCOUNTNUMBER=" + accountnumber);
			if (rs.next())
			{
				deposit = DbUtil.intFromDecimal(rs.getBigDecimal("deposit"));
				spent = rs.getLong("spent");
				BigDecimal decimalBalance = rs.getBigDecimal("balance");
				balance = DbUtil.intFromDecimal(decimalBalance);
				volume_bytesleft = rs.getLong("volume_bytesleft");
				flat_enddate = rs.getTimestamp("flat_enddate");
				validTime = rs.getTimestamp("depositvalidtime");
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "account no. " + accountnumber + " is not in database");
			}
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "got balance for account no. " + accountnumber + "from database");
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not get Balance from database");
			return null;
		}
		//private key parameter is null since we don't have it (signing will be handled by PICommandUser)
		return new XMLBalance(accountnumber,deposit,spent,now,validTime,balance,volume_bytesleft,flat_enddate,null);
	}


	public Vector getCostConfirmations(long accountnumber)
	{
		Vector userCCs = new Vector();
		XMLEasyCC curCC;
		try {
			Statement stmt = m_dbConn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT XMLCC FROM COSTCONFIRMATIONS WHERE ACCOUNT_ACCOUNTNUMBER=" +
											 accountnumber);
			while (rs.next())
			{
				curCC = new XMLEasyCC(rs.getString(1));
				userCCs.add(curCC);
			}
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not get CCs from database, returning empty vector");
		}
		return userCCs;
	}



	/***************************************************************************
	 * DATABASE CLEANUP
	 ***************************************************************************/

	/** Run once a day/week/month (depending on server load) to keep
	 * the database small, clean and fast
	 *
	 * @author Bastian Voigt
	 */
	private class CleanupThread implements Runnable
	{
		public void run()
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Thread successfully started");
			while (true)
			{
				try
				{
					Thread.sleep(1000 * 60 * 60 * 24 * 10); // do it every 10 days
				}
				catch (InterruptedException e)
				{
					break;
				}

				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Database.cleanup starting");
				try
				{
					Statement stmt = m_dbConn.createStatement();

					// delete old transfer numbers (invalid for more than 60 days)
					stmt.execute("DELETE FROM TRANSFERS WHERE VALIDTIME < '" +
								 new java.sql.Timestamp(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 60) +
								 "'");

					/*// delete old accounts that have not been used for
					// more than half a year.
					stmt.execute("DELETE FROM ACCOUNTS WHERE DEPOSITVALIDTIME < '" +
								 new java.sql.Timestamp(System.currentTimeMillis() -
						1000 * 60 * 60 * 24 * 150) + "'");*/

					// routine maintenance commands for postgresql
					stmt.execute("VACUUM"); // free unused diskspace
					stmt.execute("ANALYZE"); // make queries faster
				}
				catch (SQLException e)
				{
					LogHolder.log(LogLevel.ERR, LogType.PAY, "Error occured during database cleanup");
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Database.cleanup done.");
			}
		}
	}

	public void startCleanupThread()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Starting Cleanup Thread ...");
		Thread t = new Thread(new CleanupThread());
		t.start();
	}

	/***************************************************************************
	 * ACCOUNT NUMBER HANDLING
	 ***************************************************************************/

	// Documentation see DBInterface
	public void addAccount(long accountNumber,
						   String xmlPublicKey,
						   Timestamp creationTime,
						   String accountCert) throws Exception
	{
		//Account is valid for two months
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(creationTime.getTime());
		cal.add(Calendar.MONTH, 2);
		Timestamp validTime = new Timestamp(cal.getTimeInMillis());

		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "DataBase.addAccount() called for accountnumber " + accountNumber);
		String statement =
			"INSERT INTO ACCOUNTS(accountnumber, xmlpublickey,deposit,depositvalidtime,spent, creation_time, accountcert, volume_bytesleft, balance, blocked) VALUES (" +
			accountNumber + ",'" + xmlPublicKey +
			"',0,'" + validTime + "',0,'" + creationTime + "','"
			+ accountCert +  "',0,0,0 )";
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate(statement);
			stmt.close();
		}
		catch (SQLException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not create account no. " + accountNumber);
			throw new Exception();
		}
	}

	// Documentation see DBInterface class
	public long getNextAccountNumber() throws Exception
	{
		boolean weiter = true;
		long accountnum = 0;
		Statement stmt;
		ResultSet rs;
		try
		{
			while (weiter)
			{
				stmt = m_dbConn.createStatement();
				accountnum = rnd.nextLong();
				if (accountnum < 0)
				{
					accountnum *= -1;
				}
				while (accountnum > 999999999999l)
				{
					accountnum /= 10; // account accountnumbers should
				}
				if (accountnum < 100000000000l)
				{
					accountnum += 100000000000l; // always have twelve digits!
				}
				rs = stmt.executeQuery("select * from accounts where accountnumber=" + accountnum);
				weiter = rs.next();
			}
		}
		catch (SQLException e)
		{
			throw new Exception();
		}
		return accountnum;
	}

	//**************************************************************************
	 // TRANSFER NUMBER HANDLING
	 //**************************************************************************

	  // Documentation see DBInterface class
	  public long getNextTransferNumber() throws Exception
	  {
		  boolean weiter = true;
		  long transfernum = 0;
		  Statement stmt;
		  ResultSet rs;
		  try
		  {
			  while (weiter)
			  {
				  stmt = m_dbConn.createStatement();
				  transfernum = rnd.nextLong();
				  if (transfernum < 0)
				  {
					  transfernum *= -1;
				  }
				  while (transfernum > 99999999999l)
				  {
					  transfernum /= 10; // account transfernumbers should
				  }
				  if (transfernum < 10000000000l)
				  {
					  transfernum += 10000000000l; // always have twelve digits!

					  //add checksum digit
				  }
				  transfernum = calculateDiederChecksum(transfernum);

				  rs = stmt.executeQuery("select * from transfers where transfernumber=" + transfernum);
				  weiter = rs.next();
			  }
		  }
		  catch (SQLException e)
		  {
			  throw new Exception();
		  }
		  return transfernum;
	  }

	// Documentation see DBInterface class
	public void storeTransferNumber(long transfer_num,
									long account_num,
									long deposit,
									java.sql.Timestamp validTime) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "DataBase.storeTransferNumber() called for transfer no. " +
					  transfer_num + ", account no. " + account_num);
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate("INSERT INTO TRANSFERS VALUES (" + transfer_num + "," +
							   account_num + "," + deposit + ",'" + validTime + "','f', NULL, NULL)");
			stmt.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "DataBase.storeTransferNumber() Could not add transfer number " + transfer_num +
						  " to DB");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			throw e;
		}
	}

	// Documentation see DBInterface
	public void setTransferNumberUsed(long transfer_num) throws Exception
	{
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate("UPDATE TRANSFERS SET USED='T' WHERE TRANSFERNUMBER=" +
							   transfer_num);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			throw e;
		}
	}

	// Documentation see DBInterface class
	public String getXmlPublicKey(long accountnumber) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "DataBase.getPubKey() called for account no. " + accountnumber);
		String strXmlKey = null;
		Statement stmt = m_dbConn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT XMLPUBLICKEY FROM ACCOUNTS WHERE ACCOUNTNUMBER=" +
										 accountnumber);
		if (rs.next())
		{
			strXmlKey = rs.getString(1);
		}
		rs.close();
		stmt.close();

		return strXmlKey;
	}

	/**
	 * insertCC: inserts a settled CC in to the database
	 * and (unlike the name implies!) also updates the user account to deduct the spent balance
	 * All checks for validity of the CC are done in PICommandAI
	 *
	 * called by PICommandAI.settle() for the first CC of a combination (JAP account, AI)
	 *
	 * @param cc XMLEasyCC
	 * @throws Exception
	 */
	public void insertCC(XMLEasyCC cc) throws Exception
	{
		ResultSet rs;
		Statement stmt = m_dbConn.createStatement();
		//get next sequence number to use as id
		String getSequenceNumber = "SELECT nextval('costconfirmations_id_seq')";
		rs = stmt.executeQuery(getSequenceNumber);
		rs.next();
		int curSequenceNumber = rs.getInt(1);
		//set id in the cc object
		cc.setId(curSequenceNumber);
		//insert cc in database
		String query =
			"INSERT INTO COSTCONFIRMATIONS(id,account_accountnumber, transferredbytes, cascade_id, xmlcc) VALUES (" +
			cc.getId() + "," + cc.getAccountNumber() + "," + cc.getTransferredBytes() + ",'" + cc.getCascadeID() + "','"+
			XMLUtil.toString(XMLUtil.toXMLDocument(cc)) +
			"')";
		if (stmt.executeUpdate(query) != 1)
		{
			// error while updating DB
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Wrong number of affected rows");
			throw new Exception("Wrong number of affected rows!");
		}
		//debiting user and crediting mixes is handled centrally in PICommanAI.settle()
	}

	/**
	 * updateCC: stores a new CC in the PI database after it has been accepted by the PI as valid
	 * and updates the jap and mix balances accordingly
	 *
	 * called by PICommandAI.settle() for new CCs of existing combinations (JAPaccount, AI)
	 * (the first CC of a new cascade uses insertCC() )
	 *
	 * @param cc XMLEasyCC
	 * @throws Exception
	 */
	public void updateCC(XMLEasyCC cc) throws Exception
	{
		ResultSet rs;
		Statement stmt = m_dbConn.createStatement();

		//find right

		String query =
			"UPDATE COSTCONFIRMATIONS SET TRANSFERREDBYTES=" + cc.getTransferredBytes() +
			",XMLCC='" + XMLUtil.toString(XMLUtil.toXMLDocument(cc)) +
			"' WHERE ACCOUNT_ACCOUNTNUMBER=" + cc.getAccountNumber() +
			" AND cascade_id='" + cc.getCascadeID() + "'";
		if (stmt.executeUpdate(query) != 1)
		{
			// error while updating DB
			// error while updating DB
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Wrong number of affected rows");
			throw new Exception("Wrong number of affected rows!");

		}
		//debiting user and crediting mixes is handled centrally in PICommanAI.settle()
	}

	/**
	 * debitAccount: debits a user account for a given amount of bytes
	 * (will check whether a flatrate exists,
	 * if yes debit from its volume, if not debit from balance according to pricecerts and traffic)
	 *
	 * @param accountnumber long: the jap account to debit from
	 * @param priceCertElements Hashtable: subjectkeyidentifiers and priceCertHashes of mixes of the cascade, as returned by XMLEasyCC.getPriceCertElements()
	 * @param traffic long: the amount of bytes to debit for
	 * @return boolean: whether debiting from the account worked or not
	 */
	public boolean debitAccount(long accountnumber, Hashtable priceCertElements, long traffic)
	{
		try
		{
			Statement stmt = m_dbConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
			String getuser = "select * from accounts where accountnumber = " + accountnumber;
			ResultSet userResult = stmt.executeQuery(getuser);
			//check if user exists
			if (! userResult.next() ) //calling next() once is enough, accountnumber is primary key -> unique -> getting more than one record is impossible
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "the user to debit from doesnt exist");
				return false;
			}
			//check if user has valid flatrate, if yes debit from it
			Date userEndDate = (Date) userResult.getTimestamp("flat_enddate"); //cast to Date to get rid of nanoseconds and ensure comparison
			long userVolumeBytesleft = userResult.getLong("volume_bytesleft")*1000;
			Date now = new Date();
			if (now.before(userEndDate) && userVolumeBytesleft > traffic   ) //small residual amount of bytes is worthless
			{
				long newVolumeInDb = (userVolumeBytesleft-traffic)/1000; //Database stores in KB, we calculate in bytes
				userResult.updateLong("volume_bytesleft",newVolumeInDb);
			}
			else
			//no flatrate -> charge according to pricecerts
			{
				Enumeration mixIds = priceCertElements.keys();
				BigDecimal wholeCost = new BigDecimal("0.00000000");
				while (mixIds.hasMoreElements() )
				{
					String curId = (String) mixIds.nextElement();
					String curHash = (String) priceCertElements.get(curId);
					curHash = XMLUtil.stripNewlineFromHash(curHash);
					if (curHash == null)
					{
						; //mix has no price cert, is non-pay, skip
					} else
					{
						//since we're dealing with money, the calculation is optimized for clarity and readability,
						//@TODO: could be shortened/number of BigDecimal instantiations reduced to improve performance if necessary
						XMLPriceCertificate curPriceCert = this.getPriceCertForHash(curHash);
						double curPrice = curPriceCert.getRate();
						BigDecimal priceInEurosPerGigabyte = new BigDecimal(curPrice);
						BigDecimal gigaByteScale = new BigDecimal("1000000000.00000000");
						BigDecimal trafficInGigabyte = new BigDecimal(traffic).divide(gigaByteScale,DECIMAL_SCALE,BigDecimal.ROUND_HALF_UP);
						//System.out.println("traffic in gigabyte:"+trafficInGigabyte);
						wholeCost.setScale(DECIMAL_SCALE,BigDecimal.ROUND_HALF_UP);
						BigDecimal curCost = trafficInGigabyte.multiply(priceInEurosPerGigabyte);
						curCost.setScale(DECIMAL_SCALE,BigDecimal.ROUND_HALF_UP); //adjust scale of wholeCost and curCost, otherwise caculations go wrong
						wholeCost = wholeCost.add(curCost);
					}
				}
				//only now that we know the whole price, see if user has enough balance and deduct if yes
				//(dont deduct in the while-loop above, otherwise if the user's balance is >0 but < wholeprice,
				//some mixes of the same cascade would get paid,and others not)
				BigDecimal userBalance = userResult.getBigDecimal("balance");
				if (userBalance.compareTo(wholeCost) < 0) //compare to returns -1 if first param is smaller than second
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Insufficient balance to settle the CC");
					return false; //return without paying or increasing spent
				} else
				{
					userResult.updateBigDecimal("balance",userBalance.subtract(wholeCost));
					System.out.println("amount charged to the account: "+wholeCost);
				}
			}
			//no matter how the user paid, increase the cumulative spent by traffic
			long oldspent = userResult.getLong("spent");
			userResult.updateLong("spent",oldspent+traffic);
			//finally, write all updates to the account to the database
			userResult.updateRow();
		} catch (Exception e)
		{
			System.out.println("Exception while debiting user account: " +e.getMessage());
			return false;
		}
		return true;
	}

	public boolean creditMixes(Hashtable priceCertElements, long traffic) throws SQLException
	{
		Enumeration mixIds = priceCertElements.keys();
		try
		{
			//start transaction (to avoid paying some mixes and others not in case of database error,
			//makes it easier to manually unravel failed settling of CCs
			m_dbConn.setAutoCommit(false);
			while (mixIds.hasMoreElements() )
			{
				String curId = (String) mixIds.nextElement();
				String curHash = (String) priceCertElements.get(curId);
				curHash = XMLUtil.stripNewlineFromHash(curHash);
				if (curHash == null)
				{
					; //mix has no price cert, is non-pay, skip
				} else
				{
					//calculate amount
					XMLPriceCertificate curPriceCert = this.getPriceCertForHash(curHash);
					BigDecimal curPrice = new BigDecimal(curPriceCert.getRate());
					BigDecimal curTraffic = BigDecimal.valueOf(traffic);
					BigDecimal scalingFactor = new BigDecimal("1000000000");
					BigDecimal trafficInGigaBytes = curTraffic.divide(scalingFactor,DECIMAL_SCALE,BigDecimal.ROUND_HALF_UP);
					BigDecimal curMixAmount = curPrice.multiply(trafficInGigaBytes);

					double debug = trafficInGigaBytes.doubleValue();
					double debug2 = curMixAmount.doubleValue();


					//get mix record
					Statement stmt = m_dbConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
					String getMix = "Select * from mixaccounts where subjectkeyidentifier = '" + curId +"'";
					ResultSet mixResult = stmt.executeQuery(getMix);
					if (! mixResult.next() ) //subjectkeyidentifier is unique, so only call next() once
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not find the mix to pay");
						//we still want to pay the other mixes, so no exception thrown here
						return false;
					}
					//increase mix balance
					BigDecimal oldBalance = mixResult.getBigDecimal("balance");
					mixResult.updateBigDecimal("balance",oldBalance.add(curMixAmount));
					mixResult.updateRow();
					//commit transaction
					m_dbConn.commit();
				}
			} //of while
		} catch (Exception e)
		{
			m_dbConn.rollback();
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "exception while trying to credit the mixes");
			return false;
		} finally {
			m_dbConn.setAutoCommit(true);
		}

		return true;
	}

	/**
	 * writeMixStats: increase mixaccounts.traffic_since_clearing
	 * whenever CCs are settled
	 *
	 * @param affectedMixes Enumeration: Subjectkeyidentifiers of all Mixes to update
	 * @param bytes long: Nr of new bytes transferred over these Mixes
	 */
	public void writeMixStats(Enumeration affectedMixes, long bytes)
	{
		try
		{




			//update traffic_since_clearing for each mix
			Statement statStatement = m_dbConn.createStatement();
			String statSql = "update mixaccounts set traffic_since_clearing = traffic_since_clearing+" + bytes +
								" where subjectkeyidentifier in ('";
			int nrOfMixes = 0;
			while (affectedMixes.hasMoreElements() )
			{
				String curSubjectKeyIdentifier = (String) affectedMixes.nextElement();
				statSql = statSql + curSubjectKeyIdentifier;
				if (affectedMixes.hasMoreElements() )
				{
					statSql = statSql + "','"; //add comma only if another entry will follow
				}
				nrOfMixes++;
			}
			statSql = statSql + "')";
			if (statStatement.executeUpdate(statSql) != nrOfMixes)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Wrong number of mixes affected!");
			}
			statStatement.close();
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not set Mix stats");
		}
	}

	/**
	 * writeMixTraffic: log increased traffic per mix and month in table mixtraffic
	 * whenever CCs are settled
	 *
	 * @param affectedMixes Enumeration: Subjectkeyidentifiers of all Mixes to update
	 * @param bytes long: Nr of new bytes transferred over these Mixes
	 */
	public void writeMixTraffic(Enumeration affectedMixes, long bytes)
	{
		try
		{
			//update table mixtraffic
			while (affectedMixes.hasMoreElements())
			{
				//get id (mixaccount.id, in param affectedMixes: mixaccount.subjectkeyidentifier)
				Statement getMixStmt = m_dbConn.createStatement();
				String curSubjectKeyIdentifier = (String) affectedMixes.nextElement();
				String getMixSql = "Select * from mixaccounts where subjectkeyidentifier = '" +
					curSubjectKeyIdentifier + "'";
				ResultSet rs = getMixStmt.executeQuery(getMixSql);
				rs.next();
				int curMixId = rs.getInt("id");
				getMixStmt.close();
				//look for previous mixtraffic entry for this month
				Calendar now = Calendar.getInstance();
				int curMonth = now.get(Calendar.MONTH);
				curMonth += 1; //Calendar's months are 0-based, e.g. February would return 1
				int curYear  = now.get(Calendar.YEAR);
				Statement getEntryStmt = m_dbConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
				String getEntrySql = "Select * from mixtraffic where mixaccount_id = " + curMixId +
					        " and year = " + curYear + " and month = " + curMonth;
				ResultSet oldRecord = getEntryStmt.executeQuery(getEntrySql);
				if (oldRecord.next() )
				{
					//if exists update the record and save
					long oldBytes = oldRecord.getLong("traffic");
					long oldNrOfCcs = oldRecord.getLong("number_of_costconfirmations");
					oldRecord.updateLong("traffic",oldBytes + bytes);
					oldRecord.updateLong("number_of_costconfirmations", oldNrOfCcs + 1);
					oldRecord.updateRow(); //will throw exception on failure
				}
				else
				{
					//write new data for this month
					Statement insertStmt = m_dbConn.createStatement();
					String insertSql = "insert into mixtraffic(mixaccount_id,month, year, traffic, number_of_costconfirmations) values( "
						+ curMixId + " , " + curMonth + " , " + curYear + ", " + bytes + " , 1) ";
					int insertedRows = insertStmt.executeUpdate(insertSql);
					if (insertedRows != 1)
					{
						throw new Exception(); //throw to catch clause to log
					}
					insertStmt.close();
				}
				getEntryStmt.close();
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not write mix traffic to database");
		}
	}

	public void writeJapTraffic(long newTraffic, String cascadeId, long accountNumber)
	{
		try
		{
			//check for older record for this month
			Calendar now = Calendar.getInstance();
			int curMonth = now.get(Calendar.MONTH);
			curMonth += 1; //Calendar's months are 0-based, e.g. February would return 1
			int curYear  = now.get(Calendar.YEAR);
			Statement getEntryStmt = m_dbConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
			String getEntrySql = "Select * from japtraffic where cascade_id = '" + cascadeId +
					        "' and account_accountnumber = " + accountNumber + " and year = " + curYear + " and month = " + curMonth;
			ResultSet oldRecord = getEntryStmt.executeQuery(getEntrySql);
			if (oldRecord.next() )
			{
				//if exists update the record and save
				long oldBytes = oldRecord.getLong("traffic");
				long oldNrOfCcs = oldRecord.getLong("number_of_costconfirmations");
				long newTrafficValue = oldBytes + newTraffic;
				oldRecord.updateLong("traffic",newTrafficValue);
				oldRecord.updateLong("number_of_costconfirmations", oldNrOfCcs + 1);
				oldRecord.updateRow(); //will throw exception on failure
			}
			else
			{
				//write new data for this month
				Statement insertStmt = m_dbConn.createStatement();
				String insertSql = "insert into japtraffic(cascade_id,account_accountnumber, month, year, traffic, number_of_costconfirmations) values( '"
					+ cascadeId + "' , " + accountNumber+" , "+ curMonth + " , " + curYear + ", " + newTraffic + " , 1) ";
				int insertedRows = insertStmt.executeUpdate(insertSql);
				if (insertedRows != 1)
				{
					throw new Exception(); //throw to catch clause to log
				}
					insertStmt.close();
			}
			getEntryStmt.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not write jap traffic to database");
			System.out.println(e.getMessage());
		}
	}




	public XMLEasyCC getCC(long accountNumber, String cascadeID) throws Exception
	{
		Statement stmt = m_dbConn.createStatement();
		XMLEasyCC cc = null;
		//we use the whole varchar for the constructor, since not all elements of the cc have their own column (e.g. pricecerthashes)
		ResultSet rs = stmt.executeQuery(
			"SELECT XMLCC FROM COSTCONFIRMATIONS " +
			"WHERE ACCOUNT_ACCOUNTNUMBER=" + accountNumber +
			" AND cascade_id='" + cascadeID + "'");
		if (rs.next())
		{
			cc = new XMLEasyCC(rs.getString(1));
		}
		return cc;
	}

	/*	public long getCosts(long accountNumber, String aiName) throws Exception
	 {
	  long costs;
	  try
	  {
	   Statement stmt = m_dbConn.createStatement();
	   ResultSet rs =
	 stmt.executeQuery("SELECT COSTS FROM CASCADES WHERE ACCOUNTNUMBER=" + accountNumber +
	 " AND CASCADENUMBER=(SELECT CASCADENUMBER FROM CASCADENAMES WHERE NAME='" +
	 aiName + "')");
	   if (rs.next())
	   {
	 costs = rs.getInt(1);
	   }
	   else
	   {
	 costs = 0;
	   }
	   return costs;
	  }
	  catch (SQLException e)
	  {
	   throw e;
	  }

	 }*/

	/*	public long getPayCosts(long accountNumber, String aiName) throws Exception
	 {
	  long costs;
	  try
	  {
	   Statement stmt = m_dbConn.createStatement();
	   ResultSet rs =
	 stmt.executeQuery("SELECT PAYCOSTS FROM CASCADES WHERE ACCOUNTNUMBER=" + accountNumber +
	 " AND CASCADENUMBER=(SELECT CASCADENUMBER FROM CASCADENAMES WHERE NAME='" +
	 aiName + "')");
	   if (rs.next())
	   {
	 costs = rs.getInt(1);
	   }
	   else
	   {
	 costs = 0;
	   }
	   return costs;
	  }
	  catch (SQLException e)
	  {
	   throw e;
	  }

	 }*/

	// Documentation see DBInterface class
	//creates all database tables
	//new tables are empty, all existing data is dropped!
	//also populates the paymentoptions (plus its various string tables) and paymentsettings tables
	public void createTables()
	{
		Statement stmt;
		int num_tables = create_statements.length;

		for (int i = 0; i < num_tables; i++)
		{
			try
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "Creating Database " + db_tables[i]);
				stmt = m_dbConn.createStatement();
				stmt.executeUpdate(create_statements[i]);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "DataBase.createTables: Could not create " + db_tables[i]);
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}
		}

		int num_indexes = create_indexes.length;
		for (int i = 0; i < num_indexes; i++)
		{
			try
			{
				stmt = m_dbConn.createStatement();
				String indexSql = create_indexes[i];
				stmt.executeUpdate(indexSql);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,"Index creation failed at index nr. "+(new Integer(i)).toString());
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}
		}

		this.setPaymentSettings();
		this.setPaymentOptions();
		this.setVolumePlans();
	}

	/** Drops all tables */
	public void dropTables()
	{
		Statement stmt;

		int num_objects = db_tables.length;

		for (int i = 0; i < num_objects; i++)
		{
			try
			{
				stmt = m_dbConn.createStatement();
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "Dropping old Database " + db_tables[i]);
				stmt.executeUpdate("DROP " + db_tables[i] + " CASCADE");
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "DataBase.dropTables: Could not drop " + db_tables[i] + "!");
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}
		}
	}

	/*   protected void finalize () throws Throwable
	  {
	 try {
	 con.close();
	 }
	 catch (Exception e)
	 {
	  LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
	 }
	 super.finalize();
	  }*/

	public void chargeAccount(long a_transferNumber, long a_amount)
	{
		Statement stmt;
		long account;
		boolean used;
		Timestamp validTime = null;

		try
		{
			//Get account for transfernumber
			stmt = m_dbConn.createStatement();
			ResultSet r = stmt.executeQuery("SELECT ACCOUNT_ACCOUNTNUMBER, USED FROM TRANSFERS WHERE TRANSFERNUMBER=" +
											a_transferNumber);
			if (r.next())
			{
				account = r.getLong(1);
				used = r.getBoolean(2);
			}
			else
			{
				throw new Exception("Transfer no. " + a_transferNumber + " is not in database");
			}

			//Check if account has expired
			stmt = m_dbConn.createStatement();
			r = stmt.executeQuery("SELECT DEPOSITVALIDTIME FROM ACCOUNTS WHERE ACCOUNTNUMBER=" + account);
			if (r.next())
			{
				validTime = r.getTimestamp(1);
			}
			//Update deposit
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "Charging account " + account + " with " + a_amount);

			if (!used && validTime.after(new Date()))
			{
				Date usedDate = new Date();
				stmt = m_dbConn.createStatement();
				//amount is in eurocent, database BigDecimal in whole euros
				BigDecimal euroCent = new BigDecimal(a_amount);
				BigDecimal wholeEuros = euroCent.divide(BigDecimal.valueOf(100),BigDecimal.ROUND_HALF_UP);
				stmt.executeUpdate("UPDATE ACCOUNTS SET DEPOSIT=DEPOSIT+" + wholeEuros + ", balance = balance +" + wholeEuros + "WHERE ACCOUNTNUMBER=" +
								   account);
				//Set transfer number to "used"
				stmt = m_dbConn.createStatement();
				stmt.executeUpdate("UPDATE TRANSFERS SET USED=TRUE, USEDTIME=" + usedDate.getTime() +
								   ", AMOUNT=" + a_amount +
								   " WHERE TRANSFERNUMBER=" +
								   a_transferNumber);
			}
			else
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY, "Transfer number already used or account expired.");
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Could not charge account: " + e.getMessage());

		}
	}

	public boolean isTanUsed(long a_tan)
	{
		boolean used = false;
		Statement stmt;

		try
		{
			stmt = m_dbConn.createStatement();
			ResultSet r = stmt.executeQuery("SELECT USED FROM TRANSFERS WHERE TRANSFERNUMBER=" +
											a_tan);
			if (r.next())
			{
				used = r.getBoolean(1);
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
						  "Could not get used attribute of transfer number. Cause: " + e.getMessage());
		}
		return used;
	}

	public long getUsedDate(long a_tan)
	{
		long usedDate = 0;
		Statement stmt;

		try
		{
			stmt = m_dbConn.createStatement();
			ResultSet r = stmt.executeQuery("SELECT USEDTIME FROM TRANSFERS WHERE TRANSFERNUMBER=" +
											a_tan);
			if (r.next())
			{
				usedDate = r.getLong(1);
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
						  "Could not get used attribute of transfer number. Cause: " + e.getMessage());
		}
		return usedDate;

	}

    /**
     * Gets the charging amount for a certain transfer number
     * @param a_tan long
     * @return long
     */
    public long getTransferAmount(long a_tan)
	{
		long amount = 0;
		Statement stmt;

		try
		{
			stmt = m_dbConn.createStatement();
			ResultSet r = stmt.executeQuery("SELECT AMOUNT FROM TRANSFERS WHERE TRANSFERNUMBER=" +
											a_tan);
			if (r.next())
			{
				amount = r.getLong(1);
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
						  "Could not get amount attribute of transfer number. Cause: " + e.getMessage());
		}
		return amount;
	}

	public void storePassivePayment(XMLPassivePayment a_passivePayment) throws Exception
	{
		double foo = a_passivePayment.getAmount(); //debug

		String data = a_passivePayment.getAllPaymentData();
		String statement =
			"INSERT INTO PASSIVEPAYMENTS VALUES (" +
			"DEFAULT, " +
			a_passivePayment.getTransferNumber() + "," +
			a_passivePayment.getAmount() + "," +
			"'" + a_passivePayment.getCurrency() + "'," +
			"'" + data + "'," +
			"FALSE, '" +
			a_passivePayment.getPaymentName() + "')";
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate(statement);
			stmt.close();
		}
		catch (SQLException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not store passive payment info");
			throw new Exception();
		}

	}

	public void storePaysafecardPayment(XMLPassivePayment pp) throws Exception
	{
		String merchantId = Configuration.getMerchantId();
		String amount = new Double(pp.getAmount()).toString();
		String currency = pp.getCurrency();
		long tan = pp.getTransferNumber();
		String transactionId = merchantId+(new Long(tan)).toString();
		//state of 'created' is default of database column, so no need to set it

		String insertSql = "INSERT INTO PAYSAFECARDPAYMENTS("+
			"MERCHANT_ID,TRANSACTION_ID,AMOUNT,CURRENCY,TRANSFERS_TRANSFERNUMBER,CREATIONTIME)"+
			"VALUES('"+merchantId+"','"+transactionId+"','"+amount+"','"+
			currency+"',"+tan+",'"+new java.sql.Timestamp(System.currentTimeMillis())+"')";
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate(insertSql);
			stmt.close();
		} catch (SQLException e)
		{
			LogHolder.log(LogLevel.ERR,LogType.PAY, "Could not store psc payment");
			throw new Exception();
		}
	}

	/**
	 * purgePaysafecardPayments
	 * housekeeping function
	 * deletes:
	 * - payments ("dispositions" in PSC terminology) that have been created but not confirmed ("disposed") in time
	 * - payments that have been executed, and are older than [log_expiration]
	 */
	public void purgePaysafecardPayments()
	{
		try
		{
			//clear old, executed payments
			Integer deleteIn = Configuration.getLogExpiration();
			long now = System.currentTimeMillis();
			long deletion = deleteIn.intValue() * 24 * 60 * 60 * 1000;
			Timestamp deletionTime = new Timestamp(now - deletion);

			String deleteSql = "delete from paysafecardpayments where state='C' and creationtime < ' " +
				deletionTime + "'";
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate(deleteSql);
			stmt.close();

			//clear expired payments
			Integer timeout = Configuration.getDispositionTimeout();
			long expired = now - (timeout.intValue() * 60 * 1000);
			Timestamp expirationTime = new Timestamp(expired);

			String deleteSql2 = "delete from paysafecardpayments where state='X' and creationtime < ' " +
				expirationTime + "'";
			Statement stmt2 = m_dbConn.createStatement();
			stmt2.executeUpdate(deleteSql2);
			stmt2.close();
		}
		catch (SQLException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "error while cleaning up paysafecardpayments",e);
		}
	}

	/**
	 * getUsablePaysafecardPayments:
	 * get all those PSC payments that have been both created and confirmed by the user in time
	 * i.e. payment from which we can debit
	 *
	 * @return Vector: of XMlPassivePayment
	 */
	public Vector getUsablePaysafecardPayments()
	{
		Vector usablePayments = new Vector();
		try
		{
			String selectSql = "Select * from paysafecardpayments where state = 'D' ";
			Statement stmt = m_dbConn.createStatement();
			ResultSet rs = stmt.executeQuery(selectSql);
			stmt.close();
			while (rs.next())
			{
				XMLPassivePayment pp = new XMLPassivePayment();
				pp.setCurrency(rs.getString("currency"));
				pp.setAmount(Double.parseDouble(rs.getString("amount")));
				pp.addData("merchant_id",rs.getString("merchant_id"));
				pp.addData("transaction_id",rs.getString("transaction_id"));
				long tan = rs.getLong("transfernumber");
				//reconstruct tan from transaction id if not set
				//WARNING: assumes that transaction id = merchantid+transaction id!
				if (tan == 0)
				{
					String  mtid = rs.getString("transaction_id");
					//ignore first 10 digits = merchant id
					mtid = mtid.substring(10);
					tan = Long.parseLong(mtid);
				}
				pp.setTransferNumber(tan);
				usablePayments.add(pp);
			}
		}
		catch (SQLException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "database error while getting psc payments", e);
		}
		return usablePayments;
	}

	public void setPaysafecardPaymentUsed(XMLPassivePayment pp)
	{
		try
		{
			String mtid = pp.getPaymentData("transaction_id");
			String updateSql = "update paysafecardpayments set state='X' where transaction_id ='" + mtid +"'";
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate(updateSql);
			stmt.close();
		} catch (SQLException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "database error while updating psc payment",e);
		}
	}

	public Vector getPendingPassivePayments()
	{
		Vector payments = new Vector();
		String[] passivePayment;
		String statement =
			"SELECT ID, PAYMENTTYPE, TRANSFER_TRANSFERNUMBER, AMOUNT, CURRENCY FROM PASSIVEPAYMENTS WHERE CHARGED=FALSE";
		try
		{
			Statement stmt = m_dbConn.createStatement();
			ResultSet r = stmt.executeQuery(statement);
			while (r.next())
			{
				passivePayment = new String[5];
				passivePayment[0] = String.valueOf(r.getLong(1));
				passivePayment[1] = r.getString(2);
				passivePayment[2] = String.valueOf(r.getLong(3));
				passivePayment[3] = String.valueOf(r.getLong(4));
				passivePayment[4] = r.getString(5);
				payments.addElement(passivePayment);
			}
			stmt.close();
			return payments;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not retrieve passive payments from database");
			return null;
		}
	}

	public XMLPassivePayment getPassivePaymentData(String transfernumber)
	{
		XMLPassivePayment paymentdata = null;
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String sql = "select * from passivepayments where transfer_transfernumber ="+transfernumber;
			ResultSet rs = stmt.executeQuery(sql);
			if (!rs.next() ) throw new Exception(); //should be exactly one record per transfernumber
			paymentdata = new XMLPassivePayment();
			paymentdata.setAmount(rs.getDouble("amount"));
			paymentdata.setCurrency(rs.getString("currency"));
			paymentdata.setPaymentName(rs.getString("paymenttype"));
			paymentdata.setCharged(rs.getBoolean("charged"));
			//data is a String of format ref=value\nref=value
			String dataString = rs.getString("data");
			StringTokenizer st = new StringTokenizer(dataString,"\n");
			while (st.hasMoreTokens() )
			{
				String oneLine = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(oneLine,"=");
				//all lines should be in forma ref=value, so just grab the first two tokens
				paymentdata.addData(st2.nextToken(),st2.nextToken());
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not get passive payment data from database");
		}
		return paymentdata;
	}

	public void markPassivePaymentDone(long a_id)
	{
		String statement = "UPDATE PASSIVEPAYMENTS SET CHARGED=TRUE WHERE ID=" + a_id;
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate(statement);
			stmt.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not update passive payment in database");
		}

	}

	//used by PICommandMC

	public void storePriceCert(XMLPriceCertificate a_priceCertificate) throws Exception
	{
		//get values from MixCert XML
		String subjectKeyIdentifier = a_priceCertificate.getSubjectKeyIdentifier();
		double rate = a_priceCertificate.getRate();
		rate = (Math.rint( rate * 1000 )) / 1000;
		String BiID = a_priceCertificate.getBiID();
		java.sql.Timestamp signatureTime = a_priceCertificate.getSignatureTime();
		int mixid;
		Document signedDoc = a_priceCertificate.getDocument();
		String signedXml = XMLUtil.toString(signedDoc);

		//get id of Mix corresponding to the Mixcerthash contained in the PriceCert
		Statement stmt = m_dbConn.createStatement();
		String selectString = "SELECT ID, SUBJECTKEYIDENTIFIER FROM MIXACCOUNTS WHERE SUBJECTKEYIDENTIFIER='"+subjectKeyIdentifier+"'";
		ResultSet rs = stmt.executeQuery(selectString);
		rs.next();
		mixid = rs.getInt(1);
		stmt.close();

		//store PriceCert in Database
		//create hash value
		String hash = Base64.encodeString(XMLSignature.getHashValueOfElement(a_priceCertificate.getDocument()));

		//insert in db
		Statement st = m_dbConn.createStatement();
		//if cert is unsigned, signaturetime will be null!
		String sqlInsert;
		if (signatureTime == null)
		{
			sqlInsert = "INSERT INTO PRICECERTS(mixaccount_id,subjectkeyidentifier,rate,biid,hash) VALUES ("
				+ mixid + ",'" + subjectKeyIdentifier + "'," + new Double(rate).toString() + ",'" + BiID +
				"','" + hash + "')";
		}
		else
		{
			sqlInsert = "INSERT INTO PRICECERTS(mixaccount_id,subjectkeyidentifier,rate,biid,signaturetime, hash, signedxml) VALUES ("
				+ mixid + ",'" + subjectKeyIdentifier + "'," + new Double(rate).toString() + ",'" + BiID +
				"'," + signatureTime.toString() + ",'" + hash + "','" + signedXml + "')";
		}
		int result = st.executeUpdate(sqlInsert);
		if (result != 1)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Database returned an error when trying to insert PriceCert");
		}
		st.close();
	}








	public void deletePriceCert(XMLPriceCertificate a_priceCertificate) throws Exception
	{
		//get values from MixCert XML
		/***
		the database id is NOT contained in the xml structure
		we'll rely on a combination of subjectkeyidentifier(=mix), rate and signaturetime to identify the pricecert to delete
		reasonable assumption: no mix will have two pricecerts with the same price signed at the same time
		***/
		String ski  = a_priceCertificate.getSubjectKeyIdentifier();
		double rate = a_priceCertificate.getRate();
		Timestamp signatureTime = a_priceCertificate.getSignatureTime();

		//delete PriceCert from Database
		Statement stmt = m_dbConn.createStatement();
		//signatureTime as timestamp needs to be in quotes, but null without quotes
		String sqlDelete = "";
		if (signatureTime == null)
		{
			sqlDelete = "delete from pricecerts where subjectkeyidentifier = '" + ski +
				"' and signaturetime is null and rate = " + DbUtil.formatDoubleForSql(rate);
		}
		else
		{
			sqlDelete = "delete from pricecerts where subjectkeyidentifier = '" + ski +
				"' and signaturetime = '" + signatureTime + "' and rate = " + DbUtil.formatDoubleForSql(rate);
		}
		int result = stmt.executeUpdate(sqlDelete);
		if (result != 1)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Database error when trying to delete PriceCert");
		}
		stmt.close();
	}











	/**
	 * handleTransferRequest: do the necessary when a MixOperator requests to be paid
	 * (i.e. for the balance in his Mix's mixaccounts to be transferred to his real money bank account)
	 *
	 * Currently just sets the transferrequest column to 1 to signal the Webapp that this operator is due to be paid
	 *
	 * @param theRequest XMLTransferRequest
	 */
	public void handleTransferRequest(XMLTransferRequest theRequest)
	{
		if (theRequest.getRequested() != 1)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "unknown transfer request, nothing to be done in database");
			return;
		}
		String operator = theRequest.getOperatorCert();
		String statement = "UPDATE mixoperators SET transferrequest=1 WHERE operatorcert = '"+operator+"'";
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate(statement);
			stmt.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not update mixoperators to signal transfer request");
		}
	}

	/**
	 * get Price Certificates for a given MixOperator
	 *
	 * @param operatorCert String
	 * @return Vector containing XMLPriceCertificate Objects
	 *         might be empty in case there was some database error
	 */
	public Vector getPriceCerts(String operatorCert)
	{
		Vector allPriceCerts = new Vector();
		try
		{
			Statement stmt = m_dbConn.createStatement();

			//get Mixes owned by this operator
			Vector operatorMixes = this.getOwnedMixIds(operatorCert);

			//get price certs for those mixes
			String priceCertQuery = "SELECT * FROM PRICECERTS WHERE MIXACCOUNT_ID =";
			String orderQuery = " order by signaturetime desc";
			XMLPriceCertificate aPriceCert;
			for (Enumeration e = operatorMixes.elements(); e.hasMoreElements(); )
			{
				Integer mixId = (Integer) e.nextElement(); //keeping as Integer, not int, since we need .toString() in the query
				//get database record
				ResultSet rs = stmt.executeQuery(priceCertQuery + mixId.toString() + orderQuery);
				while (	rs.next() )
				{
					//construct XMLPriceCertificate and add to Vector
					String signedWholeCert = rs.getString("SIGNEDXML");
					if (signedWholeCert == null || signedWholeCert.equals("") || !signedWholeCert.startsWith("<?xml"))
					{
						aPriceCert = new XMLPriceCertificate(rs.getString("SUBJECTKEYIDENTIFIER"),
							rs.getDouble("RATE"), rs.getString("BIID"));

					}
					else
					{
						aPriceCert = new XMLPriceCertificate(signedWholeCert.getBytes());
					}
					allPriceCerts.add(aPriceCert);
				}
			}
			if (allPriceCerts.size() == 0)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "no price certificates exist for the Mix");
				//might simply be the case that no price certs have been created yet
				//alternatively, you could return null if you want the dialog to fail if no pricecerts exist
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Could not get price certificates for operator");
		}
		finally
		{
			return allPriceCerts;
		}
	}


	/**
	 *  get the amount of money that the given MixOperator is due to receive from the PI
	 *
	 * @param operatorCert String
	 * @return int the sum of the balances of all the Mixes owned by the given Operator
	 */
	public  int getOperatorBalance(String operatorCert)
	{
		int theBalance = 0; //we're summing up several ints, but the individual balances should be way smaller than the maximum value of an int
		int operatorId = getIdForOperatorCert(operatorCert);
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String getBalances = "Select sum(balance) from mixaccounts where mixoperator_id="+operatorId;
			ResultSet rs = stmt.executeQuery(getBalances);
			rs.next(); //can be only one record per operator
			theBalance = DbUtil.intFromDecimal(rs.getBigDecimal(1));
		} catch (Exception e) {
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Could not get balance for operator's mixes");
		}
		return theBalance;
	}

	/**
	 * getOperatorInfo
	 *
	 * @param operatorCert String: Base64-encoded
	 * @return String: human-readable String with more information about the operator whose cert was given
	 *    (Format is not guaranteed and not intended for further processing,
	 *       might me sth like "Operator-id: 5, Nr. of Mixes: 2, Balance: 12.34 Euro" )
	 */
	public String getOperatorInfo(String operatorCert)
	{
		String opInfo = new String("");
		try{
			//collect some info using other methods
			int balance = this.getOperatorBalance(operatorCert);
			int nrOfMixes = this.getOwnedMixIds(operatorCert).size();

			//get id of operator
			Statement stmt = m_dbConn.createStatement();
			String getId = "Select id from mixoperators where operatorcert = '"+operatorCert+"'";
			ResultSet rs = stmt.executeQuery(getId);
			rs.next(); //can be only one operator with this cert
			int id = rs.getInt(1);

			//construct info string
			opInfo = "Nr.(= database id) of the Operator: " + (new Integer(id)).toString()+"\n"
			        +"This operator is running " + (new Integer(nrOfMixes)).toString()+" Mixes\n"
					+"Current balance (unformatted database value): " + (new Integer(balance)).toString();

		} catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "could not get operator info");
		}
		return opInfo;
	}

	/**
	 * To be used with getOperatorBalance():
	 * Most current date of the last Update of all the Mixes involved
	 *
	 * @param operatorCert String
	 * @return Timestamp
	 */
	public java.sql.Timestamp getLastBalanceUpdate(String operatorCert)
	{
		int operatorId = getIdForOperatorCert(operatorCert);
		java.sql.Timestamp lastUpdate = null;
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String getLastTime = "Select max(updated_on) from mixaccounts where mixoperator_id ="+operatorId;
			ResultSet rs = stmt.executeQuery(getLastTime);
			rs.next(); //using select max, so we get exactly one record
			lastUpdate = rs.getTimestamp(1);
			stmt.close();
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Could not get time of last balance update");
		}
		return lastUpdate;
	}

	private Vector getOwnedMixIds (String operatorCert)
	{
		Vector theMixes = new Vector();
		try
		{
			int operatorId = getIdForOperatorCert(operatorCert);
			//get Mixes for Operator Id
			Statement stmt = m_dbConn.createStatement();
			String getMixes = "Select id from mixaccounts where mixoperator_id ="+operatorId;
			ResultSet rs = stmt.executeQuery(getMixes);
			while (rs.next() )
			{
				theMixes.add(new Integer( rs.getInt(1) ));
			}
			stmt.close();
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Could not get Mixes for Operator");
		}
		return theMixes;
	}

	private int getIdForOperatorCert(String operatorCert)
	{
		int operatorId = 0;
		try
		{
			//get id of Operator
			Statement stmt = m_dbConn.createStatement();
			String getOperatorId = "Select id from mixoperators where operatorcert ='" + operatorCert + "'";
			ResultSet rs = stmt.executeQuery(getOperatorId);
			rs.next(); // just go to first record, operatorCert is unique, so we are sure to get only one record
			operatorId = rs.getInt(1);
			stmt.close();
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Could not get id for Operator Certificate");
		}
		return operatorId;
	}

	/**
	 * getBankAccount: returns bank data (type and details) necessary to pay the MixOperator
	 *
	 */
	public  String[] getBankAccount(String operatorCert)
	{
		String[] bankData = new String[2];
		int operatorId = getIdForOperatorCert(operatorCert);
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String getBankData = "Select transfertype,transferdetails from mixoperators where id ="+operatorId;
			ResultSet rs = stmt.executeQuery(getBankData);
			rs.next();
			bankData[0] = rs.getString(1);
			bankData[1] = rs.getString(2);
			stmt.close();
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Could not get bank account data");
		}
		return bankData;
	}

	public  void storeBankAccount(XMLBankAccount accountData)
	{
		String accountType = accountData.getType();
		String accountDetails = accountData.getDetails();
		String operatorCert = accountData.getOperatorCert();
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String storeBankData = "update mixoperators set transfertype='"+accountType+"',transferdetails='"+accountDetails+"' where operatorcert ='"+operatorCert+"'";
			int result = stmt.executeUpdate(storeBankData);
			if (result != 1)
			{
				throw new Exception();
			}
			//if everything worked, do nothing

		} catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Could not store bank account data");
		}
	}

	//utility function, returns the full price certificate corresponding to a hash value of it, as stored in a CC
	public XMLPriceCertificate getPriceCertForHash(String hashValue)
	{
		XMLPriceCertificate thePriceCert = null;
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String getAllPriceCerts = "Select hash,id from pricecerts where hash='"+hashValue+"'";
			ResultSet rs = stmt.executeQuery(getAllPriceCerts);
			int id;
			if (rs.next())
			{
				id = rs.getInt("id"); //get id for the price cert so we can fetch the whole cert
			} else //no price cert with matching hash found in db
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "no matching price certificate found in database");
				return null;
			}
			//get whole cert data
			stmt = m_dbConn.createStatement();
			String getCertData =
				"Select subjectkeyidentifier,rate,signaturetime,biid,signedxml from pricecerts where id = " + id;
			ResultSet rs2 = stmt.executeQuery(getCertData);

			if (rs2.next())
			{
				String signatureString = rs2.getString("signedxml");
				if (signatureString == null)
				{
					thePriceCert = new XMLPriceCertificate(rs.getString("SUBJECTKEYIDENTIFIER"),
						rs.getDouble("RATE"),rs.getString("BIID"));
				}
				else
				{
					thePriceCert = new XMLPriceCertificate(signatureString.getBytes());
				}
			}
			stmt.close();
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,e.getMessage());
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not get whole price cert for hash from database");
		}

		return thePriceCert;
	}

	/**
	 * getPriceCertsForMix
	 *
	 * @param a_subjectKeyIdentifier String: the SubjectKeyIdentifier of the Mix for which you want to get the price certs
	 * @return Vector: contains one XMLPriceCertificate per record found in database for the given mix
	 *                 rather than an empty Vector, null is returned if no corresponding price cert is found in database
	 */
	public Vector getPriceCertsForMix(String a_subjectKeyIdentifier)
	{
		Vector theCerts = new Vector();
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String getCerts = "Select * from pricecerts where subjectkeyidentifier = '"+a_subjectKeyIdentifier+"' order by signaturetime desc";
			ResultSet rs = stmt.executeQuery(getCerts);
			XMLPriceCertificate aPriceCert;
			while (rs.next() )
			{
				String signatureString = rs.getString("signedxml");
				if (signatureString == null || signatureString.equals("") || !signatureString.startsWith("<?xml") )
				{
					aPriceCert = new XMLPriceCertificate(rs.getString("SUBJECTKEYIDENTIFIER"),
						rs.getDouble("RATE"),rs.getTimestamp("SIGNATURETIME"), rs.getString("BIID"));

				}
				else
				{
					aPriceCert = new XMLPriceCertificate(signatureString.getBytes());
				}
				theCerts.add(aPriceCert);
			}
			if (theCerts.size() == 0)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "no price certificates exist for the Mix");
				//might simply be the case that no price certs have been created yet
				//alternatively, you could return null if you want the dialog to fail if no pricecerts exist
			}
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not get price certs for mix from database");
		}
		return theCerts;
	}

	/**
	 * getOperatofOfMix
	 *
	 * @param subjectKeyIdentifier String
	 * @return JAPCertificate, might be null if an error occured
	 */
	public JAPCertificate getOperatorOfMix(String subjectKeyIdentifier)
	{
		JAPCertificate opCert = null;
		try {
			Statement stmt = m_dbConn.createStatement();
			String getOpCertForMix = "select operatorcert from mixoperators, mixaccounts where mixaccounts.mixoperator_id = mixoperators.id and mixaccounts.subjectkeyidentifier = '" +
				subjectKeyIdentifier + "'"; ;
			String opCertString = stmt.executeQuery(getOpCertForMix).getString(1);
			byte decoded[] = Base64.decode(opCertString);
			opCert = JAPCertificate.getInstance(decoded);
		}catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not get operator cert for the given Mix");
		}
		return opCert;
	}

	/**
	 * Does NOT do any validity checks, call check Validity() beforehand
	 * (therefor NOT threadsafe)
	 *
	 * @param code String: the coupon code to redeem
	 * @param transferNumber long
	 * @throws Exception
	 */
	public void redeemCoupon(String code, long transferNumber) throws Exception
	{
		m_dbConn.setAutoCommit(false);
		//get coupon value
		Statement stmt = m_dbConn.createStatement();
		String couponSql = "Select amount from coupons where couponcode = '"+code+"'";
		ResultSet rs = stmt.executeQuery(couponSql);
		if (! rs.next() )
		{
			throw new Exception ("Coupon should be valid, but does not exist");
		}
		int amount = rs.getInt("amount");
		//charge account
		chargeAccount(transferNumber,amount);
		//mark coupon as used
		String accountSql = "Select account_accountnumber from transfers where transfernumber = "+transferNumber;
		ResultSet rs2 = stmt.executeQuery(accountSql);
		rs2.next();
		long accountNumber = rs2.getLong("account_accountnumber");
		String updateSql = "update coupons set redeemed_on = current_timestamp, accounts_accountnumber = "+accountNumber+" where couponcode ='"+code+"'";
		stmt.executeUpdate(updateSql);
		try
		{
			m_dbConn.commit();
		} catch (SQLException e)
		{
			m_dbConn.rollback();
		}
		m_dbConn.setAutoCommit(true);
	}

	public boolean checkValidity(String code) throws Exception
	{
		//check if code exists
		Statement stmt = m_dbConn.createStatement();
		String validSql = "select * from coupons where couponcode = '" + code + "'";
		ResultSet rs = stmt.executeQuery(validSql);
		if (! rs.next() ) //no record found
		{
			return false;
		}
		//check for doublespending
		Timestamp redeemed = rs.getTimestamp("redeemed_on");
		if (redeemed != null)
		{
			return false;
		}
		//check for expiration
		Timestamp expiry = rs.getTimestamp("valid_until");
		Timestamp now = new Timestamp(System.currentTimeMillis());
		if (expiry != null && expiry.before(now))
		{
			return false;
		}
		//everything okay
		return true;
	}

	/**
	 * buyFlatrate: buy a flatrate (i.e. set enddate/volumebytes according to Configuration)
	 * deducting the price from the account's balance
	 *
	 * @param a_accountnumber long
	 * @return XMLErrorMessage:
	 *   ERR_OK
	 *   ERR_INSUFFICIENT_BALANCE : if the account's balance is lower than the price of the flatrate
	 *   ERR_NO_FLATRATE_OFFERED
	 */
	public XMLErrorMessage buyFlatrate(long a_accountnumber){
		//options to set
		long volumeAmount = 0;
		Timestamp endDate;
		int flatratePrice = 0;
		//send error if flat not enabled at all
		boolean flatEnabled;
		String flatEnabledString = this.getFlatrateConfigOption("FlatEnabled");
		if (flatEnabledString == null)
		{
			flatEnabled = false;
		}
		else
		{
			flatEnabled = Boolean.valueOf(flatEnabledString).booleanValue();
		}
		if (!flatEnabled)
		{
			return new XMLErrorMessage(XMLErrorMessage.ERR_NO_FLATRATE_OFFERED,"Flatrates are deactivated or not configured");
		}
		//get options for duration
		boolean durationLimited = Boolean.valueOf(this.getFlatrateConfigOption("DurationLimited")).booleanValue();
		if (durationLimited)
		{
			int flatrateDuration = Integer.parseInt(this.getFlatrateConfigOption("FlatrateDuration"));
			String flatrateDurationUnit = this.getFlatrateConfigOption("FlatrateDurationUnit");
		}
		//get options for volume
		boolean volumeLimited = Boolean.valueOf(this.getFlatrateConfigOption("VolumeLimited")).booleanValue();
		if (volumeLimited)
		{
			volumeAmount = Long.parseLong(this.getFlatrateConfigOption("VolumeAmount"));
		}
		//set huge volume if unlimited
		if (!volumeLimited)
		{
			volumeAmount = 1000000000; //"1 Terabyte should be enough for anybody"
		}
		//calculate and set flat enddate according to option value, or 2 years from now if not constrained
		if (!durationLimited)
		{
			//2 years ahead
			Calendar twoYearsAhead = Calendar.getInstance();
			twoYearsAhead.add(Calendar.YEAR,2);
			endDate = new Timestamp(twoYearsAhead.getTimeInMillis());
		} else
		{
			//calculate from unit and value
			String unit = this.getFlatrateConfigOption("FlatrateDurationUnit");
			int duration = Integer.parseInt(this.getFlatrateConfigOption("FlatrateDuration") );
			Calendar now = Calendar.getInstance();
			if (unit.equals("days") || unit.equals("day") )
			{
				now.add(Calendar.DATE,duration);
			} else if (unit.equalsIgnoreCase("weeks") || unit.equalsIgnoreCase("week")  )
			{
				now.add(Calendar.WEEK_OF_YEAR,duration);
			} else if (unit.equalsIgnoreCase("months") || unit.equalsIgnoreCase("month")  )
			{
				now.add(Calendar.MONTH,duration);
			}
 			else if (unit.equalsIgnoreCase("years") || unit.equalsIgnoreCase("year") )
			{
				now.add(Calendar.YEAR,duration);
			}
			endDate = new Timestamp(now.getTimeInMillis());
		}
		try
		{
			//get user account data
			//not sure if updating an updatable ResultSet is transaction-safe, so better use a conventional .executeUpdate
			//Statement stmt = m_dbConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
			Statement stmt = m_dbConn.createStatement();
			String getUser = "Select * from accounts where accountnumber = " + a_accountnumber;
			ResultSet rsUser = stmt.executeQuery(getUser);
			rsUser.next();
			//check the user's balance to see if he can afford the flatrate
			//conversion, in database: decimal in euros, needed: integer in eurocent
			BigDecimal bdUserBalance = rsUser.getBigDecimal("balance");
			int userBalance = DbUtil.intFromDecimal(bdUserBalance);
			flatratePrice = Integer.parseInt(this.getFlatrateConfigOption("FlatratePrice"));
			//flatrate price in config is in eurocent, balance in whole euros
			BigDecimal decimalFlatratePrice = BigDecimal.valueOf(flatratePrice/100);
			if ( ! (userBalance >= flatratePrice) )
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Account no. "+a_accountnumber+" has insufficient funds to purchase a flatrate");
				return new XMLErrorMessage(XMLErrorMessage.ERR_INSUFFICIENT_BALANCE);
			}
			//Timestamp (unlike Date) contains nanoseconds, which postgres doesnt like, so we have to do some formatting
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String endDateString = sdf.format(endDate);
			//change values for user account
			Statement updateStatement = m_dbConn.createStatement();
			BigDecimal newBalance = bdUserBalance.subtract(decimalFlatratePrice);
			String updateAccount = "update accounts set balance = "+ newBalance +" , flat_enddate =' " + endDateString + " ', volume_bytesleft = " + volumeAmount + " where accountnumber = " + a_accountnumber;
			updateStatement.executeUpdate(updateAccount);
		} catch (SQLException e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not buy flatrate for user No. "+a_accountnumber+" due to a database error");
			return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR);
		}
		//if everything okay, return ERR_OK
		return new XMLErrorMessage(XMLErrorMessage.ERR_OK);
	}

	/**
	 * getFlatrateConfig
	 * returns the current Configuration (as stored in the BI's database) affecting payment plans (flatrate, volume-based etc)
	 * (deliberately NOT called getPaymentSettings() to avoid confusion: setPaymentSettings IS the functional equivalent,
	 * but is called only when dropping and re-creating the db tables,
	 * changes should be made via the PI-GUI, not directly in the database)
	 *
	 * @return Hashtable: each entry corresponds to a record in the Paymentsettings table
	 * does not contain a fixed set of options, but whatever is found in that table
	 * (if the option you need is not contained in the returned Hashtable,
	 * call Configuration.getXXX() to fall back to the value of the config file or, if that is not set either,
	 * a default value defined in Configuration)
	 */
	public Hashtable getFlatrateConfig() {
		//values are read from databae, NOT from config-file
		//reason: allows PI-GUI to change values
		Hashtable allPaymentSettings = new Hashtable();
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String getAllSettings = "Select name, curvalue from paymentsettings"; //no condition, get all
			ResultSet rs = stmt.executeQuery(getAllSettings);
			while (rs.next() )
			{
				allPaymentSettings.put(rs.getString(1),rs.getString(2));
			}
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not get payment settings from database, returning empty Hashtable");
		}
		return allPaymentSettings;
	}

	/**
	 * like getFlatrateConfig, but only for a single setting that is passed as String
	 * (in CamelCase version. e.g. "FlatEnabled", NOT "flat_enabled")
	 *
	 * if the given key is not contained in the database,
	 * the method will try to get it from Configuration via a getXXX- or isXXX-Method
	 * (saves you from having to manually check Configuration every time if your key is not found in the db)
	 *
	 * @param key String
	 * @return String (even boolean etc settings are returned as String!)
	 * returns null if the key is contained in neither the db nor the Configuration
	 */
	public String getFlatrateConfigOption(String key){
		Hashtable dbValues = this.getFlatrateConfig();
		//if key is contained in db, return the corresponding value
		if (dbValues.containsKey(key) )
		{
			return (String) dbValues.get(key);
		}
		//if not, check if there is a getXXX-Method in Configuration
		Object value;
		try {
			Method getter = Class.forName("jpi.Configuration").getMethod("get"+key, null);
			value = getter.invoke(null,null);

		} catch (Exception e) //e.g. NoSuchMethodException
		{
			//if not, try a isXXX-Method
			try {
				Method getter = Class.forName("jpi.Configuration").getMethod("get" + key, null);
				value = getter.invoke(null, null);
			}
			catch (Exception e2)
			{
				//if this option is not set in neither the db nor Configuration, return null
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "no configuration found for key "+key);
				return null;
			}
		}
		//the following might be simplified by fancier reflection, sth like casting to value.getClass().getName()
		//this part will fail for primitive values!
		//so make sure all relevant Configuration-getters return Object, e.g. Integer instead of int
		if (value instanceof String)
		{
			return (String) value;
		} else if (value instanceof Integer)
		{
			return ((Integer) value).toString();
		}
		else if (value instanceof Long)
		{
			return ((Long) value).toString();
		}
		else if (value instanceof Boolean)
		{
			return ((Boolean) value).toString();
		} else {
			return null; //shouldnt happen
		}
	}

	private void setPaymentSettings() {
	    //for each option to be read from Configuration and stored in db,
		//put Name of the Attribute (method name of getter minus get/is) and Class into the hashtable


		String optionName;
		String optionValue;
		String optionType;
		Method getterMethod = null;
		Enumeration keys = paymentSettings.keys();
		try {
			while (keys.hasMoreElements())
			{
				optionName = (String) keys.nextElement();
				optionType = (String) paymentSettings.get(optionName);
				if (optionType.equals("Boolean"))
				{
					getterMethod = Class.forName("jpi.Configuration").getMethod("is" + optionName, null);
				}
				else
				{
					getterMethod = Class.forName("jpi.Configuration").getMethod("get" + optionName, null);
				}
				optionValue = getterMethod.invoke(null, null).toString(); //null,null means static method (no object), no arguments

				Statement paystatement = m_dbConn.createStatement();
				String paySettings = "insert into paymentsettings(name,curvalue,datatype) values('" + optionName +
					"','" + optionValue + "','" + optionType + "')";
				paystatement.executeUpdate(paySettings);
			}
		} catch (Exception e) {
			//lots of exceptions in this method that we can do nothing about, so to avoid a big mess we handle them all here generically
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not write payment setting to database");
		}
	}

	private void setVolumePlans()
	{
		XMLVolumePlans allPlansFromFile = Configuration.getVolumePlans(true);
		Vector plans = allPlansFromFile.getVolumePlans();
		for (Enumeration thePlans = plans.elements(); thePlans.hasMoreElements(); )
		{
			try
			{
				XMLVolumePlan curPlan = (XMLVolumePlan) thePlans.nextElement();
				//get next records id (we'll need to set it in the other tables)
				Statement stmt = m_dbConn.createStatement();
				String getSequenceNumber = "SELECT nextval('volumeplans_id_seq')";
				ResultSet rs = stmt.executeQuery(getSequenceNumber);
				rs.next();
				int curPlanId = rs.getInt(1);

				//put single fields into paymentoptions table
				String sql = "insert into volumeplans(id,name,price,volume_limited, duration_limited, volume_kbytes, duration, duration_unit)" +
				 " values ("+curPlanId+",'" + curPlan.getName() + "'," + curPlan.getPrice() + ","
					+ curPlan.isVolumeLimited() + "," + curPlan.isDurationLimited() + "," + curPlan.getVolumeKbytes()+"," + curPlan.getDuration() + ",'" + curPlan.getDurationUnit()+ "')";
				Statement optionStmt = m_dbConn.createStatement();
				int affectedRows = optionStmt.executeUpdate(sql);
				if (affectedRows != 1)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "database error while inserting paymentoptions");
				}
				optionStmt.close();
			} catch (Exception e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not write volume plans from file to database",e);
			}
		}
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "volume plans were read from config file and stored in database table volumeplans");
	}

	private void setPaymentOptions() {
		XMLPaymentOptions allOptions = Configuration.getPaymentOptions(true); //true = get options from file
		Vector options = allOptions.getAllOptions();
		for (Enumeration theOptions = options.elements(); theOptions.hasMoreElements();  )
		{
			try
			{
				XMLPaymentOption curOption = (XMLPaymentOption) theOptions.nextElement();

				//get next records id (we'll need to set it in the other tables)
				Statement stmt = m_dbConn.createStatement();
				String getSequenceNumber = "SELECT nextval('paymentoptions_id_seq')";
				ResultSet rs = stmt.executeQuery(getSequenceNumber);
				rs.next();
				int curOptionId = rs.getInt(1);

				//put single fields into paymentoptions table
				String type = curOption.getType();
				String sql = "insert into paymentoptions(id,name,paymenttype,generic,japversion)" +
			             " values ("+curOptionId+",'" + curOption.getName() + "','" + type + "',"
					+ curOption.isGeneric() + ",'" + curOption.getMinJapVersion() + "')";
				Statement optionStmt = m_dbConn.createStatement();
				int affectedRows = optionStmt.executeUpdate(sql);
				if (affectedRows != 1)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "database error while inserting paymentoptions");
				}

				optionStmt.close();

				//put language-specific fields into paymentoptionstrings table
				Vector optionLangs = curOption.getLanguages();
				String curLang;
				String curHeading;
				String curDetailedInfo;
				Statement langStmt = m_dbConn.createStatement();
				String langSql;
				for (Enumeration optionLanguages = optionLangs.elements(); optionLanguages.hasMoreElements(); )
				{
					curLang = (String) optionLanguages.nextElement();
					curHeading = curOption.getHeading(curLang);
					curDetailedInfo = curOption.getDetailedInfo(curLang);
					langSql = "insert into paymentoptionstrings(paymentoption_id,language,heading,detailedinfo) values(" +
					          + curOptionId + ",'" + curLang + "','" + curHeading + "','" + curDetailedInfo + "')";
					affectedRows = langStmt.executeUpdate(langSql);
					if (affectedRows != 1)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "database error while inserting paymentoptionstrings");
					}
				}
				langStmt.close();

				//put language-specific fields for passive payments into passivepaymentstrings table
				Statement inputStmt = m_dbConn.createStatement();
				Vector allInputs = curOption.getInputFields();
				String inputSql;
				String[] curInput;
				//each Input is a String[3] of ref, label, lang (will break if format of getInputFields() is changed in XMLPaymentOption)
				for (Enumeration inputs = allInputs.elements(); inputs.hasMoreElements(); )
				{
					curInput = (String[]) inputs.nextElement();
					inputSql = "insert into passivepaymentstrings(paymentoption_id,language, inputlabel, ref) values("+
							   + curOptionId + ",'" + curInput[2] + "','" + curInput[1] + "','" + curInput[0] + "')";
					affectedRows = inputStmt.executeUpdate(inputSql);
					if (affectedRows != 1)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "database error while inserting activepaymentstrings");
					}
				}
				inputStmt.close();

				//put language-specific fields for active payments into activepaymentstrings table
				Statement extraInfoStmt = m_dbConn.createStatement();
				Vector allExtraInfos = curOption.getExtraInfos();
				String extraInfoSql;
				String[] curExtraInfo;
				//each ExtraInfo is a String[3] of info, type, lang (will break if format of getExtraInfos() is changed in XMLPaymentOption)
				for (Enumeration extraInfos = allExtraInfos.elements(); extraInfos.hasMoreElements(); )
				{
					curExtraInfo = (String[]) extraInfos.nextElement();
					extraInfoSql = "insert into activepaymentstrings(paymentoption_id,language, extrainfolabel, labeltype) values("+
							   + curOptionId + ",'" + curExtraInfo[2] + "','" + curExtraInfo[0] + "','" + curExtraInfo[1] + "')";
					affectedRows = extraInfoStmt.executeUpdate(extraInfoSql);
					if (affectedRows != 1)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "database error while inserting activepaymentstrings");
					}
				}
				extraInfoStmt.close();


			} catch (Exception e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not write configuration to database",e);
			}
		}
	}

	/**
	 * getPaymentSettings: get all entries currently in the paymentsettings table
	 *
	 * @return XMLPaymentSettings
	 */
	public XMLPaymentSettings getPaymentSettings()
	{
		Hashtable allSettings = new Hashtable();
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String sql = "select name, curvalue from paymentsettings"; //no where-condition, get all
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next() )
			{
				allSettings.put(rs.getString("name"), rs.getString("curvalue") );
			}
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not read payment settings from database");
		}
		XMLPaymentSettings theSettings = new XMLPaymentSettings(allSettings);
		return theSettings;
	}

	public XMLPaymentOptions getPaymentOptionsFromDb()
	{
		XMLPaymentOptions allOptions = new XMLPaymentOptions();
		//set currencies and credit cards
		//(could be stored in database, too, but currently we're not even using currencies, so hard-code it here to avoid needing another table)
		allOptions.addCurrency("EUR");

		try
		{
			//get accepted credit cards from table paymentsettings
			Statement creditcardStmt = m_dbConn.createStatement();
			String cardSql = "Select curvalue from paymentsettings where name='AcceptedCreditCards'";
			ResultSet cardRs = creditcardStmt.executeQuery(cardSql);
			if (cardRs.next() )
			{
				String cardString = cardRs.getString("curvalue");
				allOptions.setAcceptedCreditCards(cardString);
			}
			else
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY, "No credit cards found in database, should be an entry in table Paymentsettings with name='AcceptedCreditCards'");
				allOptions.setAcceptedCreditCards("");
			}

			//get records from table paymentoptions
			XMLPaymentOption curOption;
			int curOptionId;
			Statement optionStmt = m_dbConn.createStatement();
			String optionSql = "Select * from paymentoptions";
			ResultSet rs = optionStmt.executeQuery(optionSql);
			String curName;
			String curType;
			boolean curIsGeneric;
			String curJapVersion;
			while (rs.next() )
			{
				//construct XMLPaymentOption
				curName = rs.getString("name");
				curType = rs.getString("paymenttype");
				curIsGeneric = rs.getBoolean("generic");
				curJapVersion = rs.getString("japversion");
				curOption = new XMLPaymentOption(curName,curType,curIsGeneric,curJapVersion);
				curOptionId = rs.getInt("id");

				//add fields from paymentoptionstrings
				Statement optionStringStatement = m_dbConn.createStatement();
				String optionStringSql = "Select * from paymentoptionstrings where paymentoption_id = " + curOptionId;
				String curLang;
				String curHeading;
				String curDetailedInfo;
				ResultSet rs2 = optionStringStatement.executeQuery(optionStringSql);
				//iterating over languages
				while (rs2.next() ) //no of languages is not fixed, so no point in checking no of records returned
				{
					curLang = rs2.getString("language");
					curHeading = rs2.getString("heading");
					curDetailedInfo = rs2.getString("detailedinfo");
					curOption.addDetailedInfo(curDetailedInfo,curLang);
					curOption.addHeading(curHeading,curLang);
				}
				optionStringStatement.close();

				//add fields from passivepaymentstrings
				Statement inputStatement = m_dbConn.createStatement();
				String inputSql = "Select * from passivepaymentstrings where paymentoption_id = " + curOptionId;
				ResultSet rs3 = inputStatement.executeQuery(inputSql);
				String curLabel;
				String curRef;
				while (rs3.next() )
				{
					curLang = rs3.getString("language");
					curLabel = rs3.getString("inputlabel");
					curRef = rs3.getString("ref");
					curOption.addInputField(curRef,curLabel,curLang);
				}
				inputStatement.close();

				//add fields from activepaymentstrings
				Statement extraInfoStatement = m_dbConn.createStatement();
				String extraInfoSql = "Select * from activepaymentstrings where paymentoption_id = " + curOptionId;
				ResultSet rs4 = extraInfoStatement.executeQuery(extraInfoSql);
				String curExtraInfoLabel;
				String curExtraInfoType;
				while (rs4.next() )
				{
					curLang = rs4.getString("language");
					curExtraInfoLabel = rs4.getString("extrainfolabel");
					curExtraInfoType = rs4.getString("labeltype");
					curOption.addExtraInfo(curExtraInfoLabel,curExtraInfoType,curLang);
				}
				inputStatement.close();

				allOptions.addOption(curOption);
			}
			optionStmt.close();
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not read configuration from database");
		}
		return allOptions;
	}
	/**
	 *
	 * @return XMLVolumePlans: all currently defined Volume plans
	 */
	public XMLVolumePlans getVolumePlans()
	{
		XMLVolumePlans allPlans = new XMLVolumePlans();
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String getPlansSql = "Select * from volumeplans";
			ResultSet rs = stmt.executeQuery(getPlansSql);
			while (rs.next())
			{
				String curName = rs.getString("name");
				int curPrice = rs.getInt("price");
				boolean curVolumeLimited = rs.getBoolean("volume_limited");
				boolean curDurationLimited = rs.getBoolean("duration_limited");
				long curVolumeKbytes = rs.getLong("volume_kbytes");
				int curDuration = rs.getInt("duration");
				String curDurationUnit = rs.getString("duration_unit");
				XMLVolumePlan curPlan;
				if (curVolumeLimited && curDurationLimited)
				{
					curPlan = new XMLVolumePlan(curName,curPrice,curDuration,curDurationUnit,curVolumeKbytes);
				}
				else if (curVolumeLimited)
				{
					curPlan = new XMLVolumePlan(curName,curPrice,curVolumeKbytes);
				}
				else if (curDurationLimited)
				{
					curPlan = new XMLVolumePlan(curName,curPrice,curDuration, curDurationUnit);
				}
				else //unlimited
				{
					curPlan = new XMLVolumePlan(curName,curPrice);
				}
				allPlans.addVolumePlan(curPlan);
			}
		} catch (SQLException sqe)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not return Volume Plans due to a database error");
			return null;
		}
		System.out.println(XMLUtil.toString(XMLUtil.toXMLDocument(allPlans)));
		return allPlans;
	}

	/**
	 *
	 * @param planName String: the name of the plan, as defined in the database via PIG, or the config file
	 * @return XMLVolumePlan, or null if no such plan exists
	 */
	public XMLVolumePlan getVolumePlan(String planName)
	{
		try
		{
			Statement stmt = m_dbConn.createStatement();
			String getPlanSql = "Select * from volumeplans where name = '" + planName + "'";
			ResultSet rs = stmt.executeQuery(getPlanSql);
			if (rs.next())
			{
				//build XMLVolumePlan from record
				String name = rs.getString("name");
				int price = rs.getInt("price");
				boolean durationLimited = rs.getBoolean("duration_limited");
				boolean volumeLimited = rs.getBoolean("volume_limited");
				int duration = rs.getInt("duration");
				String durationUnit = rs.getString("duration_unit");
				long volumeKbytes = rs.getLong("volume_kbytes");

				return new XMLVolumePlan(name,price,duration,durationUnit,volumeKbytes);
			}
			else //no matching record found
			{
				throw new Exception("No volume plan with name " + planName + " exists");
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not get volume plan with name"+planName );
			return null;
		}
	}

	public XMLErrorMessage buyVolumePlan(long accountNumber, XMLVolumePlan thePlan)
	{
		if (thePlan == null)
		{
			return new XMLErrorMessage(XMLErrorMessage.ERR_WRONG_DATA);
		}

		boolean durationLimited = thePlan.isDurationLimited();
		boolean volumeLimited = thePlan.isVolumeLimited();
		long volumeAmount = 0;
		Timestamp endDate;

		//set huge volume if unlimited
		if (!volumeLimited)
		{
			volumeAmount = 1000000000; //"1 Terabyte should be enough for anybody"
		}
		else
		{
			volumeAmount = thePlan.getVolumeKbytes();
		}

		//calculate and set flat enddate according to option value, or 2 years from now if not constrained
		if (!durationLimited)
		{
			//2 years ahead
			Calendar twoYearsAhead = Calendar.getInstance();
			twoYearsAhead.add(Calendar.YEAR,2);
			endDate = new Timestamp(twoYearsAhead.getTimeInMillis());
		} else
		{
			//calculate from unit and value
			String unit = thePlan.getDurationUnit();
			int duration = thePlan.getDuration();
			Calendar now = Calendar.getInstance();
			if (unit.equals("days") || unit.equals("day") )
			{
				now.add(Calendar.DATE,duration);
			} else if (unit.equalsIgnoreCase("weeks") || unit.equalsIgnoreCase("week")  )
			{
				now.add(Calendar.WEEK_OF_YEAR,duration);
			} else if (unit.equalsIgnoreCase("months") || unit.equalsIgnoreCase("month")  )
			{
				now.add(Calendar.MONTH,duration);
			}
			else if (unit.equalsIgnoreCase("years") || unit.equalsIgnoreCase("year") )
			{
				now.add(Calendar.YEAR,duration);
			}
			endDate = new Timestamp(now.getTimeInMillis());
		}

		try
		{
			//get user account data
			Statement stmt = m_dbConn.createStatement();
			String getUser = "Select * from accounts where accountnumber = " + accountNumber;
			ResultSet rsUser = stmt.executeQuery(getUser);
			rsUser.next();

			//check the user's balance to see if he can afford the flatrate
			//conversion, in database: decimal in euros, needed: integer in eurocent
			BigDecimal bdUserBalance = rsUser.getBigDecimal("balance");
			int userBalance = DbUtil.intFromDecimal(bdUserBalance);
			int flatratePrice = thePlan.getPrice();
			//flatrate price in config is in eurocent, balance in whole euros
			BigDecimal decimalFlatratePrice = BigDecimal.valueOf(flatratePrice / 100);
			if (! (userBalance >= flatratePrice))
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "Account no. " + accountNumber + " has insufficient funds to purchase a flatrate");
				return new XMLErrorMessage(XMLErrorMessage.ERR_INSUFFICIENT_BALANCE);
			}
			//Timestamp (unlike Date) contains nanoseconds, which postgres doesnt like, so we have to do some formatting
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String endDateString = sdf.format(endDate);
			//change values for user account
			Statement updateStatement = m_dbConn.createStatement();
			BigDecimal newBalance = bdUserBalance.subtract(decimalFlatratePrice);
			String updateAccount = "update accounts set balance = " + newBalance + " , flat_enddate =' " +
				endDateString + " ', volume_bytesleft = " + volumeAmount + " where accountnumber = " +
				accountNumber;
			updateStatement.executeUpdate(updateAccount);
		} catch (SQLException e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not buy flatrate for user No. "+accountNumber+" due to a database error");
			return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR);
		}

		return new XMLErrorMessage(XMLErrorMessage.ERR_OK);
	}

}
