package anon.crypto.test;

import java.security.SecureRandom;

import anon.crypto.AsymmetricCryptoKeyPair;
import anon.crypto.ECKeyPair;

/**
 * Instances of this class create EC cipher key pairs. It is for testing
 * purposes only.
 * @author Robert Hirschberger
 */
public class ECTestKeyPairGenerator extends AbstractTestKeyPairGenerator
{
	public ECTestKeyPairGenerator(SecureRandom a_random)
	{
		super(a_random);
	}
	
	public AsymmetricCryptoKeyPair createKeyPair()
	{
		return ECKeyPair.getInstance(getRandom());
	}
}
