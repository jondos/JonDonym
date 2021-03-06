/*
 Copyright (c) 2006, The JAP-Team
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
package anon.util;

/**
 * @author Rolf Wendolsky
 */
public class SingleStringPasswordReader implements IMiscPasswordReader
{
	private char[] m_password = null;

	public SingleStringPasswordReader(String a_password)
	{
		if (a_password == null)
		{
			m_password = null;
			return;
		}
		m_password = a_password.toCharArray();
	}

	public SingleStringPasswordReader(char[] a_password)
	{
		if (a_password == null)
		{
			m_password = null;
			return;
		}
		m_password = new char[a_password.length];
		System.arraycopy(a_password, 0, m_password, 0, a_password.length);
	}


	public String readPassword(Object a_message)
	{
		if (m_password == null)
		{
			return null;
		}
		String password = new String(m_password);
		clear();
		return password;
	}

	public void clear()
	{
		if (m_password != null)
		{
			for (int i = 0; i < m_password.length; i++)
			{
				m_password[i] = 0;
			}
			m_password = null;
		}
	}
}
