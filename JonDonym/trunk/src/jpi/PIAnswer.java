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
package jpi;

import anon.util.IXMLEncodable;
import anon.pay.xml.XMLErrorMessage;
import org.w3c.dom.Element;

/**
 * Datencontainer f\uFFFDr eine Bezahlinstanz-Http-Antwort.
 *
 * @author Andreas Mueller
 */
public class PIAnswer
{
	private int m_iStatusCode;
	private IXMLEncodable m_Content;
	private int m_iType;
	public final static int TYPE_ERROR = -1;
	public final static int TYPE_CLOSE = 1;
	public final static int TYPE_CHALLENGE_REQUEST = 2;

	//Bi --> JAP
	public final static int TYPE_ACCOUNT_CERTIFICATE = 3;
	public final static int TYPE_AUTHENTICATION_SUCCESS = 4;
	public final static int TYPE_TRANSFER_CERTIFICATE = 5;
	public final static int TYPE_BALANCE = 6;
	public final static int TYPE_PAYMENT_OPTIONS = 11;
	public final static int TYPE_VOLUME_PLANS = 22;
	public final static int TYPE_PAYMENT_SETTINGS = 21;
	public final static int TYPE_TRANSACTION_OVERVIEW = 12;
	public final static int TYPE_PASSIVE_PAYMENT = 13;
	public final static int TYPE_CAPTCHA_REQUEST = 14;
	public final static int TYPE_FLATRATE = 20;

	//Bi --> AI
	public final static int TYPE_PAYOFF = 7;
	public final static int TYPE_SETTLE = 8;
	public final static int TYPE_CONFIRM = 9;
	public final static int TYPE_ACCOUNT_SNAPSHOT = 10;

	//BI --> MixConfig
	public final static int TYPE_OPERATORBALANCE = 15;
	public final static int TYPE_BANKACCOUNT = 16;
	public final static int TYPE_MIXINFO = 17;
	public final static int TYPE_PRICECERT = 18;
	public final static int TYPE_PRICECERTS = 19;


	public PIAnswer(int type, IXMLEncodable content)
	{
		m_Content = content;
		m_iType = type;
	}

	public static PIAnswer getErrorAnswer(int errorCode)
	{
		IXMLEncodable err = new XMLErrorMessage(errorCode);
		return new PIAnswer(TYPE_CLOSE, err);
	}

	public static PIAnswer getErrorAnswer(int errorCode, String msg)
	{
		IXMLEncodable err = new XMLErrorMessage(errorCode, msg);
		return new PIAnswer(TYPE_CLOSE, err);
	}

	public int getType()
	{
		return m_iType;
	}

	public IXMLEncodable getContent()
	{
		return m_Content;
	}
}
