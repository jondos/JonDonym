/*
 Copyright (c) 2000 - 2008, The JAP-Team
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

import java.util.StringTokenizer;
import java.util.Vector;

import org.bouncycastle.asn1.DERSequence;

import anon.util.Util;

/**
 * The key identifier is calculated using a SHA1 hash over the BIT STRING from
 * SubjectPublicKeyInfo as defined in RFC3280.
 * For DSA-PublicKeys the AlgorithmIdentifier of the SubjectPublicKeyInfo MUST 
 * contain the DSA-Parameters as specified in RFC 3279
 * @author Rolf Wendolsky, Robert Hirschberger
 */
public abstract class AbstractX509KeyIdentifier extends AbstractX509Extension 
{
	protected String m_value;
	
	/**
	 * Create a new KeyIdentifier from a Extension-Identifier an the 
	 * octets of the Extension.
	 * KeyIdentifier-Extensions (SubjectKeyIdentifier and AuthorityKeyIdentifier)
	 * are set to non-critical by default according to RFC 5280.
	 * @param a_identifier the extensions identifier String
	 * @param a_value the octets of the extenison
	 * @see http://tools.ietf.org/html/rfc5280
	 */
	public AbstractX509KeyIdentifier(String a_identifier, byte[] a_value)
	{
		super(a_identifier, false, a_value);
	}
	
	/**
	 * Create a new KeyIdentifier from a BouncyCastle DERSequence
	 * @param a_extension the extension as DERSequence
	 */
    public AbstractX509KeyIdentifier(DERSequence a_extension)
    {
    	super(a_extension);
    }
    
	/**
	 * Returns the key identifier as human-readable hex string of the form
	 * A4:54:21:52:F1:...
	 * @return the key identifier as human-readable hex string of the form
	 * A4:54:21:52:F1:...
	 */
	public String getValue()
	{
		return m_value;
	}

	/**
	 * Returns the key identifier as human-readable hex string without ":"
	 * separators.
	 * @return the key identifier as human-readable hex string without ":"
	 * separators
	 */
	public String getValueWithoutColon()
	{
		if (m_value == null)
		{
			return null;
		}
		StringTokenizer tokenizer = new StringTokenizer(m_value, ":");
		String value = "";

		while (tokenizer.hasMoreTokens())
		{
			value += tokenizer.nextToken();
		}
		return value;
	}

	/**
	 * Returns the key identifier as human-readable hex string.
	 * @return a Vector containing the key identifier as human-readable hex string
	 */
	public Vector getValues()
	{
		return Util.toVector(m_value);
	}
}
