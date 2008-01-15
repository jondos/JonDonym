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

import java.io.IOException;
import java.security.SecureRandom;

import org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.CTSBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.util.Base64;
import anon.util.IMiscPasswordReader;
import anon.util.SingleStringPasswordReader;
import anon.util.XMLUtil;

final public class XMLEncryption
{
	public static final String XML_ELEMENT_NAME = "EncryptedData";

	private static final int SALT_SIZE = 20;
	private static final int MIN_ITERATIONS = 1000;
	private XMLEncryption()
	{
	}

	/**
	 * Encrypts an element and all its children using PKCS#12 and the given password.
	 * The encrypted Element replaces the plaintext Element inside its
	 * OwnerDocument.
	 *
	 * Note: The document Element cannot be encrypted using this function.
	 *
	 * For information on the XML encryption standard
	 * see also http://www.w3.org/TR/xmlenc-core/
	 *
	 * @author Bastian Voigt
	 * @param elemPlain Element to be encrypted (not the DocumentElement!)
	 * @param password String a password
	 * @return Element a pointer to the encrypted element
	 */
	public static Element encryptElement(Element elemPlain, String password) throws Exception
	{
		// generate random salt
		SecureRandom random = new SecureRandom();
		byte[] kSalt = new byte[SALT_SIZE];
		random.nextBytes(kSalt);

		// encrypt data
		byte[] barInput = null;
		byte[] barOutput = null;
		int len = 0;
		try
		{
			barInput = XMLUtil.toString(elemPlain).getBytes();
			len = barInput.length;
			barOutput = codeDataCTS(true, barInput, generatePBEKey(password, kSalt));
		}
		catch (Exception ex1)
		{
			throw new IOException("Exception while encrypting: " + ex1.toString());
		}

		// initialize XML-Encryption-Standard structure
		Document doc = elemPlain.getOwnerDocument();
		Node nodeParent = elemPlain.getParentNode();
		Element elemCrypt = doc.createElement(XML_ELEMENT_NAME);
		elemCrypt.setAttribute("Type", "http://www.w3.org/2001/04/xmlenc#Element");
		elemCrypt.setAttribute("xmlns", "http://www.w3.org/2001/04/xmlenc#");
		Element elemAlgo = doc.createElement("EncryptionMethod");
		elemAlgo.setAttribute("Algorithm", "aes-cts");
		elemCrypt.appendChild(elemAlgo);
		Element elemKeyInfo = doc.createElement("ds:KeyInfo");
		elemKeyInfo.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
		Element elemSalt = doc.createElement("ds:Salt");
		XMLUtil.setValue(elemSalt, Base64.encodeBytes(kSalt));
		elemKeyInfo.appendChild(elemSalt);
		//Element elemLen = doc.createElement("ds:Length");
		//elemKeyInfo.appendChild(elemLen);
		//XMLUtil.setNodeValue(elemLen, Integer.toString(len));
		elemCrypt.appendChild(elemKeyInfo);

		Element elemCipher = doc.createElement("CipherData");
		elemCrypt.appendChild(elemCipher);
		Element elemValue = doc.createElement("CipherValue");
		elemCipher.appendChild(elemValue);

		// add ciphertext to dom document and remove plaintext
		XMLUtil.setValue(elemValue, Base64.encodeBytes(barOutput));
		nodeParent.removeChild(elemPlain);
		nodeParent.appendChild(elemCrypt);

		return elemCrypt;
	}

	/**
	 * Generates a key from a password.
	 *
	 * @param password String password
	 * @param kSalt byte[] random salt
	 * @return the Key with IV
	 * @author Bastian Voigt
	 */
	private static CipherParameters generatePBEKey(String password, byte[] kSalt)
	{
		PKCS12PBEParams kParams = new PKCS12PBEParams(kSalt, MIN_ITERATIONS);
		PKCS12ParametersGenerator paramGen =
			new PKCS12ParametersGenerator(new SHA1Digest());
		paramGen.init(PKCS12ParametersGenerator.PKCS12PasswordToBytes(password.toCharArray()), // muss das hier auch wieder nachher genullt werden?
					  kParams.getIV(), // warum hier IV=Salt? ist in PKCS12.java auch so gemacht, kopiere das einfach ohne es zu verstehen :-)
					  kParams.getIterations().intValue());
		return paramGen.generateDerivedParameters(128); // sind diese laengen so richtig und sinnvoll?
	}

	/**
	 * The part that is the same for encryption and decryption..
	 *
	 * @param encrypt boolean true=encrypt, false=decrypt
	 * @param barInput byte[] input plain or ciphertext
	 * @param params the key
	 * @throws Exception
	 * @return byte[]
	 * @author Bastian Voigt
	 */
	private static byte[] codeDataCTS(boolean encrypt,
									  byte[] barInput,
									  CipherParameters params) throws Exception
	{
		BufferedBlockCipher cipher =
			new CTSBlockCipher(
				new AESFastEngine()
			);
		cipher.init(encrypt, params);

		// encrypt the data
		byte[] barOutput = new byte[cipher.getOutputSize(barInput.length)];
		int len = 0;
		if (barInput.length != 0)
		{
			len = cipher.processBytes(barInput, 0, barInput.length,
									  barOutput, 0);
		}
		cipher.doFinal(barOutput, len);
		return barOutput;
	}

	/**
	 * The part that is the same for encryption and decryption..
	 *
	 * @param encrypt boolean true=encrypt, false=decrypt
	 * @param barInput byte[] input plain or ciphertext
	 * @param params the key
	 * @throws Exception
	 * @return byte[]
	 * @author Bastian Voigt
	 */
	private static byte[] codeDataCBCwithHMAC(boolean encrypt,
											  byte[] barInput,
											  CipherParameters encKey,
											  CipherParameters macKey) throws Exception
	{
		PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
			new CBCBlockCipher(
				new AESFastEngine()
			));
		cipher.init(encrypt, encKey);

		// encrypt the data
		byte[] barOutput = new byte[cipher.getOutputSize(barInput.length)];
		int len = 0;
		if (barInput.length != 0)
		{
			len = cipher.processBytes(barInput, 0, barInput.length,
									  barOutput, 0);
		}
		len += cipher.doFinal(barOutput, len);
		if (!encrypt && len != barOutput.length) //remove padding
		{
			byte[] tmp = new byte[len];
			System.arraycopy(barOutput, 0, tmp, 0, len);
			barOutput = tmp;
		}
		return barOutput;
	}

	/**
	 * Decrypts an XML element
	 *
	 * @todo implement
	 * @param elemCrypt Element
	 * @param password String
	 * @return Element
	 * @author Bastian Voigt
	 */
	public static Element decryptElement(Element elemCrypt, final String password) throws Exception
	{
		return decryptElement(elemCrypt, new SingleStringPasswordReader(password));
	}


	public static Element decryptElement(Element elemCrypt, IMiscPasswordReader a_passwordReader) throws Exception
	{
		Document doc = elemCrypt.getOwnerDocument();
		Node nodeParent = elemCrypt.getParentNode();

		if (a_passwordReader == null)
		{
			a_passwordReader = new SingleStringPasswordReader("");
		}

		// get actual ciphertext from xml structure
		String strType = elemCrypt.getAttribute("Type");
		if (strType == null || !strType.equals("http://www.w3.org/2001/04/xmlenc#Element"))
		{
			throw new IOException("Wrong XML Format");
		}
		Element elemValue = (Element) XMLUtil.getFirstChildByName(elemCrypt, "CipherData");
		elemValue = (Element) XMLUtil.getFirstChildByName(elemValue, "CipherValue");
		byte[] barInput = Base64.decode(XMLUtil.parseValue(elemValue, null));

		// get salt from xml KeyInfo structure
		Element elemKeyInfo = (Element) XMLUtil.getFirstChildByName(elemCrypt, "ds:KeyInfo");
		Element elemSalt = (Element) XMLUtil.getFirstChildByName(elemKeyInfo, "ds:Salt");
		byte[] barSalt = Base64.decode(XMLUtil.parseValue(elemSalt, null));

		// get plaintext length
		//elemSalt = (Element) XMLUtil.getFirstChildByName(elemKeyInfo, "ds:Length");
		//int len = XMLUtil.parseNodeInt(elemSalt, 0);

		byte[] barOutput = null;
		Document doc2 = null;
		Element elemPlain = null;
		String password;
		Exception ex = null;
		while ((password = a_passwordReader.readPassword(null)) != null)
		{
			try
			{

				// decrypt
				barOutput = codeDataCTS(false, barInput, generatePBEKey(password, barSalt));

				// parse decrypted XML
				doc2 = XMLUtil.toXMLDocument(barOutput);
				elemPlain = (Element) XMLUtil.importNode(doc, doc2.getDocumentElement(), true);
				ex = null;
				break;
			}
			catch (Exception a_e)
			{
				ex = a_e;
			}
		}
		if (ex != null)
		{
			throw new IOException("Exception while decrypting (maybe password wrong): " + ex.toString());
		}
		// remove ciphertext from document and add plaintext at the right position
		nodeParent.removeChild(elemCrypt);
		nodeParent.appendChild(elemPlain);

		return elemPlain;
	}

	/** Encrypts an Element using a public key. The element and all of its content is replaced
	 * by the encryption.
	 * The resulting encrypted xml struct is as follows:
	 * <EncryptedData Type='http://www.w3.org/2001/04/xmlenc#Element'>
	 * 	<EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
	 *  	<ds:KeyInfo xmlns:ds='http://www.w3.org/2000/09/xmldsig#'>
	 *			<EncryptedKey>
	 *				<EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
	 *				<CipherData>
	 *					<CipherValue>...</CipherValue>
	 *				</CipherData>
	 *			</EncryptedKey>
	 *		</ds:KeyInfo>
	 *		<CipherData>
	 *			<CipherValue>...</CipherValue>
	 *		</CipherData>
	 *	</EncryptedData>
	 */

	public static boolean encryptElement(Element elemPlain, MyRSAPublicKey publicKey)
	{
		//generate the sym key and IV
		byte[] keyAndIv = new byte[32];
		SecureRandom sec = new SecureRandom();
		sec.nextBytes(keyAndIv);
		CipherParameters params = new ParametersWithIV(new KeyParameter(keyAndIv, 0, 16), keyAndIv, 16, 16);
		// encrypt data
		byte[] barInput = null;
		byte[] barOutput = null;
		try
		{
			barInput = XMLUtil.toString(elemPlain).getBytes();
			barOutput = codeDataCBCwithHMAC(true, barInput, params, null);
		}
		catch (Exception ex1)
		{
			return false;
		}
		//encrpytiong the sym Key and IV
		MyRSA rsa = new MyRSA();
		byte[] encryptedKey;
		try
		{
			rsa.init(publicKey);
			encryptedKey = rsa.processBlockOAEP(keyAndIv, 0, keyAndIv.length);
		}
		catch (Exception ex)
		{
			return false;
		}

		// initialize XML-Encryption-Standard structure
		Document doc = elemPlain.getOwnerDocument();

		Node nodeParent = elemPlain.getParentNode();
		Element elemCrypt = doc.createElement(XML_ELEMENT_NAME);
		elemCrypt.setAttribute("Type", "http://www.w3.org/2001/04/xmlenc#Element");
		elemCrypt.setAttribute("xmlns", "http://www.w3.org/2001/04/xmlenc#");
		Element elemAlgo = doc.createElement("EncryptionMethod");
		elemAlgo.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#aes128-cbc");
		elemCrypt.appendChild(elemAlgo);
		Element elemKeyInfo = doc.createElement("ds:KeyInfo");
		elemKeyInfo.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
		elemCrypt.appendChild(elemKeyInfo);
		Element elemEncKey = doc.createElement("EncryptedKey");
		elemKeyInfo.appendChild(elemEncKey);
		elemAlgo = doc.createElement("EncryptionMethod");
		elemAlgo.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p");
		elemEncKey.appendChild(elemAlgo);
		Element elemCipher = doc.createElement("CipherData");
		elemEncKey.appendChild(elemCipher);
		Element elemValue = doc.createElement("CipherValue");
		elemCipher.appendChild(elemValue);
		XMLUtil.setValue(elemValue, Base64.encodeBytes(encryptedKey));

		elemCipher = doc.createElement("CipherData");
		elemCrypt.appendChild(elemCipher);
		elemValue = doc.createElement("CipherValue");
		elemCipher.appendChild(elemValue);
		XMLUtil.setValue(elemValue, Base64.encodeBytes(barOutput));


		nodeParent.removeChild(elemPlain);
		nodeParent.appendChild(elemCrypt);

		return true;
	}
}
