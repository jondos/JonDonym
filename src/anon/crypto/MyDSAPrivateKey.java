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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.Base64;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

final public class MyDSAPrivateKey extends AbstractPrivateKey implements DSAPrivateKey, IMyPrivateKey
{

	public static final String XML_ELEMENT_NAME = "DSAPrivateKey";

	private BigInteger m_X;
	private MyDSAParams m_params;

	public MyDSAPrivateKey(PrivateKeyInfo privKeyInfo) throws InvalidKeyException
	{
		super(privKeyInfo);
		try
		{
			AlgorithmIdentifier algId = privKeyInfo.getAlgorithmId();
			DERInteger X = (DERInteger) privKeyInfo.getPrivateKey();
			m_X = X.getValue();
			m_params = new MyDSAParams(new DSAParameter( (ASN1Sequence) algId.getParameters()));
		}

		catch (Exception e)
		{
			throw new InvalidKeyException("IOException while decoding private key");
		}
	}

	public MyDSAPrivateKey(Element a_xmlElement) throws InvalidKeyException, XMLParseException
	{
		if (a_xmlElement == null || !a_xmlElement.getNodeName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException(XML_ELEMENT_NAME, "Element is null or has wrong name!");
		}

		Element elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "G");
		String str = XMLUtil.parseValue(elem, (String)null);
		BigInteger g = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "P");
		str = XMLUtil.parseValue(elem, (String)null);
		BigInteger p = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "Q");
		str = XMLUtil.parseValue(elem, (String)null);
		BigInteger q = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "X");
		str = XMLUtil.parseValue(elem, (String)null);
		m_X = new BigInteger(Base64.decode(str));
		m_params = new MyDSAParams(
			  new DSAPrivateKeyParameters(m_X, new DSAParameters(p, q, g)).getParameters());
	}

	public MyDSAPrivateKey(DSAPrivateKeyParameters keyParams)
	{
		m_X = keyParams.getX();
		m_params = new MyDSAParams(keyParams.getParameters());
	}

	/**
	 * Creates the corresponding public key to this private key.
	 * @return the corresponding public key to this private key
	 */
	public IMyPublicKey createPublicKey()
	{
		MyDSAPublicKey key;
		BigInteger Y;

		Y = getParams().getG().modPow(getX(), getParams().getP());
		key = new MyDSAPublicKey(new DSAPublicKeyParameters(Y, (DSAParameters) m_params));

		return key;
	}

	public String getAlgorithm()
	{
		return "DSA";
	}

	public String getFormat()
	{
		return "PKCS#8";
	}

	public BigInteger getX()
	{
		return m_X;
	}

	public PrivateKeyInfo getAsPrivateKeyInfo()
	{
		PrivateKeyInfo info;

		DERObject derParam =
			new DSAParameter(
			m_params.getP(),
			m_params.getQ(),
			m_params.getG())
			.getDERObject();

		info = new PrivateKeyInfo(
			new AlgorithmIdentifier(
			X9ObjectIdentifiers.id_dsa,
			derParam),
			new DERInteger(getX()));

		return info;
	}

	/**
	 * Gets a signature algorithm object for this key.
	 * @return a signature algorithm object for this key
	 */
	public ISignatureCreationAlgorithm getSignatureAlgorithm()
	{
		try
		{
			MyDSASignature algorithm = new MyDSASignature(); ;
			algorithm.initSign(this);
			return algorithm;
		}
		catch (InvalidKeyException a_e)
		{
			// not possible
		}
		return null;
	}

	public DSAParams getParams()
	{
		return m_params;
	}

	public MyDSAParams getMyDSAParams()
	{
		return m_params;
	}

	public 	DSAPrivateKeyParameters getPrivateParams()
	{
		return new DSAPrivateKeyParameters(m_X,m_params);
	}


	/**
	 * getXmlEncoded
	 * @param a_doc an XML document
	 * @return Document
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemPrivKey = a_doc.createElement(XML_ELEMENT_NAME);
		Element elem = a_doc.createElement("G");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_params.getG().toByteArray()));
		elem = a_doc.createElement("P");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_params.getP().toByteArray()));
		elem = a_doc.createElement("Q");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_params.getQ().toByteArray()));
		elem = a_doc.createElement("X");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_X.toByteArray()));
		return elemPrivKey;
	}
}
