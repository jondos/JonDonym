/*
Copyright (c) 2004, The JAP-Team
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

package anon.mixminion.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bouncycastle.crypto.digests.SHA1Digest;

import anon.crypto.MyAES;
import anon.util.ByteArrayUtil;
import anon.util.ZLibTools;

/**
 * Some Helping Methods for the MixMinion
 * @author Stefan Roenisch and Jens Kempe
 *
 */
public class MixMinionCryptoUtil
{

	/**
	 * Creates an octet-array fills with random Bytes
	 * @param len int
	 * @return the array byte[]
	 */
	public static byte[] randomArray(int len)
	{
		byte[] ret = new byte[len];
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(ret);
		return ret;
	}

	/**
	 * xor of two byte[]
	 * @param one byte[]
	 * @param two byte[]
	 * @return the xor, if not the same length: null
	 */
	static byte[] xor(byte[] one, byte[] two)
	{
		if (one.length != two.length)
		{
			return null;
		}
		else
		{
			byte[] result = new byte[one.length];
			for (int i = 0; i < one.length; i++)
			{
				result[i] = (byte) (one[i] ^ two[i]);
			}
			return result;
		}
	}

	/**
	 * SHA1Digest of x
	 * @param x byte[]
	 * @return the digest byte[]
	 */
	public static byte[] hash(byte[] x)
	{
		SHA1Digest myDigest = new SHA1Digest();
		myDigest.update(x, 0, x.length);
		byte[] ret = new byte[myDigest.getDigestSize()];
		myDigest.doFinal(ret, 0);
		return ret;
	}
	
	/**
	 * 
	 * @param v
	 * @param offset
	 * @param len
	 * @return
	 */
	public static Vector subVector(Vector v, int offset, int len) {
		Vector erg = new Vector();
		for (int i=offset; i < (offset+len); i++) {
			erg.addElement(v.elementAt(i));
		}
		return erg;
	}


	/**
	 * Die Encrypt-Funktion nach der MixMinion-Spec
	 */
	public static byte[] Encrypt(byte[] K, byte[] M)
	{
//      - Encrypt(K, M) = M ^ PRNG(K,Len(M)) - The encryption of an octet array
//      M with our stream cipher, using key K.  (Encrypt(Encrypt(M)) = M.)
		return xor(M, createPRNG(K, M.length));
	}

	/**
	 * Creates a octet-array using Cryptographic Stream generator
	 * @param key byte[]
	 * @param len int
	 * @return the array byte[]
	 */
	static byte[] createPRNG(byte[] key, int len)
	{
		MyAES engine = new MyAES();
		byte[] data = new byte[len];
		byte[] counter = new byte[16];
		byte[] counterOut = new byte[16];
		try
		{
			engine.init(true, key);
			int datapos = 0;
			while (len >= 16)
			{
				engine.processBlockECB(counter, counterOut);
				System.arraycopy(counterOut, 0, data, datapos, 16);
				int carry = 1;
				for (int i = counter.length - 1; i >= 0; i--)
				{
					int x = (counter[i] & 0xff) + carry;
					if (x > 0xff)
					{
						carry = 1;
					}
					else
					{
						carry = 0;
					}

					counter[i] = (byte) x;
				}
				len -= 16;
				datapos += 16;
			}
			if (len > 0)
			{
				engine.processBlockECB(counter, counterOut);
				System.arraycopy(counterOut, 0, data, datapos, len);
			}
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		return data;
	}

	/**
	 * Die SPRP_Encrypt-Funktion nach der MixMinion-Spec
	 * @param k
	 * @param M
	 * @return
	 */
	public static byte[] SPRP_Encrypt(byte[] K, byte[] M)
	{
//    3.1.1.3. Super-pseudorandom permutation
//
//    To encrypt an octet array so that any change in the encrypted
//    value will make the decryption look like random bits, we use an
//    instance of the LIONESS SPRP, with SHA-1 for a hash and the
//    stream described in 3.1.1.2 above.
//
//    Thus, in the notation described below, we encrypt a message M with
//    a key K as follows:
//        K1 = K
//        K2 = K ^ [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01]
//        K3 = K ^ [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02]
//        K4 = K ^ [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03]
//        L := M[0:20]
//        R := M[20:Len(M)-20]
//        R := Encrypt( Hash(K1 | L | K1)[0:16], R )
//        L := L ^ Hash(K2 | R | K2)
//        R := Encrypt( Hash(K3 | L | K3)[0:16], R )
//        L := L ^ Hash(K4 | R | K4)
//        SPRP_Encrypt(K, M) = L | R

		byte[] h1 = new byte[20];

		byte[] K1 = K;
		h1[19] = 1;
		byte[] K2 = xor(K, h1);
		h1[19] = 2;
		byte[] K3 = xor(K, h1);
		h1[19] = 3;
		byte[] K4 = xor(K, h1);
		byte[] L = ByteArrayUtil.copy(M, 0, 20);
		byte[] R = ByteArrayUtil.copy(M, 20, M.length - 20);
		R = Encrypt(ByteArrayUtil.copy(hash(ByteArrayUtil.conc(K1, L, K1)), 0, 16), R);
		L = xor(L, hash(ByteArrayUtil.conc(K2, R, K2)));
		R = Encrypt(ByteArrayUtil.copy(hash(ByteArrayUtil.conc(K3, L, K3)), 0, 16), R);
		L = xor(L, hash(ByteArrayUtil.conc(K4, R, K4)));

		return ByteArrayUtil.conc(L, R);
	}

	/**
	 * Die SPRP_Encrypt-Funktion nach der MixMinion-Spec
	 * @param k
	 * @param M
	 * @return
	 */
	public static byte[] SPRP_Decrypt(byte[] K, byte[] M)
	{
		/*We decrypt a message M with a key K as follows:
			 K1 = K
			 K2 = K ^ [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01]
			 K3 = K ^ [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02]
			 K4 = K ^ [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03]
			 L := M[0:20]
			 R := M[20:Len(M)-20]
			 L := L ^ Hash(K4 | R | K4)
			 R := Encrypt( Hash(K3 | L | K3)[0:16], R )
			 L := L ^ Hash(K2 | R | K2)
			 R := Encrypt( Hash(K1 | L | K1)[0:16], R )
			 SPRP_Decrypt(K, M) = L | R
*/

		byte[] h1 = new byte[20];

		byte[] K1 = K;
		h1[19] = 1;
		byte[] K2 = xor(K, h1);
		h1[19] = 2;
		byte[] K3 = xor(K, h1);
		h1[19] = 3;
		byte[] K4 = xor(K, h1);
		byte[] L = ByteArrayUtil.copy(M, 0, 20);
		byte[] R = ByteArrayUtil.copy(M, 20, M.length - 20);
		L = xor(L, hash(ByteArrayUtil.conc(K4, R, K4)));
		R = Encrypt(ByteArrayUtil.copy(hash(ByteArrayUtil.conc(K3, L, K3)), 0, 16), R);
		L = xor(L, hash(ByteArrayUtil.conc(K2, R, K2)));
		R = Encrypt(ByteArrayUtil.copy(hash(ByteArrayUtil.conc(K1, L, K1)), 0, 16), R);

		return ByteArrayUtil.conc(L, R);
	}
	/**
	 * this Method is for compressing some Data, with the ZLIB-Standard
	 */
	static byte[] compressData(byte[] message)
	{
		byte[] compress = ZLibTools.compress(message);
		if (compress[0] != 0x78 || compress[1] + 256 != 0xDA)
		{
			throw new RuntimeException("The Compressed Messege didn't start with 0x78DA");
		}

		return compress;
	}

	static byte[] decompressData(byte[] message) 
	{
		return ZLibTools.decompress(message);
	}
	/**
	 * this Method is for compressing some Data
	 */
	private static byte[] ZIPcompressData(byte[] message)
	{
		byte[] compress = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zip;

		try
		{
			zip = new ZipOutputStream(baos);
			zip.setLevel(9);
			zip.setMethod(ZipOutputStream.DEFLATED);
			ZipEntry e = new ZipEntry("MixMinionZip"); //Name darf anscheind nicht leer sein
			zip.putNextEntry(e);
			zip.write(message);
			zip.flush();
			zip.close();
			baos.flush();
			baos.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		compress = baos.toByteArray();

		// only for Testing, if the Compression was ok.
		if (!ByteArrayUtil.equal(message, ZIPextractData(compress)))
		{
			throw new RuntimeException("Something with Compression/Decompression was wrong!");
		}

		return compress;
	}

	/**
	 * this Method is for extracting some Data
	 */
	private static byte[] ZIPextractData(byte[] payload)
	{
		byte[] message = null;

		ByteArrayInputStream bais = new ByteArrayInputStream(payload);
		ZipInputStream zip;
		try
		{
			zip = new ZipInputStream(bais);
			ZipEntry e = zip.getNextEntry(); //sonst Null-Pointer-Exception

			boolean go = true;
			int size = -1;
			while (go)
			{
				size++;
				int c = zip.read();
				byte[] b =
					{
					(byte) c};
				if (c != -1)
				{
					message = ByteArrayUtil.conc(message, b);
				}
				else
				{
					go = false;
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return message;
	}

	/**
	 * this Method is for compressing some Data
	 */
	private static byte[] GZIPcompressData(byte[] message)
	{
		byte[] payload = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzip;
		try
		{
			gzip = new GZIPOutputStream(baos);
			gzip.write(message);
			gzip.flush();
			gzip.close();
			baos.flush();
			baos.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		payload = baos.toByteArray();
		return payload;
	}

	/**
	 * this Method is for extracting some Data
	 */
	private static byte[] GZIPextractData(byte[] payload)
	{
		byte[] message = null;

		ByteArrayInputStream bais = new ByteArrayInputStream(payload);
		GZIPInputStream gzip;
		try
		{
			gzip = new GZIPInputStream(bais);

			boolean go = true;
			int size = -1;
			while (go)
			{
				size++;
				int c = gzip.read();
				byte[] b =
					{
					(byte) c};
				if (c != -1)
				{
					message = ByteArrayUtil.conc(message, b);
				}
				else
				{
					go = false;
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return message;
	}
}
