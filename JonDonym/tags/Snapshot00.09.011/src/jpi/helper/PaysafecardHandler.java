/*
 Copyright (c) 2000-2006, The JAP-Team
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

package jpi.helper;

import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLPassivePayment;
import jpi.Configuration;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import jpi.db.DBSupplier;
import jpi.db.DBInterface;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.Enumeration;
import com.psc.merchantapi.MerchantAPIUtil;
import com.psc.merchantapi.MerchantAPIEnhanced;
import com.psc.merchantapi.PSCResult;
import com.psc.merchantapi.PSCDispositionStateResult;

/**
 * this class is the central point for handling all paysafecard-related functions.
 * Namely:
 * - create disposition upon receipt of a new payment from a JAP
 * - start a thread upon bi startup that periodically checks for realized dispositions
 *
 * @author Elmar Schraml
 */
public class PaysafecardHandler
{
	private static PaysafecardThread pscThread;

	/**
	 * createDisposition:
	 * contacts the Paysafecard server and creates a disposition that the user
	 * then has to confirm
	 *
	 * @param pp XMLPassivePayment: the XMl structure received
	 * at least currency, amount and either transaction_id or transfernumber have to be set
	 * @return boolean: true on success, false on failure
	 */
	public static boolean createDisposition(XMLPassivePayment pp)
	{
		String mid = pp.getPaymentData("merchant_id");
		if (mid == null)
		{
			mid = Configuration.getMerchantId();
		}
		String mtid = pp.getPaymentData("transaction_id");
		if (mtid == null)
		{
			mtid = mid + (new Long(pp.getTransferNumber())).toString();
		}
		String amount = (new Double(pp.getAmount())).toString();
		String currency = pp.getCurrency();
		String businessType = Configuration.getBusinessType();
		String reportingCriteria = "";
		String okUrl = MerchantAPIUtil.escapeString(Configuration.getOkUrl());
		String nokUrl = MerchantAPIUtil.escapeString(Configuration.getNokUrl());
		MerchantAPIEnhanced psc = new MerchantAPIEnhanced();
		PSCResult result = psc.create_disposition(mid,mtid,amount,currency,businessType,reportingCriteria,okUrl,nokUrl);
		return result.resultcode.equals("0");
	}

	/**
	 * getCustomerURL:
	 * returns the URL to which a user has to be redirected to confirm a payment,
	 * including the GET params from the specific payment passed as parameter,
	 * and pointing to the exact method/page for the confirmation
	 *
	 * @param pp XMLPassivePayment
	 * @return String: the URL as it has to be typed into the address bar of the browser
	 */
	public static String getCustomerURL(XMLPassivePayment pp)
	{
		StringBuffer completeUrl = new StringBuffer(PaysafecardHandler.getBaseURL());
		completeUrl.append("?mid="+pp.getPaymentData("merchant_id"));
		completeUrl.append("&mtid="+pp.getPaymentData("transaction_id"));
		DecimalFormat amountFormat = new DecimalFormat("#0.00");
		completeUrl.append("&amount="+amountFormat.format(pp.getAmount()));
		completeUrl.append("&currency="+pp.getCurrency());
		return completeUrl.toString();
	}

	/**
	 * getBaseURL:
	 * returns the pure URL to which a customer has to be redirected,
	 * without the payment-specific parameters,
	 * but including the specific page or method to call
	 * example: https://test.at.paysafecard.com/pscmerchant"
	 *
	 * @return String
	 */
	public static String getBaseURL()
	{
		return Configuration.getConfirmUrl();
	}

	public static void startPaysafecardThread()
	{
		pscThread = new PaysafecardThread();
		pscThread.start();
	}

}

/**
 * responsible for handling created paysafecard dispositions
 * Responsibilities:
 * - purge expired payments
 * - charge payments that have been confirmed by the user to the BI's account
 *
 * @author Elmar Schraml
 */
class PaysafecardThread extends Thread
{
	/* Configuration ia a better place to set defaults
	public static final long DISPOSITION_CHECK_INTERVAL = 600000; //ten minutes
	*/

	private static final String FLAG_CLOSE = "1";
	private static final String FLAG_KEEP_OPEN = "0";

	public void run()
	{
		try
		{
			DBInterface db = DBSupplier.getDataBase();
			//purge useless dispositions (invalid, or created and timed out)
			db.purgePaysafecardPayments();
			//get dispositions that can be debited
			Vector disposedPayments = db.getUsablePaysafecardPayments();
			MerchantAPIEnhanced psc = new MerchantAPIEnhanced();
			//and do the transfer
			for (Enumeration payments = disposedPayments.elements();payments.hasMoreElements();)
			{
				XMLPassivePayment curPayment = (XMLPassivePayment) payments.nextElement();
				//check disposition state with psc server
				String mid = curPayment.getPaymentData("merchant_id");
				String mtid = curPayment.getPaymentData("transaction_id");
				PSCDispositionStateResult stateResult = psc.get_disposition_state(mid,mtid);
				String state = stateResult.state;
				if (! state.equalsIgnoreCase("D") ) //"disposed", i.e. usable for debiting
				{
					continue;
				}
				//if OK, execute debit
				DecimalFormat amountFormat = new DecimalFormat("#0.00");
				double doubleAmount = curPayment.getAmount();
				String amount = amountFormat.format(doubleAmount);
				String currency = curPayment.getCurrency();
				PSCResult result = psc.execute_debit(mid,mtid,amount,currency,FLAG_CLOSE);
				if (! result.resultcode.equalsIgnoreCase("0") )
				{
					LogHolder.log(LogLevel.ERR, LogType.PAY, "error debitin psc disposition, code: "+result.errorcode+", message: "+result.errormessage);;

				}
				//and set state to consumed in db
				db.setPaysafecardPaymentUsed(curPayment);

				//and credit the user account
				long centAmount = Math.round(doubleAmount*100);
				long transfernumber = curPayment.getTransferNumber();
				db.chargeAccount(transfernumber,centAmount);
			}
			//all done, wait a while before doing it all again
			int cleanupInterval = Configuration.getCleanupInterval().intValue();
			long sleepForMillis = cleanupInterval * 60 *1000;
			sleep(sleepForMillis);
		}
		catch (InterruptedException ex)
		{
			//not tragic, but log it to let the user know
			LogHolder.log(LogLevel.ALERT, LogType.PAY, "Paysafecard thread has been interrupted");
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "paysafecard thread reports an error");
		}
	}
}
