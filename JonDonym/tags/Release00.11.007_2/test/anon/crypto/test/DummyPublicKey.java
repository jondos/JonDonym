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
package anon.crypto.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;

import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import anon.crypto.IMyPublicKey;
import anon.crypto.IMySignature;
import anon.crypto.ISignatureVerificationAlgorithm;
import anon.util.Util;

/**
 * This is a dummy implementation of a public key.
 * @author Rolf Wendolsky
 */
public class DummyPublicKey extends DummySignatureKey implements IMyPublicKey
{
	private static final AlgorithmIdentifier IDENTIFIER =
		new AlgorithmIdentifier(new DERObjectIdentifier("0.0.0.0.0.0.2"));

	private IMySignature m_algorithm = new DummySignatureAlgorithm();

	/**
	 * Creates a new dummy public key.
	 * @param a_key the public key as long value
	 */
	public DummyPublicKey(long a_key)
	{
		super(a_key);
	}

	public DummyPublicKey(SubjectPublicKeyInfo a_keyInfo) throws IOException
	{
		super(((DERInteger)a_keyInfo.getPublicKey()).getValue().longValue());
		if (!Util.arraysEqual(a_keyInfo.getAlgorithmId().getEncoded(),
			IDENTIFIER.getEncoded()))
		{
			throw new IOException("Not a dummy private key!");
		}
	}

	/**
	 * Creates a new signature algorithm object for this key.
	 * @return a new signature algorithm object for this key
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

	public SubjectPublicKeyInfo getAsSubjectPublicKeyInfo()
	{
		return new SubjectPublicKeyInfo(IDENTIFIER,
										new DERInteger(BigInteger.valueOf(getKeyValue())));
	}

	public int getKeyLength()
	{
		return 1;
	}


	public byte[] getEncoded()
	{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut = new DEROutputStream(bOut);
		try
		{
			dOut.writeObject(getAsSubjectPublicKeyInfo());
			dOut.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException("IOException while encoding public key");
		}
		return bOut.toByteArray();
	}

}
