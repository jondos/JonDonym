/*
 Copyright (c) 2000 - 2005, The JAP-Team
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

import java.util.Vector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.X509Extensions;

/**
 * The IssuerAlternativeName extension represents an alias to distinguished name (DN) of the
 * issuer.
 * It may contain several values and is often used for IPs, DNS-Names, URLs and E-Mail addresses.
 * @author Rolf Wendolsky
 */
public final class X509IssuerAlternativeName extends AbstractX509AlternativeName
{
	public static final String IDENTIFIER = X509Extensions.IssuerAlternativeName.getId();

	/**
	 * Constructs a new X509IssuerAlternativeName from a value.
	 * @param a_value a value
	 * @param a_tag the type tag for this value
	 */
	public X509IssuerAlternativeName(String a_value, Integer a_tag)
	{
		super(IDENTIFIER, a_value, a_tag);
	}

	/**
	 * Constructs a new X509IssuerAlternativeName from a value.
	 * @param a_critical true if the extension is critical; false otherwise
	 * @param a_value a value
	 * @param a_tag the type tag for this value
	 */
	public X509IssuerAlternativeName(boolean a_critical, String a_value, Integer a_tag)
	{
		super(IDENTIFIER, a_critical, a_value, a_tag);
	}

	/**
	 * Constructs a new X509IssuerAlternativeName from values.
	 * @param a_values values for the SubjectAlternativeName
	 * @param a_tags the type tags for the values
	 */
	public X509IssuerAlternativeName(Vector a_values, Vector a_tags)
	{
		super(IDENTIFIER, a_values, a_tags);
	}

	/**
	 * Constructs a new X509IssuerAlternativeName from values.
	 * @param a_critical true if the extension is critical; false otherwise
	 * @param a_values values for the SubjectAlternativeName
	 * @param a_tags the type tags for the values
	 */
	public X509IssuerAlternativeName(boolean a_critical, Vector a_values, Vector a_tags)
	{
		super(IDENTIFIER, a_critical, a_values, a_tags);
	}

	/**
	 * Creates an X509IssuerAlternativeName from a BouncyCastle DER sequence. For internal use only.
	 * @param a_extension a DERSequence
	 */
	public X509IssuerAlternativeName(DERSequence a_extension)
	{
		super(a_extension);
	}

	/**
	 * Returns "IssuerAlternativeName".
	 * @return "IssuerAlternativeName"
	 */
	public String getName()
	{
		return "IssuerAlternativeName";
	}
}
