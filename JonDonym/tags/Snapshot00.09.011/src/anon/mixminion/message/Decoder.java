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

/**@todo Temporary removed - needs to be rewritten.. */
//import jap.JAPController;
//import jap.JAPModel;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Vector;

import anon.util.Base64;
import anon.util.ByteArrayUtil;

/**
 * @author Stefan Roenisch
 */
public class Decoder {

	//Kostanten
	private final int KEY_LEN = 16;
	private final int MAXHOPS = 20;
	private final int PACKETSIZE = 28 * 1024 - 47;

	private String m_message;
	private String m_password;

	/**
	 * Constructor
	 * @param message
	 */
	public Decoder(String message, String password) {
		this.m_message=message;
		this.m_password = password;
	}

	/**
	 * Method to decode a given payload as String
	 * @return String decodedMessages
	 * @throws IOException
	 */
	public String decode() throws IOException {

		//get all user-secrets
		Vector mykeys = (new Keyring(m_password)).getUserSecrets();

		//needed variables
		byte[] tag = new byte[0];
		String encrypted = "";
		String plaintext = "";
		boolean success = false;
		boolean fragment = false;

		LineNumberReader reader = new LineNumberReader(new StringReader(m_message));
		String aktLine = reader.readLine();

		//extract the base64 encoded payload
		while (!aktLine.startsWith("-----BEGIN TYPE III ANONYMOUS MESSAGE-----"))
		{
			aktLine = reader.readLine();
			if (aktLine.intern() == ".") return null;

		}

		//Test Message-type: encrypted
		aktLine = reader.readLine();
		aktLine = aktLine.substring(14);
		if (!aktLine.equals("encrypted")) {
			//TODO what is with message parts(reassembling)
			return null;
		}

		//extract the tag for example Decoding-handle: ANyMIMSjFk4MBp+KQzzCZhweAyE=
		aktLine = reader.readLine();
		tag = Base64.decode(aktLine.substring(17));

		//extract the encrypted part
		aktLine = reader.readLine(); //skip empty line
		aktLine = reader.readLine(); //first content line
		while (!aktLine.startsWith("-----END TYPE III ANONYMOUS MESSAGE-----"))
		{
			encrypted = encrypted + aktLine + "\n";
			aktLine= reader.readLine();
		}
		//decode in a byte array
		byte[] enc_bytes = Base64.decode(encrypted);

		//try to decrypt
//		e2e-spec says:
//		For all SEC_i:
//		      If H(TAG | SEC_i | "Validate") ends with a zero octet:
//		         K = H(TAG | SEC_i | "Generate")
//		         STREAM = ENC(K, Z(MAX_PATH * KEY_LEN))
//		         Let P_t = P.
//		         For j in 0 ... MAX_PATH-1:
//		            Let P_t = SPRP_Encrypt(STREAM[j * KEY_LEN : KEY_LEN],
//		                                   "PAYLOAD_ENCRYPT",
//		                                   P_t)
//		            If DECODE_PLAINTEXT_PAYLOAD(P_t) is not "Unknown", return it.

		for (int i = 0; i<mykeys.size(); i++) {
			byte[] aktkey = (byte[])mykeys.elementAt(i);
			byte[] tempkey = ByteArrayUtil.conc(tag,aktkey,"Validate".getBytes());
			if (MixMinionCryptoUtil.hash(tempkey)[19] == 0x00) {
				byte[] key = ByteArrayUtil.copy(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(tag,aktkey,"Generate".getBytes())),0,KEY_LEN);
				byte[] stream = MixMinionCryptoUtil.createPRNG(key, KEY_LEN*MAXHOPS);
				byte[] temppayload = enc_bytes;
				for (int j = 0; j < MAXHOPS; j++) {
					byte[] streamkey = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(ByteArrayUtil.copy(stream, j*KEY_LEN,KEY_LEN),
							"PAYLOAD ENCRYPT".getBytes()));
					temppayload = MixMinionCryptoUtil.SPRP_Encrypt(streamkey, temppayload);
					if (testPayload(temppayload) == 1) //we have a plaintext payload
					{
						int l = byteToInt(ByteArrayUtil.copy(temppayload,0,2),0);
						temppayload = ByteArrayUtil.copy(temppayload,22,l);
						//decompress
						temppayload = MixMinionCryptoUtil.decompressData(temppayload);
						plaintext = new String(temppayload);
						success = true;
						break;
					}
					else if (testPayload(temppayload) == 2)
					{
						//we have a fragment
						System.out.println("Fragment");
						fragment = true;
						plaintext = trytoReassemble(temppayload);
						if (plaintext != null) success =true;
					}

				}


			}

		}

		//generate the result
//		Date: Tue, 24 Oct 2006 14:12:48 +0200
//		From: Anonymer Stefan <anostef@biw.de>
//		MIME-Version: 1.0
//		To: Fefan <LosRinos@gmx.de>
//		Subject: mit anhang
//		Content-Type: multipart/mixed;
//		 boundary="------------040506060207010307050100"
//
//		This is a multi-part message in MIME format.
//		--------------040506060207010307050100
//		Content-Type: text/plain; charset=ISO-8859-15
//		Content-Transfer-Encoding: 7bit
//
//		content
//		...
		String erg = "";
		if (!success)
		{
			if (fragment)
			{
				erg = "From: JAP-Decoder\n"+
				"To: local user\n"+
				"Subject: Fragment\n\n"+
				"Fuer die Decodierung der Nachricht werden weitere Fragmente benoetigt." +"\n\n";
				plaintext = erg;
			}
			else
			{
				plaintext = "From: JAP-Decoder\n"+
				"To: local user\n"+
				"Subject: Fehler\n\n"+
				"Leider konnte nichts decodiert werden.\n";
			}

		}
		else
		{

			erg = "From: JAP-Decoder\n";
			reader = new LineNumberReader(new StringReader(plaintext));
			aktLine = reader.readLine();

			if (aktLine.startsWith("MIME")) {
				erg+=aktLine +"\n";
				aktLine = reader.readLine();
			}
			erg += "To: local user\n";
			aktLine = "Subject: " + aktLine.substring(7); //Titel: ...
			while (aktLine != null) {
				erg += aktLine + "\n";
				aktLine = reader.readLine();
			}
			plaintext =  erg;

		}
		return  plaintext;
	}

	/**
	 * Tests a given payload for being a plaintext one
	 * @param payload
	 * @return true if a message, false otherwise
	 */
	private int testPayload(byte[] payload) {


//		e2e spec says:
//		If the first bit of P[0] is 0:
//		      If P[2:HASH_LEN] = Hash(P[2+HASH_LEN:Len(P)-2-HASH_LEN]):
//		         SZ = P[0:2]
//		         Return "Singleton", P[2+HASH_LEN : SZ]
		byte[] hash1 = ByteArrayUtil.copy(payload,2,20);
		byte[] hash2 = MixMinionCryptoUtil.hash(ByteArrayUtil.copy(payload,22,payload.length-22));

		int l = byteToInt(ByteArrayUtil.copy(payload,0,2),0);
		if (ByteArrayUtil.equal(hash1,hash2))
		{
			return 1;
		}

//		      Otherwise,
//		         Return "Unknown" -->we test if fragment
//		is it a fragment?-->give it to the reassembler
//		byte[] actual_fragment = frags[i];
//		byte[] flag = new byte[3];
//		flag = ByteArrayUtil.inttobyte(8388608+i,3);
//		byte[] hash = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(id, sz, actual_fragment));
//		payloads[i] = ByteArrayUtil.conc(flag, hash, id, sz, actual_fragment);
		hash1 = MixMinionCryptoUtil.hash(ByteArrayUtil.copy(payload,23,payload.length-23));
		hash2 = ByteArrayUtil.copy(payload,3,20);
		if (ByteArrayUtil.equal(hash1,hash2))
		{
			return 2;
		}
		return 0;

	}

	/**
	 * try's to reassemble the message the fragment belongs to and returns either null
	 * or the reassembled message
	 * @param fragment
	 * @return
	 */
	private String trytoReassemble(byte[] fragment) {

		String erg = null;
		//fetch vector with fragmentcontainers
		/**@todo Temporary removed - needs to be rewritten.. */
		//Vector fragments = JAPModel.getMixminionFragments();
		Vector fragments = null;
		if ( fragments == null)
		{
			fragments = new Vector();
		}

		//identify the fragment
		byte[] id = ByteArrayUtil.copy(fragment,23,20);
		int index = byteToInt(ByteArrayUtil.copy(fragment,1,2),0);
		double messagesize = byteToInt(ByteArrayUtil.copy(fragment, 43, 4),0);
		System.out.println("MessageSize:" + messagesize + " index: " +index);

		//remove fragmentheader
		fragment = ByteArrayUtil.copy(fragment,47,28*1024-47);

		//look for fragment container
		FragmentContainer myfc = null;
		int containerindex = -1;

		for (int i=0; i<fragments.size(); i++)
		{
			FragmentContainer fc = (FragmentContainer) fragments.elementAt(i);
			if (ByteArrayUtil.equal(id, fc.getID()))
			{
				myfc = fc;
				containerindex = i;
				break;
			}
		}

		if (containerindex == -1)
		{
			int payload_packets = (int) Math.ceil(messagesize / (double) PACKETSIZE);

			System.out.println("Numberof: " + payload_packets);
			myfc = new FragmentContainer(id, payload_packets);
		}

		if (myfc.addFragment(fragment, index))
		{
			//reassemble
			byte[] payload = myfc.reassembleMessage();
			//remove padding
			payload = ByteArrayUtil.copy(payload, 0, (int)messagesize);
			//unwhiten
			payload = unwhiten(payload);
			//remove header
			int rs = byteToInt(ByteArrayUtil.copy(payload,3,1),0);
			payload = ByteArrayUtil.copy(payload, 4+rs, payload.length-4-rs);
			//decompress
			payload = MixMinionCryptoUtil.decompressData(payload);
			erg = new String(payload);
		}

		if (containerindex == -1)
		{
			fragments.addElement(myfc);
		}
		else
		{
			fragments.setElementAt(myfc,containerindex);
		}
		/**@todo Temporary removed - needs to be rewritten.. */
		//JAPController.setMixminionFragments(fragments);
		return erg;


	}

	/**
	 * Calculates the int value of a given ByteArray
	 * @param b
	 * @param offset
	 * @return int value of the bytearray
	 */
	private int byteToInt(byte[] b, int offset) {
		int value = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (b.length - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
	}

	/**
	 * Unwhites a ByteArray in the fashion of mixminion
	 * @param m
	 * @return
	 */
	private byte[] unwhiten(byte[] m) {
		byte[] k_whiten = {0x57,0x48,0x49,0x54,0x45,0x4E};
		byte[] valuetohash = ByteArrayUtil.conc(k_whiten,"WHITEN".getBytes());
		return MixMinionCryptoUtil.SPRP_Decrypt(MixMinionCryptoUtil.hash(valuetohash),m);
	}
}
