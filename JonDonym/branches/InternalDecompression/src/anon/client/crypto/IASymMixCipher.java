package anon.client.crypto;

import java.math.BigInteger;

import org.w3c.dom.Element;

import anon.crypto.MyRSAPublicKey;

public interface IASymMixCipher
	{

		public int encrypt(byte[] from, int ifrom, byte[] to, int ito);

		public int getOutputBlockSize();
		public int getInputBlockSize();

		public int getPaddingSize();

		public int setPublicKey(BigInteger modulus, BigInteger exponent);

		public int setPublicKey(Element xmlKey);

		public MyRSAPublicKey getPublicKey();

	}