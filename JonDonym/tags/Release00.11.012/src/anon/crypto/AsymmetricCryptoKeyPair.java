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
package anon.crypto;

import java.security.InvalidKeyException;
import java.security.Key;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import anon.util.ClassUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * A key pair used for signing and encryption with an asymmetric cryptographic algorithm.
 * @author Rolf Wendolsky
 */
public class AsymmetricCryptoKeyPair
{
	public static final int KEY_LENGTH_512 = 512;
	public static final int KEY_LENGTH_1024 = 1024;

	// register the mandatory key classes so that they are compiled; do not remove!
	private static final MyDSAPrivateKey dsaKey = null;
	private static final MyRSAPrivateKey rsaKey = null;
	private static final MyECPrivateKey ecKey = null;

	/**
	 * Stores all registered private key classes.
	 */
	private static Vector ms_privateKeyClasses;
	/**
	 * Stores all registered public key classes.
	 */
	private static Vector ms_publicKeyClasses;

	private IMyPrivateKey m_privateKey;
	private IMyPublicKey m_publicKey;

	/**
	 * Creates a new key pair from a private key.
	 * @param a_privateKey a private key
	 */
	public AsymmetricCryptoKeyPair(IMyPrivateKey a_privateKey)
	{
		m_privateKey = a_privateKey;
		m_publicKey = a_privateKey.createPublicKey();
	}

	/**
	 * Creates a new key pair from a PrivateKeyInfo. It does this by creating a new instance
	 * of all available private key classes with the PrivateKeyInfo as constructor argument.
	 * Therefore, all private key classes that should be able to be loaded dynamically must
	 * implement a constructor that has only one argument - a PrivateKeyInfo.
	 * @param a_keyInfo a PrivateKeyInfo object
	 * @see anon.crypto.IMyPrivateKey
	 * @see anon.util.ClassUtil#loadClasses()
	 * @exception InvalidKeyException if no private key could be created from this key info
	 */
	public AsymmetricCryptoKeyPair(PrivateKeyInfo a_keyInfo)
		throws InvalidKeyException
	{
		IMyPrivateKey privateKey;

		try
		{
			privateKey = (IMyPrivateKey) createAsymmetricCryptoKey(a_keyInfo, getPrivateKeyClasses());
		}
		catch (ClassCastException a_e)
		{
			throw new InvalidKeyException("The key that was created was no private key!");
		}

		m_privateKey = privateKey;
		m_publicKey = privateKey.createPublicKey();
	}

	/**
	 * Creates a new public key from a SubjectPublicKeyInfo. It does this by creating a new instance
	 * of all available public key classes with the SubjectPublicKeyInfo as constructor argument.
	 * Therefore, all public key classes that should be able to be loaded dynamically must
	 * implement a constructor that has only one argument - a SubjectPublicKeyInfo.
	 * @param a_keyInfo a SubjectPublicKeyInfo describing a public key
	 * @return the corresponding public key to this key info
	 * @exception InvalidKeyException if no public key could be created from this key info
	 * @see anon.util.ClassUtil#loadClasses()
	 * @see anon.crypto.IMyPublicKey
	 */
	public static final IMyPublicKey createPublicKey(SubjectPublicKeyInfo a_keyInfo)
		throws InvalidKeyException
	{
		IMyPublicKey publicKey;

		try
		{
			publicKey = (IMyPublicKey) createAsymmetricCryptoKey(a_keyInfo, getPublicKeyClasses());
		}
		catch (ClassCastException a_e)
		{
			throw new InvalidKeyException("The key that was created was no public key!");
		}

		return publicKey;
	}

	/**
	 * Returns the private key.
	 * @return the private key
	 */
	public final IMyPrivateKey getPrivate()
	{
		return m_privateKey;
	}

	/**
	 * Returns the public key.
	 * @return the public key
	 */
	public final IMyPublicKey getPublic()
	{
		return m_publicKey;
	}

	/**
	 * Tests if a given key pair is a valid key pair. Key pair create or getInstance methods
	 * should not return a key pair the has not been tested with this method!
	 * @todo check encryption/decryption once they are available...
	 * @param a_keyPair an AsymmetricKeyPair
	 * @return true if the key pair is valid; false otherwise
	 */
	protected static final boolean isValidKeyPair(AsymmetricCryptoKeyPair a_keyPair)
	{
		Random random;
		byte[] message;
		byte[] signature;
		byte[] dummySignature;

		if (a_keyPair == null)
		{
			return false;
		}

		// create a random message
		random = new Random();
		random.setSeed(0);
		message = new byte[1024];
		random.nextBytes(message);

		// test encryption/decryption
		// ....

		// test signing and verifying
		signature = ByteSignature.sign(message, a_keyPair);
		dummySignature = new byte[signature.length];
		random.nextBytes(dummySignature);

		try
		{
			if (ByteSignature.verify(message, dummySignature, a_keyPair))
			{
				// verifying of the dummy signature must fail!
				return false;
			}
		}
		catch (Exception a_e)
		{
			// ignore any exception here
		}

		if (!ByteSignature.verify(message, signature, a_keyPair))
		{
			return false;
		}

		return true;
	}

	private static Key createAsymmetricCryptoKey(Object a_keyInfo, Enumeration a_keyClasses)
		throws InvalidKeyException
	{
		Key key = null;
		Class keyClass;
		Class[] parameterTypes;
		Object[] parameters;

		parameterTypes = new Class[1];
		parameters = new Object[1];
		parameterTypes[0] = a_keyInfo.getClass();
		parameters[0] = a_keyInfo;

		while (key == null && a_keyClasses.hasMoreElements())
		{
			keyClass = (Class)a_keyClasses.nextElement();
			try
			{
				key = (Key) keyClass.getConstructor(parameterTypes).newInstance(parameters);
			}
			catch (Throwable a_e)
			{
				// this is not the right key for this key info; ignore this error
			}
		}

		if (key == null)
		{
			throw new InvalidKeyException("No key available for this key info!");
		}

		return key;
	}

	/**
	 * Returns all registered private key classes.
	 * @return all registered private key classes
	 * @see anon.util.ClassUtil#loadClasses()
	 * @see anon.crypto.IMyPrivateKey
	 */
	private static Enumeration getPrivateKeyClasses()
	{
		if (ms_privateKeyClasses == null)
		{
			try
			{
				ms_privateKeyClasses = ClassUtil.findSubclasses(IMyPrivateKey.class);
				ms_privateKeyClasses.removeElement(IMyPrivateKey.class);
			}
			catch (Throwable a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, a_e);
			}
			if (ms_privateKeyClasses == null)
			{
				ms_privateKeyClasses = new Vector();
			}

			if (ms_privateKeyClasses.size() < 2)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO,
							  "Private key classes have not been loaded automatically!");
				// load them manually and prevent double references
				ms_privateKeyClasses.removeElement(MyDSAPrivateKey.class);
				ms_privateKeyClasses.removeElement(MyRSAPrivateKey.class);
				ms_privateKeyClasses.addElement(MyDSAPrivateKey.class);
				ms_privateKeyClasses.addElement(MyRSAPrivateKey.class);
			}
		}

		return ms_privateKeyClasses.elements();
	}

	/**
	 * Returns all registered public key classes.
	 * @return all registered public key classes
	 * @see anon.util.ClassUtil#loadClasses()
	 * @see anon.crypto.IMyPublicKey
	 */
	private static Enumeration getPublicKeyClasses()
	{
		if (ms_publicKeyClasses == null)
		{
			try
			{
				ms_publicKeyClasses = ClassUtil.findSubclasses(IMyPublicKey.class);
				ms_publicKeyClasses.removeElement(IMyPublicKey.class);
			}
			catch (Throwable a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, a_e);
			}
			if (ms_publicKeyClasses == null)
			{
				ms_publicKeyClasses = new Vector();
			}

			if (ms_publicKeyClasses.size() < 2)
			{
				int level;
				if (ClassUtil.isFindSubclassesEnabled())
				{
					level = LogLevel.EXCEPTION;
				}
				else
				{
					level = LogLevel.NOTICE;
				}
				LogHolder.log(level, LogType.CRYPTO,
				"Public key classes have not been loaded automatically!");
				// load them manually and prevent double references
				ms_publicKeyClasses.removeElement(MyDSAPublicKey.class);
				ms_publicKeyClasses.removeElement(MyRSAPublicKey.class);
				ms_publicKeyClasses.addElement(MyDSAPublicKey.class);
				ms_publicKeyClasses.addElement(MyRSAPublicKey.class);
			}
		}

		return ms_publicKeyClasses.elements();
	}
}
