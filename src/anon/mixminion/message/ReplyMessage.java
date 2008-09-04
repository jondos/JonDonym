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
 *
 */
public class ReplyMessage extends ReplyImplementation{

	private MMRList m_mmrlist;
	private int m_hops;
	private byte[][] m_message_parts;
	private Vector m_replyblocks; //parsed replyblock from the payload
	private Vector m_start_server; //for every ready to send message the first server

	/**
	 * Concrete Implementation of ReplyImplementation
	 * builds a repliable message with an ReplyBlock as second Header
	 * Constructor
	 * @param from
	 * @param message_parts
	 * @param hops
	 * @param recipient
	 * @param replyblock
	 */
	public ReplyMessage(byte[][] message_parts, int hops, Vector replyblocks, MMRList mmrlist) {

		this.m_mmrlist = mmrlist;
		this.m_hops = hops;
		this.m_message_parts = message_parts;
		this.m_replyblocks = replyblocks;
		this.m_start_server = new Vector();

	}

	public Vector buildMessage() {

		Vector ready_to_send_Messages = new Vector(); // returned
		boolean isfragmented = m_message_parts.length > 1; //multipart?

		//FIXME fragmented reply-messages are to decode local; not implemented yet

		if (isfragmented) {

			System.out.println("Reply und Fragmente; Decodierung wird noch nicht moeglich sein...");
			//return null;
		}


		Vector paths = new Vector();
		//initialise paths in the needed way
		//We need for every leg 1 a path
		//for Singleton
		if (!isfragmented)
			{
				Vector tv = m_mmrlist.getByRandomWithExit(m_hops);
				paths.addElement(tv);
			}

			// every packet of the fragmented Message must have the same end-server
			else
			{
				paths = m_mmrlist.getByRandomWithFrag(m_hops, m_message_parts.length);
			}

		//enough replyblocks?
		if (m_replyblocks.size() < m_message_parts.length)
		{
			//no?
			return null;
		}

		//okay build the parts
		for (int i_frag = 0; i_frag < m_message_parts.length; i_frag++)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[Message] make Header to Fragment_" + i_frag);

			byte[] headerbytes_1 = null;;
			byte[] headerbytes_2 = ((ReplyBlock)m_replyblocks.elementAt(i_frag)).getHeaderBytes();

			Vector secrets = new Vector(); //Secrets in Header1
			//Build first Header
			Vector path = (Vector) paths.elementAt(i_frag);
			m_start_server.addElement((MMRDescription)path.elementAt(0));

			//build secrets for Header 1
			for (int i = 0; i < m_hops; i++)
				{
					secrets.addElement(MixMinionCryptoUtil.randomArray(16));
				}

			//define ExitInfos and Crossoverpoint
			ExitInformation exit1 = new ExitInformation();
			RoutingInformation ri1 = ((ReplyBlock)m_replyblocks.elementAt(i_frag)).getRouting();
			exit1.m_Type = ri1.m_Type;
			exit1.m_Content = ri1.m_Content;

			//build Header 1
			Header header1 = new Header(path, secrets, exit1);
			headerbytes_1 = header1.getAsByteArray();

			//okay, all stuff prepared. Now build the packet.
			byte[] payload = m_message_parts[i_frag];
			// message is the MixMinionPacket Type III
			byte[] packet = null;

			/** Phase 1 - if H2 is a reply block **/
				// Let K_surb = the end to end encryption key in H2
				// P = SPRP_ Decrypt(K_SURB, "PAYLOAD ENCRYPT", P)
				byte[] key = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(((ReplyBlock)m_replyblocks.elementAt(i_frag)).getSharedSecret(),"PAYLOAD ENCRYPT".getBytes()));
				payload = MixMinionCryptoUtil.SPRP_Decrypt(key,payload);

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
					for (int i = secrets.size() - 1; i >= 0; i--)
					{
						byte[] SK1_i = (byte[]) secrets.elementAt(i);
						headerbytes_2 = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK1_i,
							"HEADER ENCRYPT".getBytes())), headerbytes_2);
						payload = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK1_i,
							"PAYLOAD ENCRYPT".getBytes())), payload);

					}
					packet = ByteArrayUtil.conc(headerbytes_1, headerbytes_2, payload);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "[Message] the Messagesize = " + packet.length + " Bytes");
					ready_to_send_Messages.addElement(packet);

		}

		return ready_to_send_Messages;
	}

	/**
	 * returns a Vector with the MMRDescriptions of the first Server of every ready_to send_message
	 */
	public Vector getStartServers()
	{
		return m_start_server;
	}
}
