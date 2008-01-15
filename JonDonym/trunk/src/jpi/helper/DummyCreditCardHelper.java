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
package jpi.helper;

import java.util.Vector;
import jpi.ICreditCardListener;
import java.util.Enumeration;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import jpi.db.DBInterface;
import jpi.db.DBSupplier;
import jpi.Configuration;

/**
 * This class is processes dummy credit card payments
 *
 * @author Tobias Bayer
 */
public class DummyCreditCardHelper implements ICreditCardHelper
{
	private static DummyCreditCardHelper ms_instance;
	private Vector m_listeners;

	/** This string must be included (non-case-sensitive) in the payment option's
	 *  name to be supported by this helper class.
	 */
	private static final String CREDITCARD = "creditcard";

	private DummyCreditCardHelper()
	{
		m_listeners = new Vector();
	}

	public static DummyCreditCardHelper getInstance()
	{
		if (ms_instance == null)
		{
			ms_instance = new DummyCreditCardHelper();
		}
		return ms_instance;

	}

	public boolean chargePending()
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching pending credit card payments from database");
			try
			{
				DBInterface db = DBSupplier.getDataBase();
				Vector payments = db.getPendingPassivePayments();
				for (int i = 0; i < payments.size(); i++)
				{
					String[] payment = (String[]) payments.elementAt(i);
				if (payment[1].toLowerCase().indexOf(CREDITCARD) != -1)
					{
						long cents = Long.parseLong(payment[3])*100;
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  "Charging account with transfernumber: " + payment[2]
								  + " Amount: " + cents);

					db.chargeAccount(Long.parseLong(payment[2]), cents);
						db.markPassivePaymentDone(Long.parseLong(payment[0]));
					}
				}
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Error while processing pending passive payments!");
			return false;
		}
		return true;

			}

	public void run()
	{
		while (true)
		{
			chargePending();
			try
			{
				Thread.sleep(30000);
			}
			catch (Exception e)
			{}
		}
	}

	public void addCreditCardListener(ICreditCardListener a_listener)
	{
		if (!m_listeners.contains(a_listener))
		{
			m_listeners.addElement(a_listener);
		}

	}

	public void fireConfirmedPayment()
	{
		Enumeration e = m_listeners.elements();
		while (e.hasMoreElements())
		{
			( (ICreditCardListener) e.nextElement()).paymentConfirmed();
		}
	}
}
