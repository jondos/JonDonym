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
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Klasse die ein Objekt liefert, welches {@link DBInterface} implementiert.
 * Zur Zeit ist nur der Zugriff auf eine Postgres-Datenbank implementiert.
 * Zur Unterstuetzung einer anderen Datenbank (z.B. MySQL) sind diese Klasse
 * und gegebenenfalls {@link DataBase} anzupassen.
 *
 * @author Andreas Mueller, Elmar Schraml
 */
public abstract class DBSupplier
{
	private static Connection con = null;

	//suggestion for improvement: use non-static variables, and replace initDataBase with getInstance()
	private static String dbHost;
	private static int    dbPort;
	private static String dbUsername;
	private static String dbPassword;
	private static String dbDatabasename;

	/**
	 * Initialisiert die Verbindung zur Datenbank.
	 *
	 * @param databaseHost Datenbank-Host
	 * @param databasePort Datenbank-Port
	 * @param databaseName Datenbank-Name
	 * @param userName Datenbanknutzername
	 * @param password Passwort
	 * @throws Exception
	 */
	public static synchronized void initDataBase(String databaseHost, int databasePort,
												 String databaseName, String userName,
												 String password) throws Exception
	{
		if (con != null)
		{
			closeDataBase();
		}
		Class.forName("org.postgresql.Driver"); //check if driver class is present
		dbHost = databaseHost;
		dbPort = databasePort;
		dbUsername = userName;
		dbPassword = password;
		dbDatabasename = databaseName;

		con = getNewConnection();

	}

	private static synchronized Connection getNewConnection() throws SQLException
	{
		return DriverManager.getConnection("jdbc:postgresql://" + dbHost + ":" +
										  dbPort + "/" + dbDatabasename, dbUsername, dbPassword);
	}

	/**
	 * Liefert ein Objekt welches {@link DBInterface} implementiert.
	 *
	 * @return {@link DataBase}
	 * @throws Exception
	 */
	public static synchronized DBInterface getDataBase() throws Exception
	{
		if (con == null) //connection died
		{
			//try to get a new connection
			try
			{
				con = getNewConnection();
			}
			catch (SQLException e)
			{
				throw new Exception("no connection to database");
			}
		}
		return (DBInterface)new DataBase(con);
	}

	/**
	 * Schliesst die Datenbank.
	 *
	 * @throws Exception
	 */
	public static synchronized void closeDataBase() throws Exception
	{
		con.close();
	}
}
