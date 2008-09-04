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

import anon.mixminion.mmrdescription.MMRDescription;
import anon.mixminion.message.MixMinionCryptoUtil;
import anon.crypto.MyRSA;
import anon.crypto.MyRSAPublicKey;
import java.util.Vector;
import org.bouncycastle.crypto.digests.SHA1Digest;
import anon.util.ByteArrayUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * @author Stefan Roenisch
 */
public class Header
{

	//Constants
	private final int HEADER_LEN = 2048;
	//private final int OAEP_OVERHEAD = 42;
	//private final int MIN_SUBHEADER_LEN = 42;
	private final int PK_ENC_LEN = 256;
	private final int PK_OVERHEAD_LEN = 42;
	private final int PK_MAX_DATA_LEN = 214;
	private final int HASH_LEN = 20;
	private final int MIN_SH = 42;
	private byte[] VERSION_MAJOR =
		{
		0x00, 0x03}; //Version Minor, Version Major

	//stuff
	private byte[] m_header;

	/**
	 * CONSTRUCTOR
	 * @param hops
	 * @param recipient
	 */
	public Header(Vector path, Vector secrets, ExitInformation exitInfo)
	{
		m_header = buildHeader(path, secrets, exitInfo);

	}

	/**
	 *
	 * builds a header with the specified hops
	 * @param hops int
	 * @param recipient String
	 * @return Vector with byte[]-Objects
	 */
	private byte[] buildHeader(Vector path, Vector secrets, ExitInformation exitInfo)
	{
//Variables
		//Adresses of the intermediate nodes
		Vector routingInformation = new Vector(); // Vector with the RoutingInformation
		//Public Keys of the intermediate nodes
		Vector publicKey = new Vector(); // Vector with the Public-Keys
		//Secret Keys to be shared with the intermediate Nodes
		//junkKeys
		Vector junkKeys = new Vector(); // Vector with the Junk-Keys
		//Subsecrets
		Vector subSecret = new Vector(); // Subkeys of the Secret Keys
		//Anzahl der intermediate Nodes
		int internodes = path.size();
	//Padding Sizes
		int[] size = new int[internodes + 1];

//Initialisation of the needed data

		//set at index 0 a null
		publicKey.addElement(null);
		routingInformation.addElement(null);
		junkKeys.addElement(null);
		subSecret.addElement(null);

		for (int i = 1; i <= internodes; i++)
		{

			//Headerkeys, k und junkKeys
			MMRDescription mmdescr = (MMRDescription)path.elementAt(i-1);
			publicKey.addElement(mmdescr.getPacketKey());
			junkKeys.addElement( (subKey( (byte[]) secrets.elementAt(i-1), "RANDOM JUNK")));
			subSecret.addElement( (subKey( (byte[]) secrets.elementAt(i-1), "HEADER SECRET KEY")));

			//Routing Information
			RoutingInformation	ri = mmdescr.getRoutingInformation();
			routingInformation.addElement(ri);

		}

		//sizes berechnen
		int totalsize = 0; // Length of all Subheaders
		for (int i = 1; i <= internodes; i++)
		{
			if (i == internodes)
			{
				size[i] = exitInfo.m_Content.length;
			}
			else
			{
				size[i] = ( (RoutingInformation) routingInformation.elementAt(i + 1)).m_Content.length;
			}
			size[i] += MIN_SH + PK_OVERHEAD_LEN;
			//Length of all subheaders
			totalsize += size[i];
		}

//Length of padding needed for the header
		int paddingLen = HEADER_LEN - totalsize;
		if (totalsize > HEADER_LEN)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "[Calculating HEADERSIZE]: Subheaders don't fit into HEADER_LEN ");
		}

		// Calculate the Junk that will be appended during processing.
		// J_i is the junk that node i will append, and node i+1 will see.
//		  J_0 = ""
		//	  for i = 1 .. N
		//	 J_i = J_(i-1) | PRNG(JUNK_KEY_i, SIZE_i)
		// Stream_i = PRNG(K_i, 2048 + SIZE_i)
		// Before we encrypt the junk, we encrypt all the data, and all
		// the initial padding, but not the RSA-encrypted part.
		//    OFFSET = PADDING_LEN + SUM(SIZE_i ... SIZE_N) - 256
		//           = 2048 - SUM(SIZE_1 ... SIZE_N) + SUM(SIZE_i ... SIZE_N)
		//             -256
		//           = 2048-256 - SUM(SIZE_1 ... SIZE_(i-1))
		//           = 2048 - 256 - len(J_{i-1})
		//OFFSET = HEADER_LEN - PK_ENC_LEN - Len(J_(i-1))
		//J_i = J_i ^ Stream_i[OFFSET:Len(J_i)]
		//end
		Vector junkSeen = new Vector();
		//Vector stream = new Vector();
		//at position 0 there is no junk
		byte[] Stream_i = null;
		junkSeen.addElement( ("").getBytes());

		//calculating for the rest
		for (int i = 1; i <= internodes; i++)
		{
			byte[] lastJunk = (byte[]) junkSeen.elementAt(i - 1);
			byte[] J_i = ByteArrayUtil.conc(lastJunk,
											(MixMinionCryptoUtil.createPRNG( (byte[]) junkKeys.elementAt(i),
				size[i])));
			Stream_i = MixMinionCryptoUtil.createPRNG( (byte[]) subSecret.elementAt(i), 2048 + size[i]);
			int offset = HEADER_LEN - PK_ENC_LEN - lastJunk.length;
			junkSeen.addElement(MixMinionCryptoUtil.xor(J_i,
				ByteArrayUtil.copy(Stream_i, offset, J_i.length)));
		}

//We start with the padding.
		Vector header = new Vector(); //Vector for cumulating the subheaders
		header.setSize(internodes + 2);
		// Create the Header, starting with the padding.
		// H_(N+1) = Rand(PADDING_LEN)
		byte[] padding = MixMinionCryptoUtil.randomArray(paddingLen);
		header.setElementAt(padding, internodes + 1);

//Now, we build the subheaders, iterating through the nodes backwards.
		/*
		 for i = N .. 1
		  if i = N then
		 Set RT and RI from R.
		  else
		 Let RT = RT_(i+1), RI = RI_(i+1)
		  endif

		  SH0 = SHS(V, SK_i, Z(HASH_LEN), len(RI), RT, RI)
		  SH_LEN = LEN(SH0)
		  H0 = SH0 | H_(i+1)

		  REST = H0[PK_MAX_DATA_LEN : Len(H0) - PK_MAX_DATA_LEN]

		  EREST = Encrypt(K_i, REST)
		  DIGEST = HASH(EREST | J_(i-1))

		  SH = SHS(V, SK_i, DIGEST, len(RI), RT, RI)
		  UNDERFLOW = Max(PK_MAX_DATA_LEN - SH_LEN, 0)
		  RSA_PART = SH | H0[PK_MAX_DATA_LEN - UNDERFLOW : UNDERFLOW]

		  ESH = PK_ENCRYPT(PK_i, RSA_PART)
		  H_i = ESH | EREST
		  end
		 */
		for (int i = internodes; i >= 1; i--)
		{
			//initial the actual routingInformation and routingType

			ForwardInformation ri;
			if (i == internodes)
			{
				ri = (ForwardInformation) exitInfo;
			}
			else
			{
				ri = (ForwardInformation) routingInformation.elementAt(i + 1);
			}

			//build the Subheader without the digest
			byte[] sh0 = makeSHS(VERSION_MAJOR, (byte[]) secrets.elementAt(i-1),
								 new byte[HASH_LEN],
								 ByteArrayUtil.inttobyte(ri.m_Content.length, 2), ri.m_Type, ri.m_Content);
			int sh_len = sh0.length;
			//concatenate with the last Subheader
			byte[] h0 = ByteArrayUtil.conc(sh0, (byte[]) header.elementAt(i + 1));
			//take the rest...
			byte[] rest = ByteArrayUtil.copy(h0, PK_MAX_DATA_LEN, h0.length - PK_MAX_DATA_LEN);
			//...and encrypt it
			byte[] erest = MixMinionCryptoUtil.Encrypt( (byte[]) subSecret.elementAt(i), rest);
			//digest of the encrypted rest
			byte[] digest = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(erest,
				(byte[]) junkSeen.elementAt(i - 1)));
			//build the subheader with the digest
			byte[] sh = makeSHS(VERSION_MAJOR, (byte[]) secrets.elementAt(i-1), digest,
								ByteArrayUtil.inttobyte(ri.m_Content.length, 2), ri.m_Type, ri.m_Content);
			//calculate the underflow
			int underflow = max(PK_MAX_DATA_LEN - sh_len, 0);
			//take the part needed to encrypt with the publiuc key
			byte[] rsa_part = ByteArrayUtil.conc(sh,
												 ByteArrayUtil.copy(h0, PK_MAX_DATA_LEN - underflow,
				underflow));
			//encrypt it
			byte[] esh = pk_encrypt( (MyRSAPublicKey) publicKey.elementAt(i), rsa_part);
			//adden
			header.setElementAt(ByteArrayUtil.conc(esh, erest), i);

		}
		//last one is the final header

		return (byte[]) header.elementAt(1);
	}

	/**
	 * Gives back the header as byteArray
	 * @return header byte[]
	 */
	public byte[] getAsByteArray()
	{
		return m_header;
	}


//------------------HELPER-Methods---------------------------

	/**
	 * Subkey
	 * @param Secret Key byte[]
	 * @param phrase String
	 * @return the subkey byte[]
	 */
	private byte[] subKey(byte[] secretKey, String phrase)
	{
		return ByteArrayUtil.copy(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(secretKey, phrase.getBytes())),
								  0, 16);
	}

	/**
	 * fixed-size part of the subheader structure
	 * @param version byte[]
	 * @param secretkey byte[]
	 * @param digest byte[]
	 * @param routingsize byte[]
	 * @param routingtype byte[]
	 * @return part byte[]
	 */
	private byte[] makeFSHS(byte[] v, byte[] sk, byte[] d, byte[] rs, short rt)
	{

		return ByteArrayUtil.conc(v, sk, d, rs, ByteArrayUtil.inttobyte(rt, 2));
	}

	/**
	 * entire subheader
	 * @param Version byte[]
	 * @param SecretKey byte[]
	 * @param Digest byte[]
	 * @param RoutingSize byte[]
	 * @param RoutingType byte[]
	 * @param RoutingInformation byte[]
	 * @return entire subheader byte[]
	 */
	private byte[] makeSHS(byte[] V, byte[] SK, byte[] D, byte[] RS, short RT, byte[] RI)
	{

		return ByteArrayUtil.conc(makeFSHS(V, SK, D, RS, RT), RI);
	}

	/**
	 * Compares two int values and returns the larger one
	 * @param first int
	 * @param second int
	 * @return the larger one int
	 */
	private int max(int first, int second)
	{
		if (first < second)
		{
			first = second;
		}
		return first;
	}

	/**
	 * PublicKey Encryption of
	 * @param key MyRSAPublicKey
	 * @param m byte[]
	 * @return the digest byte[]
	 */
	private byte[] pk_encrypt(MyRSAPublicKey key, byte[] m)
	{
		byte[] sp = "He who would make his own liberty secure, must guard even his enemy from oppression.".
			getBytes();
		SHA1Digest digest = new SHA1Digest();
		digest.update(sp, 0, sp.length);
		MyRSA engine = new MyRSA(digest);
		try
		{
			engine.init(key);
			return engine.processBlockOAEP(m, 0, m.length);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, e);
			return null;
		}
	}
}
