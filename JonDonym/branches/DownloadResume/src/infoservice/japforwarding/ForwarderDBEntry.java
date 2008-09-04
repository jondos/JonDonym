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
package infoservice.japforwarding;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SecureRandom;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.MyAES;
import anon.crypto.MyRandom;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Constants;
import anon.util.Base64;
import captcha.*;
import anon.util.XMLUtil;

/**
 * This is the implementation of a JAP forwarder entry, which can be stored in the forwarder
 * database.
 */
public class ForwarderDBEntry extends AbstractDatabaseEntry
{

	/**
	 * This is the number of bits of an AES key, which are embedded within a captcha. This value
	 * must be a multiple of 8. Also this number plus the number of extra key bits have not to
	 * be bigger than 128 (the sum of CAPTCHA_KEY_BITS + EXTRA_KEY_BITS should be less than 128
	 * because the difference between 128 and that sum is used as redundancy to show the blockee,
	 * whether he has solved the captcha correctly or not, so that sum shouldn't be bigger than
	 * around 90). Whether we can create a captcha with this number of embedded bits, depends also
	 * on the used captcha implementation. We try to create a captcha with the number of embedded
	 * bits as near as possible to this value.
	 */
	private static final int CAPTCHA_KEY_BITS = 48;

	/**
	 * This is the number of extra key bits (maybe 0), which are not embedded in the captcha. If the
	 * number of key bits embedded in the captcha is very small, the attacker could solve the
	 * captcha via a brute-force attack. Because of the redundancy in the 128 bit AES block, he
	 * would know, when he has broken the captcha. With this extra key bits, we can make the attack
	 * more expensive because the attacker has to do the brute-force attack over the captcha key
	 * bits and the extra key bits. But a normal blockee doesn't know those extra key bits too. He
	 * has also to do a brute-force tryout, but only over those extra key bits. So this value
	 * shouldn't be to big, around 16 should be ok. Keep in mind that if you use extra key bits, the
	 * blockee needs some redundancy for verifying that he has found the right extra key. So the sum
	 * of CAPTCHA_KEY_BITS + EXTRA_KEY_BITS shouldn't be bigger than 90. The EXTRA_KEY_BITS need
	 * not!!! to be a multiple of 8.
	 */
	private static final int EXTRA_KEY_BITS = 16;

	/**
	 * Stores the internet address of the forwarder. At the moment only IPv4 addresses are
	 * supported.
	 */
	private InetAddress m_forwarderAddress;

	/**
	 * Stores the port number of the forwarder.
	 */
	private int m_forwarderPort;

	/**
	 * Stores the plain data which are used when generating the cipher data with the captcha key.
	 * It's a 128 bit data block (for encryption with AES). The bytes 0 ... 9 are always 0, the
	 * bytes 10 ... 13 are the IP address of the forwarder in network byte order, the highest order
	 * byte is byte 10. The bytes 14, 15 are the port of the forwarder, where byte 14 is the highest
	 * order byte.
	 */
	private byte[] m_forwarderData;

	/**
	 * Stores the ID for this ForwarderDBEntry. It's a Base64 encoded String of a 128 bit random
	 * number.
	 */
	private String m_id;

	/**
	 * Stores the time when this ForwarderDBEntry was created. This value is used to determine the
	 * more recent entry, if two entries are compared (higher version number -> more recent entry).
	 */
	private long m_creationTimeStamp;

	/**
	 * Creates a new ForwarderDBEntry. Also an ID is generated for this entry. The ID is a 128 bit
	 * random number, stored as a Base64 encoded string. This constructor can throw an Exception
	 * but this should never happen.
	 *
	 * @param a_forwarderAddress The internet address of the forwarder. At the moment, only IPv4
	 *                           addresses are supported.
	 * @param a_forwarderPort The port number the forwarder listens on.
	 */
	public ForwarderDBEntry(InetAddress a_forwarderAddress, int a_forwarderPort) throws Exception
	{
		super(System.currentTimeMillis() + Constants.TIMEOUT_JAP_FORWARDERS);
		m_creationTimeStamp = System.currentTimeMillis();
		m_forwarderAddress = a_forwarderAddress;
		m_forwarderPort = a_forwarderPort;
		/* create the plain data block */
		m_forwarderData = new byte[16];
		for (int i = 0; i < 10; i++)
		{
			m_forwarderData[i] = 0;
		}
		System.arraycopy(m_forwarderAddress.getAddress(), 0, m_forwarderData, 10, 4);
		ByteArrayOutputStream transformerOut = new ByteArrayOutputStream(4);
		DataOutputStream transformer = new DataOutputStream(transformerOut);
		transformer.writeInt(a_forwarderPort);
		transformer.flush();
		transformerOut.flush();
		/* we need only the lower to bytes of the port (the higher 2 bytes are always 0) */
		System.arraycopy(transformerOut.toByteArray(), 2, m_forwarderData, 14, 2);
		transformer.close();
		/* generate the id */
		byte[] randomId = new byte[16];
		new SecureRandom().nextBytes(randomId);
		m_id = Base64.encode(randomId, false);
	}

	/**
	 * This internal constructor is needed for creating a clone of a ForwarderDBEntry, needed for
	 * updating the forwarder database. The creation time is set to the current system time.
	 *
	 * @param a_forwarderAddress The internet address of the forwarder.
	 * @param a_forwarderPort The port number the forwarder listens on.
	 * @param a_forwarderData The data block with the IP and port of the forwarder, which is used
	 *                        for AES encryption.
	 * @param a_id The ID of this forwarder database entry.
	 */
	private ForwarderDBEntry(InetAddress a_forwarderAddress, int a_forwarderPort, byte[] a_forwarderData,
							 String a_id)
	{
		super(System.currentTimeMillis() + Constants.TIMEOUT_JAP_FORWARDERS);
		m_creationTimeStamp = System.currentTimeMillis();
		m_forwarderAddress = a_forwarderAddress;
		m_forwarderPort = a_forwarderPort;
		m_forwarderData = a_forwarderData;
		m_id = a_id;
	}

	/**
	 * Returns a clone of this ForwarderDBEntry. The clone is identical to this instance except
	 * the creation time, which is set to the current time, and the updates number, which is
	 * rised by 1.
	 *
	 * @return The clone of this entry needed for updating the forwarder database.
	 */
	public ForwarderDBEntry getUpdateClone()
	{
		return (new ForwarderDBEntry(m_forwarderAddress, m_forwarderPort, m_forwarderData, m_id));
	}

	/**
	 * Returns a unique ID for this forwarder database entry. It's a Base64 encoded string of
	 * a 128 bit random number.
	 *
	 * @return The ID of this forwarder database entry.
	 */
	public String getId()
	{
		return m_id;
	}

	public long getLastUpdate()
	{
		return m_creationTimeStamp;
	}

	/**
	 * Returns the time when this ForwarderDBEntry was created.
	 *
	 * @return A version number which is used to determine the more recent entry, if two entries are
	 *         compared (higher version number -> more recent entry).
	 */
	public long getVersionNumber()
	{
		return m_creationTimeStamp;
	}

	/**
	 * Creates the JapForwarder node with the captcha data included. This node can be transferd
	 * to the blockee, who can solve the captcha and gets the forwarder information, which is
	 * encoded with the key embedded in the captcha. This method creates everything needed for
	 * transfer to the blockee. The method can throw an Exception, when there is something wrong
	 * with the XML transformation, but this should never happen.
	 *
	 * @return The JapForwarder node for transfer to the blockee.
	 */
	public Element createCaptchaNode() throws Exception
	{
		ICaptchaGenerator captchaGenerator = CaptchaGeneratorFactory.getInstance().getCaptchaGenerator();
		String captchaCharacters = captchaGenerator.getValidCharacters();
		/* calculate the number of needed captcha characters to get as close to the defined captcha
		 * key length as possible
		 */
		float bitsPerCharacter = (float) (Math.log(captchaCharacters.length()) / Math.log(2));
		int neededCharacters = Math.min(Math.round( ( (float) CAPTCHA_KEY_BITS) / bitsPerCharacter),
										captchaGenerator.getMaximumStringLength());
		String captchaString = "";
		BigInteger alphabetSize = new BigInteger(Integer.toString(captchaCharacters.length()));
		BigInteger optimalEncoding = new BigInteger("0");
		for (int i = 0; i < neededCharacters; i++)
		{
			/* create the captcha string from random captcha characters */
			int characterPosition = (new MyRandom()).nextInt(captchaCharacters.length());
			captchaString = captchaString +
				captchaCharacters.substring(characterPosition, characterPosition + 1);
			/* we need also an optimal encoding of the captcha string for calculating the AES key */
			BigInteger currentCharacter = new BigInteger(Integer.toString(characterPosition));
			optimalEncoding = optimalEncoding.multiply(alphabetSize).add(currentCharacter);
		}
		byte[] captchaKey = new byte[CAPTCHA_KEY_BITS / 8];
		for (int i = 0; i < captchaKey.length; i++)
		{
			/* initialize the bytes */
			captchaKey[i] = 0;
		}
		byte[] optimalData = optimalEncoding.toByteArray();
		/* if the optimal encoded data are shorter then the number of CAPTCHA_KEY_BITS, fill it with
		 * 0 at the highest order positions, if the optimal encoded data are longer, truncate the
		 * highest order bits of the optimal encoded data -> that is no problem because the client
		 * will do the same after the user has typed in the captcha data, so the client will get the
		 * same key
		 */
		int usedCaptchaKeyBits = Math.min(captchaKey.length, optimalData.length);
		System.arraycopy(optimalData, optimalData.length - usedCaptchaKeyBits, captchaKey,
						 captchaKey.length - usedCaptchaKeyBits, usedCaptchaKeyBits);
		byte[] extraKey = null;
		if (EXTRA_KEY_BITS % 8 == 0)
		{
			extraKey = new byte[EXTRA_KEY_BITS / 8];
		}
		else
		{
			/* we need one more byte */
			extraKey = new byte[ (EXTRA_KEY_BITS / 8) + 1];
		}
		/* create a random key */
		new SecureRandom().nextBytes(extraKey);
		if (EXTRA_KEY_BITS % 8 != 0)
		{
			/* we have to mask the highest byte, because the current key is to long */
			int mask = 0xff;
			mask = mask >>> (8 - (EXTRA_KEY_BITS % 8));
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
		byte[] cipheredForwarderData = aes.processBlockECB(m_forwarderData);
		String cipheredForwarder = Base64.encode(cipheredForwarderData, false);
		/* create the captcha, it is already Base64 encoded */
		String captchaData = captchaGenerator.createCaptcha(captchaString);
		/* we now have everything needed for creating the xml structure */
		Document doc = XMLUtil.createDocument();
		/* create the JapForwarder element */
		Element japForwarderNode = doc.createElement("JapForwarder");
		/* create the CaptchaEncoded element (child of JapForwarder) */
		Element captchaEncodedNode = doc.createElement("CaptchaEncoded");
		/* create the children of CaptchaKeyBits, CaptchaEncoded (ExtraKeyBits, CaptchaCharacters,
		 * CaptchaCharacterNumber, CaptchaDataFormat, CaptchaData, ForwarderCipher)
		 */
		Element captchaKeyBitsNode = doc.createElement("CaptchaKeyBits");
		captchaKeyBitsNode.appendChild(doc.createTextNode(Integer.toString(CAPTCHA_KEY_BITS)));
		captchaEncodedNode.appendChild(captchaKeyBitsNode);
		Element extraKeyBitsNode = doc.createElement("ExtraKeyBits");
		extraKeyBitsNode.appendChild(doc.createTextNode(Integer.toString(EXTRA_KEY_BITS)));
		captchaEncodedNode.appendChild(extraKeyBitsNode);
		Element captchaCharactersNode = doc.createElement("CaptchaCharacters");
		captchaCharactersNode.appendChild(doc.createTextNode(captchaCharacters));
		captchaEncodedNode.appendChild(captchaCharactersNode);
		Element captchaCharacterNumberNode = doc.createElement("CaptchaCharacterNumber");
		captchaCharacterNumberNode.appendChild(doc.createTextNode(Integer.toString(neededCharacters)));
		captchaEncodedNode.appendChild(captchaCharacterNumberNode);
		Element captchaDataFormatNode = doc.createElement("CaptchaDataFormat");
		captchaDataFormatNode.appendChild(doc.createTextNode(captchaGenerator.getCaptchaDataFormat()));
		captchaEncodedNode.appendChild(captchaDataFormatNode);
		Element captchaDataNode = doc.createElement("CaptchaData");
		captchaDataNode.appendChild(doc.createTextNode(captchaData));
		captchaEncodedNode.appendChild(captchaDataNode);
		Element forwarderCipherNode = doc.createElement("DataCipher");
		forwarderCipherNode.appendChild(doc.createTextNode(cipheredForwarder));
		captchaEncodedNode.appendChild(forwarderCipherNode);
		///@todo remove some day: For compatibility reasons we include also the old format here
		forwarderCipherNode = doc.createElement("ForwarderCipher");
		forwarderCipherNode.appendChild(doc.createTextNode(cipheredForwarder));
		captchaEncodedNode.appendChild(forwarderCipherNode);
		japForwarderNode.appendChild(captchaEncodedNode);
		return japForwarderNode;
	}

}
