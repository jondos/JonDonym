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
import java.util.StringTokenizer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROutputStream;

import anon.util.Util;

/**
 * The subject public key identifier is calculated using a SHA1 hash over the BIT STRING from
 * SubjectPublicKeyInfo as defined in RFC3280.
 * For DSA-PublicKeys the AlgorithmIdentifier of the SubjectPublicKeyInfo MUST contain the DSA-Parameters as specified in RFC 3279
 * @author Rolf Wendolsky
 */
public final class X509SubjectKeyIdentifier extends AbstractX509Extension
{
	public static final String IDENTIFIER = X509Extensions.SubjectKeyIdentifier.getId();

	private String m_value;

	/**
	 * Creates a new X509SubjectKeyIdentifier from a public key.
	 * @param a_publicKey a public key
	 */
	public X509SubjectKeyIdentifier(IMyPublicKey a_publicKey)
	{
		super(IDENTIFIER, false, createDEROctets(a_publicKey));
		createValue();
	}

	/**
	 * Creates an extension from a BouncyCastle DER sequence. For internal use only.
	 * @param a_extension a DERSequence
	 */
	public X509SubjectKeyIdentifier(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}

	/**
	 * Returns "SubjectKeyIdentifier".
	 * @return "SubjectKeyIdentifier"
	 */
	public String getName()
	{
		return "SubjectKeyIdentifier";
	}

	/**
	 * Returns the subject public key identifier as human-readable hex string of the form
	 * A4:54:21:52:F1:...
	 * @return the subject public key identifier as human-readable hex string of the form
	 * A4:54:21:52:F1:...
	 */
	public String getValue()
	{
		return m_value;
	}

	/**
	 * Returns the subject public key identifier as human-readable hex string without ":"
	 * separators.
	 * @return the subject public key identifier as human-readable hex string without ":"
	 * separators
	 */
	public String getValueWithoutColon()
	{
		StringTokenizer tokenizer = new StringTokenizer(m_value, ":");
		String value = "";

		while (tokenizer.hasMoreTokens())
		{
			value += tokenizer.nextToken();
		}
		return value;
	}

	/**
	 * Returns the subject public key identifier as human-readable hex string.
	 * @return a Vector containing the subject public key identifier as human-readable hex string
	 */
	public Vector getValues()
	{
		return Util.toVector(m_value);
	}

	private static byte[] createDEROctets(IMyPublicKey a_publicKey)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try
		{
			new DEROutputStream(out).writeObject(

				( (DEROctetString)
				 new SubjectKeyIdentifier(
				a_publicKey.getAsSubjectPublicKeyInfo()).getDERObject()));
		}
		catch (Exception a_e)
		{
			// should never happen
			throw new RuntimeException("Could not write DER object to bytes!");
		}

	   return out.toByteArray();
	}

	private void createValue()
	{
		byte[] identifier;

		try
		{
			 identifier = ((DEROctetString)new ASN1InputStream(
						 new ByteArrayInputStream(getDEROctets())).readObject()).getOctets();
		}
		catch (Exception a_e)
		{
			// this should never happen
			throw new RuntimeException("Could not read subject key identifier from byte array!");
		}

		m_value = ByteSignature.toHexString(identifier);
	}

}
