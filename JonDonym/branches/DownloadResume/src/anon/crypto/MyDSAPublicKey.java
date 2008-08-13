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

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.Base64;
import anon.util.XMLUtil;

final public class MyDSAPublicKey extends AbstractPublicKey implements DSAPublicKey, IMyPublicKey
{
	private BigInteger m_Y;
	private MyDSAParams m_params;
	private long m_hashValue = 0;

	public MyDSAPublicKey(DSAPublicKeyParameters params)
	{
		m_Y = params.getY();
		m_params = new MyDSAParams(params.getParameters());
	}

	public MyDSAPublicKey(SubjectPublicKeyInfo info) throws IllegalArgumentException
	{
		try
		{

			DSAParameter params = new DSAParameter( (ASN1Sequence) info.getAlgorithmId().getParameters());
			DERInteger derY = null;
			derY = (DERInteger) info.getPublicKey();
			m_Y = derY.getValue();
			m_params = new MyDSAParams(params);
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("invalid info structure in DSA public key");
		}

	}

	/**
	 * Gets a signature algorithm object for this key.
	 * @return a signature algorithm object for this key
	 */
	public ISignatureVerificationAlgorithm getSignatureAlgorithm()
	{
		try
		{
			MyDSASignature algorithm = new MyDSASignature();
			algorithm.initVerify(this);
			return algorithm;
		}
		catch (InvalidKeyException a_e)
		{
			// not possible
		}
		return null;
	}

	public BigInteger getY()
	{
		return m_Y;
	}

	public DSAParams getParams()
	{
		return m_params;
	}

	public DSAPublicKeyParameters getPublicParams()
	{
		return new DSAPublicKeyParameters(m_Y,m_params);
	}

	public MyDSAParams getMyDASParams()
	{
		return m_params;
	}

	public String getAlgorithm()
	{
		return "DSA";
	}

	public String getFormat()
	{
		return "X.509";
	}

	public SubjectPublicKeyInfo getAsSubjectPublicKeyInfo()
	{
		DERObject derParam = new DSAParameter(m_params.getP(),
											  m_params.getQ(),
											  m_params.getG()).getDERObject();
		AlgorithmIdentifier algID = new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa,
			derParam);
		return new SubjectPublicKeyInfo(algID, new DERInteger(getY()));
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("DSAKeyValue");
		Element elem = null;

		elem = a_doc.createElement("Y");
		XMLUtil.setValue(elem, Base64.encodeBytes(m_Y.toByteArray()));
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("P");
		XMLUtil.setValue(elem, Base64.encodeBytes(m_params.getP().toByteArray()));
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("Q");
		XMLUtil.setValue(elem, Base64.encodeBytes(m_params.getQ().toByteArray()));
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("G");
		XMLUtil.setValue(elem, Base64.encodeBytes(m_params.getG().toByteArray()));
		elemRoot.appendChild(elem);

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
		if (! (a_publicKey instanceof DSAPublicKey))
		{
			return false;
		}
		DSAPublicKey d = (DSAPublicKey) a_publicKey;
		return (d.getY().equals(m_Y) && d.getParams().equals(m_params));
	}

	/**
	 * @return the public key`s hash code
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		if (m_hashValue == 0)
		{
			m_hashValue = (m_Y.longValue() + m_params.getG().longValue() +
						   m_params.getP().longValue() + m_params.getQ().longValue());
		}

		return (int) m_hashValue;
	}

	public int getKeyLength()
	{
		return m_params.getP().bitCount();
	}

}
