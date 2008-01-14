/*
 Copyright (c) 2000, The JAP-Team
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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PublicKey;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.Base64;
import anon.util.XMLUtil;

final public class MyRSAPublicKey extends AbstractPublicKey implements IMyPublicKey
{
	private MyRSASignature m_algorithm = new MyRSASignature();
	private BigInteger m_n;
	private BigInteger m_e;
	private long m_hashValue = 0;
	private int m_keyLength = 0;

	public MyRSAPublicKey(BigInteger modulus, BigInteger exponent)
	{
		m_n = modulus;
		m_e = exponent;
	}

	public MyRSAPublicKey(CipherParameters cipherparams) throws Exception
	{
		RSAKeyParameters p = (RSAKeyParameters) cipherparams;
		m_n = p.getModulus();
		m_e = p.getExponent();
	}

	public MyRSAPublicKey(RSAPublicKeyStructure en) throws IllegalArgumentException
	{
		try
		{
			m_n = en.getModulus();
			m_e = en.getPublicExponent();
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("invalid info structure in RSA public key");
		}

	}

	public MyRSAPublicKey(SubjectPublicKeyInfo info) throws IllegalArgumentException
	{
		try
		{
			RSAPublicKeyStructure en = RSAPublicKeyStructure.getInstance(info.getPublicKey());
			m_n = en.getModulus();
			m_e = en.getPublicExponent();
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("invalid info structure in RSA public key");
		}

	}

	public static MyRSAPublicKey getInstance(byte[] encoded)
	{
		try
		{
			return new MyRSAPublicKey(
						 new RSAPublicKeyStructure( (
											   ASN1Sequence)new ASN1InputStream(
				new ByteArrayInputStream(encoded)).readObject())
				);
		}
		catch (Throwable t)
		{
			return null;
		}
	}

	/**
	 * Gets the signature algorithm object that is held and initialised by this key.
	 * It is ready to verify messages and must not be reinitialised.
	 * @return the signature algorithm object that is held and initialised by this key
	 */
	public ISignatureVerificationAlgorithm getSignatureAlgorithm()
	{
		try
		{
		m_algorithm.initVerify(this);

		}
		catch (InvalidKeyException a_e)
		{
			// not possible
		}
		return m_algorithm;
	}

	public BigInteger getModulus()
	{
		return m_n;
	}

	public BigInteger getPublicExponent()
	{
		return m_e;
	}

	public String getAlgorithm()
	{
		return "RSA";
	}

	public String getFormat()
	{
		return "X.509";
	}

	public int getKeyLength()
	{
		return getModulus().bitLength();
	}
	public SubjectPublicKeyInfo getAsSubjectPublicKeyInfo()
	{
		AlgorithmIdentifier algID = new AlgorithmIdentifier(new DERObjectIdentifier("1.2.840.113549.1.1.1")); //RSA
		return new SubjectPublicKeyInfo(algID, new RSAPublicKeyStructure(m_n, m_e).getDERObject());
	}

	public CipherParameters getParams()
	{
		return new RSAKeyParameters(false, m_n, m_e);
	}

	/**
	 * Builds an XML Node containing the public key data.
	 * This is compliant to the W3C XML Signature standard
	 * @param a_doc the root document
	 * @return an XML Node
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("RSAKeyValue");
		Element elemModulus = a_doc.createElement("Modulus");
		elemRoot.appendChild(elemModulus);
		byte[] b = m_n.toByteArray();
		XMLUtil.setValue(elemModulus, Base64.encodeBytes(b));
		Element elemExponent = a_doc.createElement("Exponent");
		elemRoot.appendChild(elemExponent);
		b = m_e.toByteArray();
		XMLUtil.setValue(elemExponent, Base64.encodeBytes(b));
		return elemRoot;
	}

	/**
	 * This method returns if two public keys have the same public key parameters.
	 * @param a_publicKey an other public key
	 * @return true if the keys have the same public key parameters; false otherwise
	 */
	public boolean equals(Object a_publicKey)
	{
		if (a_publicKey == null)
		{
			return false;
		}
		if (! (a_publicKey instanceof PublicKey))
		{
			return false;
		}
		if (! (a_publicKey instanceof MyRSAPublicKey))
		{
			return false;
		}
		MyRSAPublicKey r = (MyRSAPublicKey) a_publicKey;
		return r.getModulus().equals(m_n) && r.getPublicExponent().equals(m_e);
	}

	/**
	 * @return the public key`s hash code
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		if (m_hashValue == 0)
		{
			m_hashValue = m_n.longValue() + m_e.longValue();
		}

		return (int) m_hashValue;
	}

}
