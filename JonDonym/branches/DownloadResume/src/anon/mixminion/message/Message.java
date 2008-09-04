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

/*sk13 removed because not allowed in anon.* */
//import jap.JAPController;
//import jap.JAPModel;

import java.io.IOException;
import java.util.Vector;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.mixminion.EMail;
import anon.mixminion.FirstMMRConnection;
import anon.mixminion.Mixminion;
import anon.mixminion.mmrdescription.InfoServiceMMRListFetcher;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.mixminion.mmrdescription.MMRList;
import anon.mixminion.mmrdescription.PlainMMRListFetcher;


/**
 * @author Jens Kempe, Stefan Roenisch
 */
public class Message
{
	private EMail m_email = null;
//	private String m_payload = null; // "raw"-Payload from the mail
//	private String[] m_recipient = null; // all recipients; if there is a replyblock in use: dont care
	private int m_hops = 0;
	private String m_address; // E-Mail address of the sender; needed to build a Replyblock(this one is appended to the payload to allow a reply)
	private String m_decoded = null;
	private String m_keyringpassword;
	private int m_rbs;


	// Constants (from original Python-Implementation)
	int MAX_FRAGMENTS_PER_CHUNK = 16;
	double EXP_FACTOR = 1.3333333333333333;
	// keyringpassword


/**
 * Constructor
 */
	public Message(EMail email, int hops, String myadress, String password, int rbs)
	{
		this.m_email = email;
		this.m_hops = hops;
		this.m_address = myadress; //=Exit-Address in the ReplyBlock, which allows a recipient to reply
		this.m_keyringpassword = password;
		this.m_rbs = rbs; //=how many replyblocks to allow reply to me

	}

	public boolean send()
	{
		return encodeMessage();
	}

	boolean encodeMessage()
//  1. Determine the type of the message: plaintext forward, or reply.

				{
//		0. 	Check whether the user only wants to decode a payload, yes: decode it no: 0.1
//		0.1 Look if the Sender has specified (a) ReplyBlock(s) to send to
//		0.2 add m_rbs replyblocks to the payload
//		1. Compress the message
//		2. Choose whether SingleBlock or Fragmented Message Imlementation and build the payload(s)
//		3. Choose whether in Reply or normal Forward Message
//		4. Build the packets.
//      5. Deliver each packet

		//0.look if the user only wants a decoded representation
		String plaintext = null;
		if (m_email.getType().equals("ENC"))
		{
			Decoder decoder = new Decoder(m_email.getPayload(), m_keyringpassword);
			Vector decoded = new Vector();

			try
			{
				plaintext = decoder.decode();

			} catch (IOException e2)
				{
				System.out.println("Decodier-Exception...");
				}

			decoded.addElement(plaintext);
			/**@todo Temporary removed - needs to be rewritten.. */
			//JAPController.setMixminionMessages(decoded);
			m_decoded=(String)decoded.elementAt(0);
			return false;
		}


		//There is a message to send....
		//Needed Variables
		byte[][] message_parts = null; // Array with finalized payload parts, each 28kb
		boolean returnValue = true; // all parts sended correct?

		MessageImplementation payload_imp; //BridgeVariable, if single or multipart implemetation
		ReplyImplementation message_imp; //BridgeVariable, if repliable or not

		//0.1 replyblocks?
		Vector replyblocks = null;
		if (m_email.getType().equals("RPL"))
		{
			replyblocks = m_email.getReplyBlocks();
		}

		//prepare mmrlist
		//already fetched?
		MMRList mmrlist = null;
			/**@todo Temporary removed - needs to be rewritten.. */
			///MMRList mmrlist = JAPModel.getMixminionMMRlist();
		//if no get it
		if (mmrlist == null)
		{
			mmrlist  = new MMRList(new InfoServiceMMRListFetcher()); //try to get it from the infoservice
			if (!mmrlist.updateList()) //if this fails, try to get it directly from the server
			{
				mmrlist = new MMRList(new PlainMMRListFetcher());
				if (!mmrlist.updateList())  //if nothing works return false
				{
					return false;
				}
				System.out.println("Groesse: " +mmrlist.size());
			}
			/**@todo Temporary removed - needs to be rewritten.. */
			//JAPController.setMixminionMMRList(mmrlist);
		}


		//0.2 build m_rbs replyblocks and add them to the payload...
		for (int i = 0; i < m_rbs; i++)
		{
			Vector path_to_me = mmrlist.getByRandomWithExit(m_hops);
			byte[] user_secret = new Keyring(m_keyringpassword).getNewSecret();
			ReplyBlock reply_to_me = new ReplyBlock(m_address, path_to_me, user_secret);
			reply_to_me.buildBlock();
			m_email.addRBtoPayload(reply_to_me.getReplyBlockasString());
		}

		//1. Compress  "raw" payload.
		byte[] compressed_payload = MixMinionCryptoUtil.compressData(m_email.getPayload().getBytes());
		LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "[Message] Compressed Size = " + compressed_payload.length);

		//2. choose which concrete Implementation for the payload to use
		//Test if the payload fix to one 28k block or if it must be fragmented
		if (compressed_payload.length + SingleBlockMessage.SINGLETON_HEADER_LEN <= 28 * 1024)

		{
			payload_imp = new SingleBlockMessage(compressed_payload);
		}

		else
		{
			System.out.println("fragmente!");
			payload_imp = new FragmentedMessage(m_email.getReceiver(), m_email.getPayload().getBytes());

		}

		//build payload
		message_parts = payload_imp.buildPayload();

		//constraint
		if (message_parts.length == 0)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
					  "[Message] Compression failure--> 0 packets ");
			return false;
		}

		//3. Choose wether to send with in Reply to an ReplyBlock or without, note that the replyblock in the constructor
		//contains the address of the recipient
		//for every element in message_parts build a 32k message
		if (m_email.getType().equals("RPL"))
		{
			message_imp = new ReplyMessage(message_parts, m_hops, replyblocks, mmrlist);
		}

		else {
			message_imp = new NoReplyMessage(message_parts, m_hops, m_email.getReceiver(), mmrlist);
		}


		//4. build the packets
		Vector packets = message_imp.buildMessage();
		//5. send each packet
		Vector firstservers = message_imp.getStartServers();

		for(int i = 0; i < packets.size(); i++)
		{
			returnValue = returnValue && sendToMixMinionServer((byte[])packets.elementAt(i), (MMRDescription) firstservers.elementAt(i));
		}
		return returnValue;
	}

	/**
	 * Send a message to the specified MMR
	 * @param message
	 * @param description
	 * @return true if sended successfully
	 */
	private boolean sendToMixMinionServer(byte[] message, MMRDescription description)
	{
		boolean returnValue = false;

		try
		{
			Mixminion mixminion = Mixminion.getInstance();
			FirstMMRConnection fMMRcon = new FirstMMRConnection(description, mixminion);

			System.out.println("   connecting...");
			fMMRcon.connect();

			System.out.println("   sending...");
			returnValue = fMMRcon.sendMessage(message);
			System.out.println("   Value of SendingMethod = " + returnValue);

			System.out.println("   close connection");
			fMMRcon.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return returnValue;
	}

	/**
	 * rechte die Ganzzahl aus, welche groesser als a/b ist
	 * @param a
	 * @param b
	 * @return
	 */
	private int ceilDiv(double a, double b)
	{
		return (int) Math.ceil(a / b);
	}

	public String getDecoded() {
		return m_decoded;
	}
}
