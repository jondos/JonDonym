/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package anon.util.captcha;

import java.awt.Image;
import java.math.BigInteger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.crypto.MyAES;
import anon.util.Base64;
import anon.util.ZLibTools;

/**
 * This is the client implementation for all captchas of the "ZIP_BINARY_IMAGE" format.
 */
public class ZipBinaryImageCaptchaClient implements IImageEncodedCaptcha
{

	/**
	 * This is the captcha format identifier for the class of captchas, which can be handled by
	 * this class implementation. We are handling the "ZIP_BINARY_IMAGE" format.
	 */
	public static final String CAPTCHA_DATA_FORMAT = "ZIP_BINARY_IMAGE";

	/**
	 * Stores the captcha image.
	 */
	private Image m_captchaImage;

	/**
	 * Stores the number of captcha key bits. This is the number of bits, we shall obtain via
	 * solving the captcha image. If there are not enough characters in the captcha, we pad the
	 * difference between this number and the real captcha information with 0.
	 */
	private int m_captchaKeyBits;

	/**
	 * Stores the number of extra key bits (bits of the key, which we have to guess with brute
	 * force).
	 */
	private int m_extraKeyBits;

	/**
	 * Stores the character set, which was used when the captcha was created.
	 */
	private String m_characterSet;

	/**
	 * Stores the number of characters, which are embedded in the captcha image.
	 */
	private int m_characterNumber;

	/**
	 * Stores the encoded information.
	 */
	private byte[] m_encodedData;

	/**
	 * Creates a new instance of ZipBinaryImageClient, which handles all captchas of the
	 * "ZIP_BINARY_IMAGE" format. If something is wrong with the specified XML captcha data
	 * (e.g. data with the wrong captcha format or invalid data), an Exception is thrown.
	 *
	 * @param a_captchaEncodedNode The CaptchaEncoded node with the data of a "ZIP_BINARY_IMAGE"
	 *                             captcha.
	 */
	public ZipBinaryImageCaptchaClient(Element a_captchaEncodedNode) throws Exception
	{
		byte[] compressedImageData;
		byte[] unCompressedImageData;

		/* process the CaptchaEncoded node tree */
		/* first check the format */
		NodeList captchaDataFormatNodes = a_captchaEncodedNode.getElementsByTagName("CaptchaDataFormat");
		if (captchaDataFormatNodes.getLength() == 0)
		{
			throw (new Exception(
				"ZipBinaryImageCaptchaClient: Error in XML structure (CaptchaDataFormat node)."));
		}
		Element captchaDataFormatNode = (Element) (captchaDataFormatNodes.item(0));
		if (!CAPTCHA_DATA_FORMAT.equals(captchaDataFormatNode.getFirstChild().getNodeValue()))
		{
			throw (new Exception("ZipBinaryImageCaptchaClient: Wrong captcha format."));
		}
		/* get the image */
		NodeList captchaDataNodes = a_captchaEncodedNode.getElementsByTagName("CaptchaData");
		if (captchaDataNodes.getLength() == 0)
		{
			throw (new Exception("ZipBinaryImageCaptchaClient: Error in XML structure. (CaptchaData node)."));
		}
		Element captchaDataNode = (Element) (captchaDataNodes.item(0));
		compressedImageData = Base64.decode(captchaDataNode.getFirstChild().getNodeValue());
		unCompressedImageData = ZLibTools.decompress(compressedImageData);
		if (unCompressedImageData == null)
		{
			throw (new Exception("ZipBinaryImageCaptchaClient: Error while decompressing the captcha data."));
		}
		m_captchaImage = BinaryImageExtractor.binaryToImage(unCompressedImageData);
		if (m_captchaImage == null)
		{
			throw (new Exception("ZipBinaryImageCaptchaClient: The image is invalid."));
		}
		/* get the encoded information */
		NodeList cipherNodes = a_captchaEncodedNode.getElementsByTagName("DataCipher");

		if (cipherNodes.getLength() == 0)
		{
			throw (new Exception(
				"ZipBinaryImageCaptchaClient: Error in XML structure. (DataCipher node)."));
		}
		Element forwarderCipherNode = (Element) (cipherNodes.item(0));
		m_encodedData = Base64.decode(forwarderCipherNode.getFirstChild().getNodeValue());
		/* get the other parameters */
		NodeList captchaKeyBitsNodes = a_captchaEncodedNode.getElementsByTagName("CaptchaKeyBits");
		if (captchaKeyBitsNodes.getLength() == 0)
		{
			throw (new Exception(
				"ZipBinaryImageCaptchaClient: Error in XML structure. (CaptchaKeyBits node)."));
		}
		Element captchaKeyBitsNode = (Element) (captchaKeyBitsNodes.item(0));
		m_captchaKeyBits = Integer.parseInt(captchaKeyBitsNode.getFirstChild().getNodeValue());
		NodeList extraKeyBitsNodes = a_captchaEncodedNode.getElementsByTagName("ExtraKeyBits");
		if (extraKeyBitsNodes.getLength() == 0)
		{
			throw (new Exception("ZipBinaryImageCaptchaClient: Error in XML structure. (ExtraKeyBits node)."));
		}
		Element extraKeyBitsNode = (Element) (extraKeyBitsNodes.item(0));
		m_extraKeyBits = Integer.parseInt(extraKeyBitsNode.getFirstChild().getNodeValue());
		NodeList captchaCharactersNodes = a_captchaEncodedNode.getElementsByTagName("CaptchaCharacters");
		if (captchaCharactersNodes.getLength() == 0)
		{
			throw (new Exception(
				"ZipBinaryImageCaptchaClient: Error in XML structure. (CaptchaCharacters node)."));
		}
		Element captchaCharactersNode = (Element) (captchaCharactersNodes.item(0));
		m_characterSet = captchaCharactersNode.getFirstChild().getNodeValue();
		NodeList captchaCharacterNumberNodes = a_captchaEncodedNode.getElementsByTagName(
			"CaptchaCharacterNumber");
		if (captchaCharacterNumberNodes.getLength() == 0)
		{
			throw (new Exception(
				"ZipBinaryImageCaptchaClient: Error in XML structure. (CaptchaCharacterNumber node)."));
		}
		Element captchaCharacterNumberNode = (Element) (captchaCharacterNumberNodes.item(0));
		m_characterNumber = Integer.parseInt(captchaCharacterNumberNode.getFirstChild().getNodeValue());
	}

	/**
	 * Returns the image with the captcha data.
	 */
	public Image getImage()
	{
		return m_captchaImage;
	}

	/**
	 * Returns the character set which was used when the captcha was created. Only characters from
	 * this set are in the captcha image.
	 */
	public String getCharacterSet()
	{
		return m_characterSet;
	}

	/**
	 * Returns the number of characters which are included in the captcha.
	 */
	public int getCharacterNumber()
	{
		return m_characterNumber;
	}

	/**
	 * Solves the captcha and returns the encoded information. The key is the character string visible in the captcha image. If the
	 * wrong key is specified, an Exception is thrown.
	 *
	 * @param a_key The key for solving the captcha (it's the string which is shown in the captcha
	 *              image).
	 *
	 * @return The encoded data
	 */
	public byte[] solveCaptcha(String a_key, byte[] a_startsWith) throws Exception
	{
		if (a_key.length() != m_characterNumber)
		{
			throw (new Exception(
				"ZipBinaryImageCaptchaClient: solveCaptcha: The specified key has an invalid size."));
		}
		BigInteger alphabetSize = new BigInteger(Integer.toString(m_characterSet.length()));
		BigInteger optimalEncoding = new BigInteger("0");
		for (int i = 0; i < m_characterNumber; i++)
		{
			/* get the position the captcha string from random captcha characters */
			int characterPosition = m_characterSet.indexOf(a_key.substring(i, i + 1));
			if (characterPosition == -1)
			{
				throw (new Exception(
					"ZipBinaryImageCaptchaClient: solveCaptcha: The specified key contains invalid characters."));
			}
			BigInteger currentCharacter = new BigInteger(Integer.toString(characterPosition));
			optimalEncoding = optimalEncoding.multiply(alphabetSize).add(currentCharacter);
		}
		byte[] captchaKey = new byte[m_captchaKeyBits / 8];
		for (int i = 0; i < captchaKey.length; i++)
		{
			/* initialize the bytes */
			captchaKey[i] = 0;
		}
		byte[] optimalData = optimalEncoding.toByteArray();
		/* if the optimal encoded data are shorter then the number of CAPTCHA_KEY_BITS, fill it with
		 * 0 at the highest order positions, if the optimal encoded data are longer, truncate the
		 * highest order bits of the optimal encoded data -> that is no problem because the
		 * infoservice has done the same, so the infoservices used the same key here
		 */
		int usedCaptchaKeyBits = Math.min(captchaKey.length, optimalData.length);
		System.arraycopy(optimalData, optimalData.length - usedCaptchaKeyBits, captchaKey,
						 captchaKey.length - usedCaptchaKeyBits, usedCaptchaKeyBits);
		byte[] extraKey = null;
		int mod8ExtraKeyBits = m_extraKeyBits % 8;
		if (mod8ExtraKeyBits == 0)
		{
			extraKey = new byte[m_extraKeyBits / 8];
		}
		else
		{
			/* we need one more byte */
			extraKey = new byte[ (m_extraKeyBits / 8) + 1];
		}
		/* create the first key */
		for (int i = 0; i < extraKey.length; i++)
		{
			extraKey[i] = 0;
		}
		boolean plainDataValid = true;
		do
		{
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
			MyAES aes = new MyAES();
			aes.init(false, finalKey);
			/** How many blocks?*/
			int len = m_encodedData.length;
			int blocks = len / 16;
			byte[] decBlock = new byte[16];
			byte[] sourceBlock = new byte[16];
			byte[] plainForwarderData = new byte[blocks * 16];

			for (int i = 0; i < blocks; i++)
			{
				System.arraycopy(m_encodedData, i * 16, sourceBlock, 0, 16);
				decBlock = aes.processBlockECB(sourceBlock);
				System.arraycopy(decBlock, 0, plainForwarderData, i * 16, 16);
			}

			/* check whether the plain data is valid*/
			plainDataValid = true;
			for (int i = 0; i < a_startsWith.length; i++)
			{
				if (plainForwarderData[i] != a_startsWith[i])
				{
					plainDataValid = false;
				}
			}

			if (!plainDataValid)
			{
				/* we have used the wrong key -> try the next extra key, if the current one was the last,
				 * an Exception is thrown and we will end the solveCaptcha method
				 */
				try
				{
				extraKey = generateNextKey(extraKey, mod8ExtraKeyBits);
				}
				catch (Exception e)
				{
					return null;
				}
			}
			else
			{
				return plainForwarderData;
			}
		}
		while (!plainDataValid);
		/* we have solved the captcha */
		return null;
	}

	/**
	 * Generates the next key, which is equal to the current key + 1. If that next key is 000...000
	 * (overflow), an Exception is thrown. So if you start with the 000...000 key, you will know
	 * when you have tried all keys.
	 *
	 * @param a_currentKey The current key, you should start with 000...000.
	 * @param a_mod8KeyBits This is the number of key bits mod 8 in the current key. This value
	 *                      describes how many bits of the highest order byte are used (attention:
	 *                      a value of 0 means, that all bits are used).
	 *
	 * @return The key following to the a_currentKey (a_currentKey + 1).
	 */
	private byte[] generateNextKey(byte[] a_currentKey, int a_mod8KeyBits) throws Exception
	{
		/* so it's save */
		a_mod8KeyBits = a_mod8KeyBits % 8;
		byte[] nextKey = new byte[a_currentKey.length];
		boolean overflow = true;
		for (int i = nextKey.length - 1; i >= 0; i--)
		{
			byte currentByte = a_currentKey[i];
			if (overflow == true)
			{
				currentByte = (byte) (currentByte + 1);
				if ( (i != 0) || (a_mod8KeyBits == 0))
				{
					/* one of the lower bytes, or the highest byte is also full used -> overflow, if the
					 * byte is 0 again
					 */
					if (currentByte != 0)
					{
						/* no overflow -> don't change the higher bytes */
						overflow = false;
					}
				}
				else
				{
					/* we are at the highest byte -> uses the a_mod8KeyBits to decide, whether there is
					 * an overflow
					 */
					int mask = 0xff;
					mask = mask >>> (8 - a_mod8KeyBits);
					currentByte = (byte) (mask & currentByte);
					if (currentByte != 0)
					{
						overflow = false;
					}
				}
			}
			nextKey[i] = currentByte;
		}
		if (overflow == true)
		{
			/* there was an overflow in the highest byte -> no more keys available -> throw an
			 * exception
			 */
			throw (new Exception("ZipBinaryImageCaptchaClient: generateNextKey: No more keys available."));
		}
		return nextKey;
	}

}
