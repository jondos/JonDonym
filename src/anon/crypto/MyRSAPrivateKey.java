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

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.Base64;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

final public class MyRSAPrivateKey extends AbstractPrivateKey implements IMyPrivateKey
{
	public static final String XML_ELEMENT_NAME = "RSAPrivateKey";
	private MyRSASignature m_algorithm = new MyRSASignature();
	private RSAPrivateCrtKeyParameters m_Params;

	public MyRSAPrivateKey(CipherParameters cipherparams) throws Exception
	{
		m_Params = (RSAPrivateCrtKeyParameters) cipherparams;
	}

	public MyRSAPrivateKey(PrivateKeyInfo privKeyInfo) throws Exception
	{
		super(privKeyInfo);
		DERObject d = privKeyInfo.getPrivateKey();
		RSAPrivateKeyStructure gh = new RSAPrivateKeyStructure( (ASN1Sequence) d);
		m_Params = new RSAPrivateCrtKeyParameters(gh.getModulus(), gh.getPublicExponent(),
												  gh.getPrivateExponent(),
												  gh.getPrime1(), gh.getPrime2(), gh.getExponent1(),
												  gh.getExponent2(),
												  gh.getCoefficient());

	}

		public MyRSAPrivateKey(Element a_xmlElement) throws Exception
		{
			if (a_xmlElement == null || !a_xmlElement.getNodeName().equals(XML_ELEMENT_NAME))
			{
				throw new XMLParseException(XML_ELEMENT_NAME, "Element is null or has wrong name!");
			}

			Element elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "Modulus");
			String str = XMLUtil.parseValue(elem, (String)null);
			BigInteger modulus = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "PublicExponent");
			str = XMLUtil.parseValue(elem, (String)null);
			BigInteger publicExponent = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "PrivateExponent");
			str = XMLUtil.parseValue(elem, (String)null);
			BigInteger privateExponent = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "P");
			str = XMLUtil.parseValue(elem, (String)null);
			BigInteger p = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "Q");
			str = XMLUtil.parseValue(elem, (String)null);
			BigInteger q = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "dP");
			str = XMLUtil.parseValue(elem, (String)null);
			BigInteger dP = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "dQ");
			str = XMLUtil.parseValue(elem, (String)null);
			BigInteger dQ = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(a_xmlElement, "QInv");
			str = XMLUtil.parseValue(elem, (String)null);
			BigInteger qInv = new BigInteger(Base64.decode(str));

			m_Params = new RSAPrivateCrtKeyParameters(
				 modulus, publicExponent, privateExponent, p, q, dP, dQ, qInv);
		}

	public MyRSAPrivateKey(
		BigInteger modulus,
		BigInteger publicExponent,
		BigInteger privateExponent,
		BigInteger p,
		BigInteger q,
		BigInteger dP,
		BigInteger dQ,
		BigInteger qInv) throws Exception
	{
		m_Params = new RSAPrivateCrtKeyParameters(modulus, publicExponent, privateExponent,
												  p, q, dP, dQ, qInv);

	}

	/**
	 * Gets a signature algorithm object for this key.
	 * @return a signature algorithm object for this key
	 */
	public ISignatureCreationAlgorithm getSignatureAlgorithm()
	{
		try
		{
			m_algorithm.initSign(this);
		}
		catch (InvalidKeyException a_e)
		{
			// not possible
		}
		return m_algorithm;
	}

	/**
	 * Creates the corresponding public key to this private key.
	 * @return the corresponding public key to this private key
	 */
	public IMyPublicKey createPublicKey()
	{
		return new MyRSAPublicKey(this.getModulus(), this.getPublicExponent());
	}

	public CipherParameters getParams()
	{
		return m_Params;
	}

	public BigInteger getModulus()
	{
		return m_Params.getModulus();
	}

	public BigInteger getPrivateExponent()
	{
		return m_Params.getExponent();
	}

	public BigInteger getP()
	{
		return m_Params.getP();
	}

	public BigInteger getQ()
	{
		return m_Params.getQ();
	}

	public BigInteger getDP()
	{
		return m_Params.getDP();
	}

	public BigInteger getDQ()
	{
		return m_Params.getDQ();
	}

	public BigInteger getQInv()
	{
		return m_Params.getQInv();
	}

	public BigInteger getPublicExponent()
	{
		return m_Params.getPublicExponent();
	}

	public String getAlgorithm()
	{
		return "RSA";
	}

	public String getFormat()
	{
		return "PKCS#8";
	}

	public PrivateKeyInfo getAsPrivateKeyInfo()
	{
		PrivateKeyInfo privKey =
			new PrivateKeyInfo(
			new AlgorithmIdentifier(
			new DERObjectIdentifier("1.2.840.113549.1.1.1")
			),
			new RSAPrivateKeyStructure(m_Params.getModulus(), m_Params.getPublicExponent(),
									   m_Params.getExponent(), m_Params.getP(), m_Params.getQ(),
									   m_Params.getDP(), m_Params.getDQ(), m_Params.getQInv()).
			getDERObject());

		return privKey;
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemPrivKey = a_doc.createElement(XML_ELEMENT_NAME);
		Element elem = a_doc.createElement("Modulus");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_Params.getModulus().toByteArray()));
		elem = a_doc.createElement("PublicExponent");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_Params.getPublicExponent().toByteArray()));
		elem = a_doc.createElement("PrivateExponent");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_Params.getExponent().toByteArray()));
		elem = a_doc.createElement("P");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_Params.getP().toByteArray()));
		elem = a_doc.createElement("Q");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_Params.getQ().toByteArray()));
		elem = a_doc.createElement("dP");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_Params.getDP().toByteArray()));
		elem = a_doc.createElement("dQ");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_Params.getDQ().toByteArray()));
		elem = a_doc.createElement("QInv");
		elemPrivKey.appendChild(elem);
		XMLUtil.setValue(elem, Base64.encodeBytes(m_Params.getQInv().toByteArray()));

		return elemPrivKey;
	}
}
