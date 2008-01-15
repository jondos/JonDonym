package jpi.util;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.crypto.MyAES;
import anon.crypto.MyRandom;
import anon.util.Base64;
import anon.util.IXMLEncodable;
import captcha.ICaptchaGenerator;
import captcha.CaptchaGeneratorFactory;

public class XMLCaptcha implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "CaptchaEncoded";
	private static final String XML_DOCUMENT_VERSION = "1.0";

	private int m_keyBits, m_extraKeyBits;
	private String m_captchaCharacters, m_captchaDataString, m_cipheredData;
	private int m_neededCharacters;
	private ICaptchaGenerator m_captchaGenerator;

	public XMLCaptcha(byte[] a_data, int a_keyBits, int a_extraKeyBits) throws Exception
	{
		m_keyBits = a_keyBits;
		m_extraKeyBits = a_extraKeyBits;
		m_captchaGenerator = CaptchaGeneratorFactory.getInstance().getCaptchaGenerator();
		m_captchaCharacters = m_captchaGenerator.getValidCharacters();
		/* calculate the number of needed captcha characters to get as close to the defined captcha
		 * key length as possible
		 */
		float bitsPerCharacter = (float) (Math.log(m_captchaCharacters.length()) / Math.log(2));
		m_neededCharacters = Math.min(Math.round( ( (float) a_keyBits) / bitsPerCharacter),
									  m_captchaGenerator.getMaximumStringLength());
		String captchaString = "";
		BigInteger alphabetSize = new BigInteger(Integer.toString(m_captchaCharacters.length()));
		BigInteger optimalEncoding = new BigInteger("0");
		for (int i = 0; i < m_neededCharacters; i++)
		{
			/* create the captcha string from random captcha characters */
			int characterPosition = (new MyRandom()).nextInt(m_captchaCharacters.length());
			captchaString = captchaString +
				m_captchaCharacters.substring(characterPosition, characterPosition + 1);
			/* we need also an optimal encoding of the captcha string for calculating the AES key */
			BigInteger currentCharacter = new BigInteger(Integer.toString(characterPosition));
			optimalEncoding = optimalEncoding.multiply(alphabetSize).add(currentCharacter);
		}
		byte[] captchaKey = new byte[a_keyBits / 8];
		for (int i = 0; i < captchaKey.length; i++)
		{
			/* initialize the bytes */
			captchaKey[i] = 0;
		}
		byte[] optimalData = optimalEncoding.toByteArray();
		/* if the optimal encoded data is shorter then the number of a_keyBits, fill it with
		 * 0 at the highest order positions, if the optimal encoded data are longer, truncate the
		 * highest order bits of the optimal encoded data -> that is no problem because the client
		 * will do the same after the user has typed in the captcha data, so the client will get the
		 * same key
		 */
		int usedCaptchaKeyBits = Math.min(captchaKey.length, optimalData.length);
		System.arraycopy(optimalData, optimalData.length - usedCaptchaKeyBits, captchaKey,
						 captchaKey.length - usedCaptchaKeyBits, usedCaptchaKeyBits);
		byte[] extraKey = null;
		if (a_extraKeyBits % 8 == 0)
		{
			extraKey = new byte[a_extraKeyBits / 8];
		}
		else
		{
			/* we need one more byte */
			extraKey = new byte[ (a_extraKeyBits / 8) + 1];
		}
		/* create a random key */
		new SecureRandom().nextBytes(extraKey);
		if (a_extraKeyBits % 8 != 0)
		{
			/* we have to mask the highest byte, because the current key is too long */
			int mask = 0xff;
			mask = mask >>> (8 - (a_extraKeyBits % 8));
			extraKey[0] = (byte) (mask & extraKey[0]);
		}
		/* now put the keys together, the format of the final 128 bit AES key is: bytes 0 .. x are 0,
		 * bytes x+1 .. y are the extra key, bytes y+1 .. 15 are the captcha key, where x and y are
		 * fitting to the extra key length and captcha key length
		 */
		byte[] finalKey = new byte[16];
		for (int i = 0; i < finalKey.length; i++)
		{
			finalKey[i] = 0;
		}
		System.arraycopy(captchaKey, 0, finalKey, finalKey.length - captchaKey.length, captchaKey.length);
		System.arraycopy(extraKey, 0, finalKey, finalKey.length - captchaKey.length - extraKey.length,
						 extraKey.length);
		/* get the ciphered data, we need Base64 encoded data */
		MyAES aes = new MyAES();
		aes.init(true, finalKey);
		/** How many blocks?*/

		int len = a_data.length + (16 - (a_data.length % 16));
		byte[] source = new byte[len];
		System.arraycopy(a_data, 0, source, 0, a_data.length);

		int blocks = (len / 16);

		byte[] encBlock = new byte[16];
		byte[] sourceBlock = new byte[16];
		byte[] cipheredDataBytes = new byte[blocks * 16];
		for (int i = 0; i < blocks; i++)
		{
			System.arraycopy(source, i * 16, sourceBlock, 0, 16);
			encBlock = aes.processBlockECB(sourceBlock);
			System.arraycopy(encBlock, 0, cipheredDataBytes, i * 16, 16);
		}
		m_cipheredData = Base64.encode(cipheredDataBytes, false);

		/* create the captcha, it is already Base64 encoded */
		m_captchaDataString = m_captchaGenerator.createCaptcha(captchaString);

	}

	/**
	 * Return an element that can be appended to the document.
	 *
	 * @param a_doc a document
	 * @return the interface as xml element
	 * @todo Implement this anon.util.IXMLEncodable method
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
		elemRoot.setAttribute("version", XML_DOCUMENT_VERSION);
		Element captchaKeyBitsNode = a_doc.createElement("CaptchaKeyBits");
		captchaKeyBitsNode.appendChild(a_doc.createTextNode(Integer.toString(m_keyBits)));
		elemRoot.appendChild(captchaKeyBitsNode);
		Element extraKeyBitsNode = a_doc.createElement("ExtraKeyBits");
		extraKeyBitsNode.appendChild(a_doc.createTextNode(Integer.toString(m_extraKeyBits)));
		elemRoot.appendChild(extraKeyBitsNode);
		Element captchaCharactersNode = a_doc.createElement("CaptchaCharacters");
		captchaCharactersNode.appendChild(a_doc.createTextNode(m_captchaCharacters));
		elemRoot.appendChild(captchaCharactersNode);
		Element captchaCharacterNumberNode = a_doc.createElement("CaptchaCharacterNumber");
		captchaCharacterNumberNode.appendChild(a_doc.createTextNode(Integer.toString(m_neededCharacters)));
		elemRoot.appendChild(captchaCharacterNumberNode);
		Element captchaDataFormatNode = a_doc.createElement("CaptchaDataFormat");
		captchaDataFormatNode.appendChild(a_doc.createTextNode(m_captchaGenerator.getCaptchaDataFormat()));
		elemRoot.appendChild(captchaDataFormatNode);
		Element captchaDataNode = a_doc.createElement("CaptchaData");
		captchaDataNode.appendChild(a_doc.createTextNode(m_captchaDataString));
		elemRoot.appendChild(captchaDataNode);
		Element forwarderCipherNode = a_doc.createElement("DataCipher");
		forwarderCipherNode.appendChild(a_doc.createTextNode(m_cipheredData));
		elemRoot.appendChild(forwarderCipherNode);

		return elemRoot;
	}
}
