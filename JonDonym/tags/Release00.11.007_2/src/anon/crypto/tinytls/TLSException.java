/*
 Copyright (c) 2004, The JAP-Team
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
/*
 * Created on Mar 29, 2004
 *
 */
package anon.crypto.tinytls;

import java.io.IOException;

/**
 *
 * @author stefan
 *
 *TLSException
 */
public class TLSException extends IOException
{
	public static final int LEVEL_WARNING = 1;
	public static final int LEVEL_FATAL = 2;
	
	public static final String MSG_EOF = "EOF";
	
	public static final int DESC_CLOSE_NOTIFY = 0;
	
	private byte m_AlertLevel;
	private byte m_AlertDescription;
	private boolean m_Alert;

	/**
	 * Constructor
	 * @param s message
	 */
	public TLSException(String s)
	{
		super(s);
		m_Alert = false;
		m_AlertLevel = 0;
		m_AlertDescription = 0;
	}

	/**
	 *  Constructor
	 * @param s message
	 * @param level errorlevel (1:warning, 2:fatal)
	 * @param description  description (see RFC2246 -> 7.2 Alert protocol)
	 */
	public TLSException(String s, int level, int description)
	{
		super(s);
		m_Alert = true;
		m_AlertLevel = (byte) (level & 0xFF);
		m_AlertDescription = (byte) (description & 0xFF);
	}

	/**
	 * check if this Exception is an alert
	 * @return
	 */
	public boolean Alert()
	{
		return m_Alert;
	}

	/**
	 * if the Exception is an alert an alertlevel is returned
	 * @return
	 * alertlevel
	 */
	public byte getAlertLevel()
	{
		return m_AlertLevel;
	}

	/**
	 * if the Exception is an alert an alertdescription is returned
	 * @return
	 * alertdescription
	 */
	public byte getAlertDescription()
	{
		return m_AlertDescription;
	}

}
