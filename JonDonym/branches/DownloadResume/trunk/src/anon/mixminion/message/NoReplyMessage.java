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

import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.mixminion.mmrdescription.MMRList;
import anon.util.ByteArrayUtil;


/**
 * @author Stefan Roenisch
 *
 */
public class NoReplyMessage extends ReplyImplementation {

	private byte[][] m_message_parts; //28kb prepared payload(s)
	private int m_hops;
	private String[] m_recipient;
	private Vector m_start_server; //for every ready to send message the first server
	private MMRList m_mmrlist;

	/**
	 * Constructor
	 * build a no-Reply Message with two Headers, if a replyblock is specified its used as header2
	 * @param message_parts
	 * @param hops
	 * @param recipient
	 */
	public NoReplyMessage(byte[][] message_parts, int hops, String[] recipient, MMRList mmrlist)
	{
		this.m_message_parts = message_parts;
		this.m_hops = hops;
		this.m_recipient = recipient;
		this.m_start_server = new Vector();
		this.m_mmrlist = mmrlist;
	}

	/**
	 * Builds for every element in m_message_parts a ready to send message
	 * @return Vector with ready to send 32k blocks
	 */
	public Vector buildMessage()
	{

	Vector ready_to_send_Messages = new Vector(); // returned
	boolean isfragmented = m_message_parts.length > 1; //multipart?

	Vector paths = new Vector(); //Vector with one path for every final packet

	//prepare the hops
	int firstleg_hops = m_hops/2;
	int secondleg_hops = m_hops - firstleg_hops;

	//for Single Block Message
	if (!isfragmented)
	{
		Vector tv = m_mmrlist.getByRandomWithExit(m_hops);
		paths.addElement(tv);
	}

	// for every block the last server must be the same to allow reassembling
	else
	{
		paths = m_mmrlist.getByRandomWithFrag(m_hops, m_message_parts.length);
	}

	// for every part build a 32k packet
	for (int i_frag = 0; i_frag < m_message_parts.length; i_frag++)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "[Message] make Header to Fragment_" + i_frag);
		// build paths
		Vector wholepath = (Vector) paths.elementAt(i_frag);
		Vector path1 = new Vector();
		Vector path2 = new Vector();

		path1 = MixMinionCryptoUtil.subVector(wholepath, 0, firstleg_hops);
		path2 = MixMinionCryptoUtil.subVector(wholepath, firstleg_hops, secondleg_hops);


		m_start_server.addElement((MMRDescription)path1.elementAt(0));

		//build secrets

		Vector secrets1 = new Vector();
		Vector secrets2 = new Vector();
		for (int i = 0; i < (m_hops / 2); i++)
		{
			secrets1.addElement(MixMinionCryptoUtil.randomArray(16));
			secrets2.addElement(MixMinionCryptoUtil.randomArray(16));
		}
		if (secrets2.size() < secondleg_hops) secrets2.addElement(MixMinionCryptoUtil.randomArray(16));

		//definate ExitInfos and Crossoverpoint
		ExitInformation exit2 = new ExitInformation();
		if (isfragmented)
		{
			exit2.m_Type = ExitInformation.TYPE_FRAGMENTED;
			exit2.m_Content = new byte[0];
		}
		else
		{
			exit2 = MMRDescription.getExitInformation(m_recipient,null);
		}

		ExitInformation exit1 = new ExitInformation();
		exit1.m_Type = RoutingInformation.TYPE_SWAP_FORWARD_TO_HOST;
		exit1.m_Content = ( (ForwardInformation) ( (MMRDescription) path2.elementAt(0)).
						   getRoutingInformation()).m_Content;
		//Header erzeugen
		Header header1 = new Header(path1, secrets1, exit1);
		Header header2 = new Header(path2, secrets2, exit2);

		byte[] headerbytes_1 = header1.getAsByteArray();
		byte[] headerbytes_2 = header2.getAsByteArray();
		byte[] payload = m_message_parts[i_frag];
		// M is the MixMinionPacket Type III
		byte[] final_message = null;

		/**Phase 1 - if H2 is not a reply block**/
		// for i = N .. 1
		//   P = SPRP_Encrypt(SK2_i, "PAYLOAD ENCRYPT", P)
		// end
		for (int i = secrets2.size() - 1; i >= 0; i--)
		{
			byte[] secretKey = (byte[]) secrets2.elementAt(i);
			byte[] key = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(secretKey, "PAYLOAD ENCRYPT".getBytes()));
			payload = MixMinionCryptoUtil.SPRP_Encrypt(key, payload);
		}

		/** Phase 2: SPRP verschluesseln **/
		// H2 = SPRP_Encrypt(SHA1(P), "HIDE HEADER", H2)
		// P = SPRP_Encrypt(SHA1(H2), "HIDE PAYLOAD", P)
		// for i = N .. 1
		//   H2 = SPRP_Encrypt(SK1_i, "HEADER ENCRYPT",H2)
		//   P = SPRP_Encrypt(SK1_i, "PAYLOAD ENCRYPT",P)
		// end
		// M = H1 | H2 | P
		headerbytes_2 = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(
			MixMinionCryptoUtil.hash(payload), "HIDE HEADER".getBytes())), headerbytes_2);
		payload = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(
			MixMinionCryptoUtil.hash(headerbytes_2), "HIDE PAYLOAD".getBytes())), payload);
		for (int i = secrets1.size() - 1; i >= 0; i--)
		{
			byte[] SK1_i = (byte[]) secrets1.elementAt(i);
			headerbytes_2 = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK1_i,
				"HEADER ENCRYPT".getBytes())), headerbytes_2);
			payload = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK1_i,
				"PAYLOAD ENCRYPT".getBytes())), payload);
		}
		final_message = ByteArrayUtil.conc(headerbytes_1, headerbytes_2, payload);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "[Message] the Messagesize = " + final_message.length + " Bytes");
		ready_to_send_Messages.addElement(final_message);
	}
	return ready_to_send_Messages;
	}

	public Vector getStartServers() {
		return m_start_server;
	}

}
