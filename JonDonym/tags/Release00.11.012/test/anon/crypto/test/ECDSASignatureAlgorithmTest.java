package anon.crypto.test;

import java.security.SecureRandom;

import anon.crypto.AsymmetricCryptoKeyPair;
import anon.crypto.ECKeyPair;
import anon.crypto.MyECDSASignature;

/**
 * These are the tests for the ECDSA signature algorithm.
 * @author Robert Hirschberger
 */
public class ECDSASignatureAlgorithmTest extends AbstractSignatureAlgorithmTest
{
	/**
	 * Creates a new test case.
	 * @param a_name the name of the test case
	 */
	public ECDSASignatureAlgorithmTest(String a_name)
	{
		super(a_name);
	}
	
	/**
	 * This method initialises the keys and the signature algorithm.
	 */
	protected void setUp()
	{
		SecureRandom random = new SecureRandom();
		MyECDSASignature algorithm = new MyECDSASignature();
		AsymmetricCryptoKeyPair keyPair;

		random.setSeed(932365628);

		// initialise the algorithm
		setSignatureAlgorithm(algorithm);

		// initialise the keys
		for (int i = 0; i < NUMBER_OF_KEYS; i++)
		{
			keyPair = ECKeyPair.getInstance(random);
			getPrivateKeys()[i] = keyPair.getPrivate();
			getPublicKeys()[i] = keyPair.getPublic();
		}
	}
}
