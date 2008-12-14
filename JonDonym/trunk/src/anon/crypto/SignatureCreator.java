/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package anon.crypto;

import java.util.Hashtable;

import org.w3c.dom.Node;

public class SignatureCreator
{

	/**
	 * Stores the instance of SignatureCreator (Singleton).
	 */
	private static SignatureCreator ms_scInstance;

	private Hashtable m_signatureKeys;

	/**
	 * Returns the instance of SignatureCreator (Singleton). If there is no instance, there is a
	 * new one created.
	 *
	 * @return The SignatureCreator instance.
	 */
	public static SignatureCreator getInstance()
	{
		synchronized (SignatureCreator.class)
		{
			if (ms_scInstance == null)
			{
				ms_scInstance = new SignatureCreator();
			}
		}
		return ms_scInstance;
	}

	/**
	 * Creates a new instance of SignatureVerifier.
	 */
	private SignatureCreator()
	{
		m_signatureKeys = new Hashtable();
	}

	public void setSigningKey(int a_purpose, PKCS12 a_signatureKey)
	{
		synchronized (m_signatureKeys)
		{
			m_signatureKeys.put(new Integer(a_purpose), a_signatureKey);
		}
	}

	public XMLSignature getSignedXml(int a_documentClass, Node a_nodeToSign)
	{
		PKCS12 signatureKey = null;
		XMLSignature createdSignature = null;

		synchronized (m_signatureKeys)
		{
			signatureKey = (PKCS12) (m_signatureKeys.get(new Integer(a_documentClass)));
		}

		if (signatureKey != null)
		{
			try
			{
				createdSignature = XMLSignature.sign(a_nodeToSign, signatureKey);
				if (createdSignature != null && createdSignature.getCertPath() != null)
				{
					createdSignature.getCertPath().setDocType(a_documentClass);
				}
			}
			catch (Exception e)
			{
			}
		}
		return createdSignature;
	}

	public boolean signXml(int a_documentClass, Node a_nodeToSign)
	{
		return getSignedXml(a_documentClass, a_nodeToSign) != null;
	}
}
