package anon.crypto.test;

import java.security.SecureRandom;

import anon.crypto.AsymmetricCryptoKeyPair;

public class DummyTestKeyPairGenerator extends AbstractTestKeyPairGenerator
{
	public DummyTestKeyPairGenerator(SecureRandom a_random)
	{
		super(a_random);
	}

	public AsymmetricCryptoKeyPair createKeyPair()
	{
		return DummyKeyPair.getInstance(getRandom());
	}
}
