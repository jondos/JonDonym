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



import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.mixminion.fec.FECCode;
import anon.mixminion.fec.FECCodeFactory;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.util.ByteArrayUtil;

/**
 * @author Stefan Roenisch
 * *
 */
public class FragmentedMessage extends MessageImplementation {
	static final int 	KEY_LEN = 16,
						OVERHEAD = 0,
						FRAGMENT_HEADER_LEN = 47;
	String[] m_recipient;
	byte[] m_payload;

public FragmentedMessage(String[] recipient, byte[] payload) {
	this.m_payload = payload;
	this.m_recipient = recipient;

}

public byte[][] buildPayload() {

//	When generating plaintext forward
//	fragmented messages, the message generator uses a routing type of
//	"FRAGMENT" (0x0103), an empty routing info, and prepends the
//	following fields to the message body before compressing and
//	fragmenting it:
//  correct: in the Python Code they add it after compression....
//	         RS Routing size    2 octets
//	         RT Routing type    2 octets
//	         RI Routing info    (variable length; RS=Len(RI))
//  correct: python code says RT,RS,RI
//
	
	//	Compress the payload
	m_payload = MixMinionCryptoUtil.compressData(m_payload);
	//Prepend the real Delivery-Information to the message body
	ExitInformation pl_exit_info = MMRDescription.getExitInformation(m_recipient,null);
	pl_exit_info.m_Content = m_recipient[0].getBytes(); 
	byte[] prepayload = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(ExitInformation.TYPE_SMTP, 2),
			ByteArrayUtil.inttobyte(pl_exit_info.m_Content.length, 2),
		    pl_exit_info.m_Content);
	m_payload = ByteArrayUtil.conc(prepayload, m_payload);


	LogHolder.log(LogLevel.DEBUG, LogType.MISC,
				  "[Message] Fragmented, new Compressed Size = " + m_payload.length);
	//Constraint
	if (m_payload.length + SingleBlockMessage.SINGLETON_HEADER_LEN <= 28 * 1024)
	{
		throw new RuntimeException("Fragmented Header nach Neukomprimierung mit Single-Laenge");
	}

	// whiten
	m_payload = whiten(m_payload);


//    Let FRAGMENTS = DIVIDE(M_C, 28KB-OVERHEAD-FRAGMENT_HEADER_LEN)
//    Let ID = Rand(TAG_LEN)
//    Let SZ = Int(32,Len(M_C))
//    For every FRAGMENT_i in FRAGMENTS:
//       Let PAYLOAD_i = a fragment payload containing:
//          Flag 1 | Int(23,i) | Hash(ID | SZ | FRAGMENT_i ) | ID
//             | SZ | FRAGMENT_i
//    return every PAYLOAD_i.
	byte[] [] frags = divideIntoFragments(m_payload);
	byte[] id = MixMinionCryptoUtil.randomArray(20);
	byte[] sz = ByteArrayUtil.inttobyte(m_payload.length, 4);
	byte[][] payloads = new byte[frags.length][28 * 1024];
	for (int i = 0; i < frags.length; i++)
	{
		byte[] actual_fragment = frags[i];
		byte[] flag = new byte[3];
		flag = ByteArrayUtil.inttobyte(8388608+i,3);
		byte[] hash = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(id, sz, actual_fragment));
		payloads[i] = ByteArrayUtil.conc(flag, hash, id, sz, actual_fragment);
	}
	return payloads;
	}

/**
 * Diese Methode fragmentiert eine Nachricht (nach der E2E.txt)
 * @param payload, the message to send
 * @return the fragments of the Message
 */
byte[][] divideIntoFragments(byte[] payload)
{
	int packet_size = 28 * 1024 - FRAGMENT_HEADER_LEN - OVERHEAD;
	double exf = 4.0 / 3.0;

//	System.out.println("   divideIntoFragments");
//	System.out.println("      M   = " + M.length);
//	System.out.println("      PS  = " + PS);
//	System.out.println("      EXF = " + EXF);

	double tmp;

	// Let M_SIZE = CEIL(LEN(M) / PS)
	double payload_packets = Math.ceil(payload.length / (double) packet_size);

	// Let K = Min(16, 2**CEIL(Log2(M_SIZE)))
	tmp = Math.log(payload_packets) / Math.log(2);
	tmp = Math.ceil(tmp);
	tmp = Math.pow(2, tmp);
	int k = (int) Math.min(16, tmp);

	// Let NUM_CHUNKS = CEIL(M_SIZE / K)
	int overall_packets = (int) Math.ceil(payload_packets / (double) k);

	// Let M = M | PRNG(Rand(KEY_LEN), Len(M) - NUM_CHUNKS*PS*K)
	byte[] random = MixMinionCryptoUtil.randomArray(KEY_LEN);
	int len = payload.length - overall_packets * packet_size * k;
	System.out.println(len);

	// begin   this wasn't written in the E2E, but it seems to be right
	len = Math.abs(len);
	// end    

	byte[] padding = MixMinionCryptoUtil.createPRNG(random, len);
	payload = ByteArrayUtil.conc(payload, padding);

	// For i from 1 to NUM_CHUNKS:
	//    Let CHUNK_i = M[(i-1)*PS*K : i*PS*K]
	// End
	byte[][] packets = new byte[overall_packets][packet_size];
	for (int i = 1; i <= overall_packets; i++)
	{
		byte[] b = ByteArrayUtil.copy(payload, (int) ( (i - 1) * packet_size * k), (int) ( /*i **/packet_size * k));
		packets[i - 1] = b;
	}

	// Let N = Ceil(EXF*K)
	int N = (int) Math.ceil(exf * k);

	// For i from 0 to NUM_CHUNKS-1:
	//    For j from 0 to N-1:
	//      FRAGMENTS[i*N+j] = FRAGMENT(CHUNK_i, K, N, j)
	//    End loop
	// End loop

	System.out.println("   N,num " + N + " " + overall_packets);

	byte[][] FRAGMENTS = new byte[overall_packets * N][28 * 1024];
	for (int i = 0; i <= overall_packets - 1; i++)
	{
		for (int j = 0; j <= N - 1; j++)
		{
			FRAGMENTS[i * N + j] = FRAGMENT(packets[i], k, N, j, packet_size);
		}
	}

	return FRAGMENTS;
}

/**
 * soll den von der Nachricht m das i'te von N Paketen zurueckgeben
 */
byte[] FRAGMENT(byte[] M, int K, int N, int I, int PS)
{
	//k = number of source packets to encode
	//n = number of packets to encode to
	//packetsize = 1024;
	int packetsize = PS;

//	System.out.println("   " + M);

//    Random rand = new Random();

	byte[] source = M; //new byte[K*packetsize]; //this is our source file

	//NOTE:
	//The source needs to split into k*packetsize sections
	//So if your file is not of the write size you need to split it into
	//groups.  The final group may be less than k*packetsize, in which case
	//you must pad it until you read k*packetsize.  And send the length of the
	//file so that you know where to cut it once decoded.

//    rand.nextBytes(source);     //this is just so we have something to encode

	byte[] repair = new byte[N * packetsize]; //this will hold the encoded file

	//These buffers allow us to put our data in them
	//they reference a packet length of the file (or at least will once
	//we fill them)
	byte[][] sourceBuffer = new byte[K][];
	byte[][] repairBuffer = new byte[N][];
	int[] srcOffs = new int[sourceBuffer.length];
	int[] repairOffs = new int[repairBuffer.length];
	for (int i = 0; i < sourceBuffer.length; i++)
	{
		sourceBuffer[i] = source;
		srcOffs[i] = i * packetsize;
	}

	for (int i = 0; i < repairBuffer.length; i++)
	{
		repairBuffer[i] = repair;
		repairOffs[i] = i * packetsize;
	}

	//When sending the data you must identify what it's index was.
	//Will be shown and explained later
	int[] repairIndex = new int[N];

	for (int i = 0; i < repairIndex.length; i++)
	{
		repairIndex[i] = i;
	}

	//create our fec code
	FECCode fec = FECCodeFactory.getDefault().createFECCode(K, N);

	//encode the data
	fec.encode(sourceBuffer, srcOffs, repairBuffer, repairOffs, repairIndex, packetsize);
	//encoded data is now contained in the repairBuffer/repair byte array

	return ByteArrayUtil.copy(repairBuffer[I], repairOffs[I], packetsize);
//	return new String(repairBuffer[I], repairOffs[I], packetsize);
	
}

private byte[] whiten(byte[] m) {
	byte[] k_whiten = {0x57,0x48,0x49,0x54,0x45,0x4E};
	byte[] valuetohash = ByteArrayUtil.conc(k_whiten,"WHITEN".getBytes());
	return MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(valuetohash),m);
}

}
