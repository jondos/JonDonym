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

import java.sql.*;
import java.util.*;

import anon.crypto.*;
import anon.pay.xml.*;

/**
 * The interface to the database. Implementation for Postgresql see
 * {@link DataBase}. To use other databases, simply rewrite this class
 * and add some code to DBSupplier
 */
public abstract class DBInterface
{
	/**
	 * Launches a Thread that performs routine Database maintenance
	 * jobs at regular intervals
	 */
	public abstract void startCleanupThread();

	/**
	 * Returns the account certificate for a public key
	 *
	 * @param pubkey public key
	 * @return account certificate
	 * @throws RequestException
	 */
	/*    public abstract String getCert(anon.crypto.IMyPublicKey pubkey)
	 throws RequestException;*/


	/**
	 * Returns the account number for a public key
	 *
	 * @param pubkey public key
	 * @return account number
	 * @throws Exception
	 */
	/*    public abstract long getAccountNumber(anon.crypto.IMyPublicKey pubkey)
	 throws Exception;*/

	/**
	 * Returns the account balance
	 * Elmar: use of Balance is deprecated and outdated (Balance does not contain the new account balance format)
	 * Used getXmlBalance instead (the same as Balance in IXMLEncodable format, without CCs)
	 * and getCostConfirmations() to get the CCs for an account
	 *
	 * @param accountnumber Kontonummer
	 * @return Kontostand
	 * @throws Exception
	 */
	public abstract Balance getBalance(long accountnumber) throws Exception;

	/**
	 * returns all balance data (new format including flatrate support) for an account
	 * replaces getBalance()
	 *
	 * @param accountnumber long
	 * @return XMLBalance
	 */
	public abstract XMLBalance getXmlBalance(long accountnumber);

	/**
	 * getCostConfirmations
	 *
	 * @param accountnumber long
	 * @return Vector
	 */
	public abstract Vector getCostConfirmations(long accountnumber);

	/**
	 * F\uFFFDgt ein neues Konto in die Datenbank ein.
	 *
	 * @param accountnumber Kontonummer
	 * @param pubkey \uFFFDffentlicher Schl\uFFFDssel
	 * @param validtime G\uFFFDltigkeitsdatum
	 * @param accountcert Kontozertifikat
	 * @throws Exception
	 */
	public abstract void addAccount(long accountNumber,
									String xmlPublicKey,
									java.sql.Timestamp creationTime,
									String accountCert) throws Exception;

	/**
	 * Returns the next unused account number
	 *
	 * @return account number
	 * @throws Exception
	 */
	public abstract long getNextAccountNumber() throws Exception;

	/**
	 * Returns a yet unused transfer number
	 *
	 * @return Transfer number
	 * @throws Exception
	 */
	public abstract long getNextTransferNumber() throws Exception;

	/**
	 * Stores a transfer number with a reference to the account number
	 * and the valid time in the db. Transfer numbers stored using
	 * this function will always be marked as not yet used
	 *
	 * @param transfer_num transfer number
	 * @param account_num account number
	 * @param maxbalance maxbalance at the time of transfernumber generation, stored with
	 * the transfer number to prevent replay attacks, stored in database in table transfers, column deposit
	 * @param validTime valid time
	 * @throws Exception
	 */
	public abstract void storeTransferNumber(long transfer_num,
											 long account_num,
											 long maxbalance,
											 java.sql.Timestamp validTime) throws Exception;

	/**
	 * Sets the "used" flag for the given transfer number.
	 * that means the number was already used for a transaction
	 */
	public abstract void setTransferNumberUsed(long transfer_num) throws Exception;

	/**
	 * Liefert den \uFFFDffentlichen Schl\uFFFDssel zu einer Kontonummer.
	 *
	 * @param accountnumber Kontonummer
	 * @return \uFFFDffentlicher Schl\uFFFDssel
	 * @throws Exception
	 */
	public abstract String getXmlPublicKey(long accountnumber) throws Exception;


	public abstract void insertCC(XMLEasyCC cc) throws Exception;

	public abstract void updateCC(XMLEasyCC cc) throws Exception;

	public abstract void writeMixStats(Enumeration affectedMixes, long traffic);

	public abstract void writeMixTraffic(Enumeration affectedMixes, long traffic);

	/**
	 * writeJapTraffic
	 *
	 * @param newTraffic long
	 * @param firstMix String: aiId = subjectkeyidentifier of first mix
	 * @param accountNumber long
	 */
	public abstract void writeJapTraffic(long newTraffic, String firstMix, long accountNumber);

	public abstract boolean debitAccount(long accountnumber, Hashtable priceCertElements, long traffic);

	public abstract boolean creditMixes(Hashtable priceCertElements, long traffic) throws SQLException;

	public abstract XMLEasyCC getCC(long accountnumber, String aiName) throws Exception;

	/*
	 * Liefert den Kontoschnappschuss bei Abrechnung der Kosten durch eine AI.
	 *
	 * @param aiName Name der Abrechnungsinstanz
	 * @param accountNumber Kontonummer
	 * @param costs Kosten
	 * @param costConfirm Vom Nutzer unterschriebene Kostenbest\uFFFDtigung
	 * @return Kontoschnappschuss
	 * @throws Exception
	 */
	/*	public abstract void storeCosts(anon.pay.xml.XMLEasyCC cc) throws Exception;*/

	/*
	 * Liefert den Kontoschnappschuss eines Nutzerkontos f\uFFFDr eine
	 * Abrechnungsinstanz.
	 *
	 * @param accountNumber Kontonummer
	 * @param aiName Name der Abrechnungsinstanz
	 * @return Kontoschnappschuss
	 * @throws Exception
	 */
	/*	public abstract AccountSnapshot getAccountSnapshot(long accountNumber,
	 String aiName) throws Exception;*/

	/*
	 * Liefert den Kostenstand eines Nutzerkontos bei einer AI.
	 *
	 * @param accountNumber Kontonummer
	 * @param aiName Name der AI
	 * @return Kostenstand
	 * @throws Exception
	 */
	/*	public abstract long getCosts(long accountNumber,
	   String aiName) throws Exception;*/

	/*
	 * Liefert die H\uFFFDhe der durch die BI an die AI beglichenen Kosten eines
	 * Nutzerkontos.
	 *
	 * @param accountNumber Kontonummer
	 * @param aiName Name der AI
	 * @return augezahlte Kosten
	 * @throws Exception
	 */
	/*	public abstract long getPayCosts(long accountNumber,
	   String aiName) throws Exception;*/

	/**
	 * L\uFFFDscht die Tabellen in der Datenbank.
	 */
	public abstract void dropTables();

	/**
	 * Creates the tables in the database
	 */
	public abstract void createTables();

	//    public void closeDataBase();



	////////////////////////////////////////////////////////////////////////////////
	// Functions for Dieder group checksums
	// See c't magazine 4/97 "Bl\uFFFDtenrein"
	// These checksum functions should be used by implementations
	// to add a checksum digit to transfer numbers

	// Multiplication table for Dieder group D5.
	private static final int multipl[][] =
		{
		{
		0, 1, 2, 3, 4, 5, 6, 7, 8, 9}
		,
		{
		1, 2, 3, 4, 0, 6, 7, 8, 9, 5}
		,
		{
		2, 3, 4, 0, 1, 7, 8, 9, 5, 6}
		,
		{
		3, 4, 0, 1, 2, 8, 9, 5, 6, 7}
		,
		{
		4, 0, 1, 2, 3, 9, 5, 6, 7, 8}
		,

		{
		5, 9, 8, 7, 6, 0, 4, 3, 2, 1}
		,
		{
		6, 5, 9, 8, 7, 1, 0, 4, 3, 2}
		,
		{
		7, 6, 5, 9, 8, 2, 1, 0, 4, 3}
		,
		{
		8, 7, 6, 5, 9, 3, 2, 1, 0, 4}
		,
		{
		9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
	};

	/****  Inverse bzgl. Dieder-Multiplikation  ****/
	private static final int inv_multipl[] =
		{
		0, 4, 3, 2, 1, 5, 6, 7, 8, 9};

	/****  Permutationstabelle von Verhoeff  ****/
	private static final int perm[][] =
		{
		{
		2, 8, 6, 3, 4, 1, 0, 9, 7, 5}
		,
		{
		3, 1, 2, 0, 4, 5, 6, 7, 8, 9}
		,
		{
		4, 9, 1, 2, 5, 6, 3, 0, 7, 8}
		,
		{
		4, 8, 5, 0, 9, 7, 2, 6, 3, 1}
		,
		{
		1, 2, 6, 9, 4, 5, 7, 8, 3, 0}
		,
		{
		3, 5, 0, 1, 2, 8, 9, 6, 7, 4}
		,
		{
		3, 8, 9, 4, 7, 2, 6, 1, 5, 0}
		,
		{
		2, 4, 8, 6, 9, 0, 7, 1, 3, 5}
		,
		{
		1, 7, 9, 5, 0, 3, 8, 6, 4, 2}
		,
		{
		1, 8, 2, 9, 5, 0, 4, 7, 6, 3}
		,
		{
		4, 3, 2, 6, 8, 7, 0, 1, 5, 9}
		,
		{
		2, 6, 9, 8, 5, 4, 1, 3, 0, 7}
	};

	/****  inverse Permutationen  ****/
	private static final int inv_perm[][] =
		{
		{
		6, 5, 0, 3, 4, 9, 2, 8, 1, 7}
		,
		{
		3, 1, 2, 0, 4, 5, 6, 7, 8, 9}
		,
		{
		7, 2, 3, 6, 0, 4, 5, 8, 9, 1}
		,
		{
		3, 9, 6, 8, 0, 2, 7, 5, 1, 4}
		,
		{
		9, 0, 1, 8, 4, 5, 2, 6, 7, 3}
		,
		{
		2, 3, 4, 0, 9, 1, 7, 8, 5, 6}
		,
		{
		9, 7, 5, 0, 3, 8, 6, 4, 1, 2}
		,
		{
		5, 7, 0, 8, 1, 9, 3, 6, 2, 4}
		,
		{
		4, 0, 9, 5, 8, 3, 7, 1, 6, 2}
		,
		{
		5, 0, 2, 9, 6, 4, 8, 7, 1, 3}
		,
		{
		6, 7, 2, 1, 0, 8, 3, 5, 4, 9}
		,
		{
		8, 6, 0, 7, 5, 4, 1, 9, 3, 2}
	};

	/** Calculates the Dieder group Checksum for a long integer and
	  adds the checksum digit to the end
	  @return the number with the checksum digit added at the end
	 */
	public static long calculateDiederChecksum(long number)
	{
		long temp = number;
		int num_digits = 0;
		long power = 0;
		int right = -1, left = 0;
		int k = 0;

		// calculate number of digits
		while (temp > 0)
		{
			temp /= 10;
			num_digits++;
		}
		num_digits++; // one more for the checksum

		// calculate "right" and "left checksum"
		for (temp = 0; temp < num_digits - 1; temp++)
		{

			// extract the current digit
			power = 1;
			for (int i = 0; i < num_digits - temp - 2; i++)
			{
				power *= 10;

			}
			k = ( (int) ( (number / power) % 10));
			k = perm[ (int) (temp % 12)][k];
			if (right < 0)
			{
				left = multipl[left][k];
			}
			else
			{
				right = multipl[right][k];
			}
		}
		int pruef_pos = num_digits - 1;
		right = 0;

		/****  Pr\uFFFDfziffer berechnen durch Umkehrung von    ****/
		/****  left * perm(pruef_pos+1) (x) * rechts = 0  ****/
		int i = multipl[inv_multipl[left]][inv_multipl[right]];
		int x = inv_perm[pruef_pos % 12][i];

		// Add Checksum to the end of the number
		number = number * 10 + x;
		return number;
	}

	/** Checks the Dieder checksum
	 * @return true, if checksum is valid, false otherwise
	 */
	public static boolean isDiederChecksumValid(long number)
	{
		long temp = number;
		int num_digits = 0;
		int k = 0, d = 0;
		long power = 0;

		// calculate number of digits
		while (temp > 0)
		{
			temp /= 10;
			num_digits++;
		}

		for (temp = 0; temp < num_digits; temp++)
		{
			power = 1;
			for (int i = 0; i < num_digits - temp - 1; i++)
			{
				power *= 10;
			}
			k = (int) ( (number / power) % 10l);
			k = perm[ (int) (temp % 12)][k];
			d = multipl[d][k];
		}
		return (d == 0);
	}

	public abstract void chargeAccount(long a_transferNumber, long a_amount);

	public abstract boolean isTanUsed(long a_tan);

	public abstract long getUsedDate(long a_tan);

	public abstract long getTransferAmount(long a_tan);

	public abstract void storePassivePayment(XMLPassivePayment a_passivePayment) throws Exception;

	public abstract Vector getPendingPassivePayments();

	public abstract XMLPassivePayment getPassivePaymentData(String transfernumber);

	public abstract void markPassivePaymentDone(long a_id);

	public abstract XMLErrorMessage buyFlatrate(long a_accountnumber);

	public abstract Hashtable getFlatrateConfig();

	public abstract XMLPaymentOptions getPaymentOptionsFromDb();

	public abstract XMLPaymentSettings getPaymentSettings();

	//used by PICommandMC

	public abstract void storePriceCert(XMLPriceCertificate a_priceCertificate) throws Exception;

	public abstract void deletePriceCert(XMLPriceCertificate a_priceCertificate) throws Exception;

	public abstract Vector getPriceCerts(String operatorCert);

	public abstract int getOperatorBalance(String operatorCert);

	public abstract String getOperatorInfo(String operatorCert);

	public abstract java.sql.Timestamp getLastBalanceUpdate(String operatorCert);

	public abstract String[] getBankAccount(String operatorCert);

	public abstract void storeBankAccount(XMLBankAccount theAccount);

	public abstract void handleTransferRequest(XMLTransferRequest theRequest);

	//utility functions dealing with price certs

	public abstract XMLPriceCertificate getPriceCertForHash(String hashValue);

	public abstract Vector getPriceCertsForMix(String subjectKeyIdentifier);

	public abstract JAPCertificate getOperatorOfMix(String subjectKeyIdentifier);

	//methods for handling paysafecard payments

	public abstract void storePaysafecardPayment(XMLPassivePayment a_passivePayment) throws Exception;

	public abstract void purgePaysafecardPayments();

	public abstract Vector getUsablePaysafecardPayments();

	public abstract void setPaysafecardPaymentUsed(XMLPassivePayment pp);

	//methods for handling coupons

	public abstract void redeemCoupon(String couponCode, long transferNumber) throws Exception;

	public abstract boolean checkValidity(String couponCode) throws Exception;

	//methods for getting volume plans

	public abstract XMLVolumePlans getVolumePlans();

	public abstract XMLVolumePlan getVolumePlan(String name);

	public abstract XMLErrorMessage buyVolumePlan(long accountNumber, XMLVolumePlan aPlan);

}
