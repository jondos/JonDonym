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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Vector;

import anon.mixminion.mmrdescription.MMRDescription;
import anon.util.Base64;
import anon.util.ByteArrayUtil;
//import java.util.Properties;
//import javax.mail.Session;
//import javax.mail.internet.MimeMessage;
//import java.io.ByteArrayInputStream;
//import javax.mail.internet.MimeMultipart;

/**
 * @author Stefan Roenisch
 *
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ReplyBlock {

	static final int KEY_LEN = 16;
    // We discard SURB keys after 3 months.
    static final long KEY_LIFETIME = 3*30*24*60*60;

	private byte[] m_sharedSecret;
	private Vector m_path; //path for the reply-header -->the second leg
	private byte[] m_longterm_secret; //userkey
	private RoutingInformation m_myrouting; //RoutingInformation for the first server -->Crossoverpoint
	private byte[] m_headerbytes;
	private String m_myaddress; //the address on which the block points on
	private long m_timetolive;


	/**
	 * Constructor 1
	 * used to build a replyblock, which is appended to a message
	 * call build() to complete it!
	 * @param myadress
	 * @param path
	 * @param usersecret
	 */
	public ReplyBlock(String myaddress, Vector path, byte[] usersecret){
		this.m_myaddress = myaddress;
		this.m_path = path;
		this.m_longterm_secret = usersecret;
		this.m_myrouting = new RoutingInformation();
		this.m_headerbytes = null;

	}

	/**
	 * Constructor 2
	 * used to instantiate a ReplyBlock Object,
	 * used to parse a ReplyBlock out of a message
	 * call parseReplyBlock(String) to complete iT!
	 * @param routingInfo
	 * @param header
	 * @param sharedsecret
	 * @param time
	 */
	public ReplyBlock(RoutingInformation routingInfo, byte[] header, byte[] sharedsecret, long time) {
		this.m_myrouting = routingInfo;
		this.m_headerbytes = header;
		this.m_sharedSecret = sharedsecret;
		this.m_timetolive = time;
		}

	/**
	 * builds the replyHeader
	 *
	 */
	public void buildBlock() {
		System.out.println("Baue ReplyBlock an: " + m_myaddress);
		int hops = m_path.size();

		// Repeat:
		// Let SEED = a random 159-bit seed.
		// Until Hash(SEED | SEC | "Validate") ends with a 0 octet.
		// start value
		byte[] seed = null; //==tag

		while (true) {
			//make a 20 byte random array
			seed = MixMinionCryptoUtil.randomArray(20);
			//first bit of a decoding handle is always zero
			seed[0] &= 0x7F;
			//test whether last octed is zero
			byte last = (MixMinionCryptoUtil.hash(ByteArrayUtil.conc(seed, m_longterm_secret, "Validate".getBytes())))[19];
			if (last == 0x00) break;
		}

		// Let K = Hash(SEED | SEC | "Generate")[0:KEY_LEN]
		byte[] key = ByteArrayUtil.copy(MixMinionCryptoUtil.hash(
							ByteArrayUtil.conc(seed, m_longterm_secret, "Generate".getBytes())),
								0,KEY_LEN);

		// Let STREAM = PRNG(K, KEY_LEN*(PATH_LEN + 1))
		// Let SHARED_SECRET = STREAM[PATH_LEN*KEY_LEN:KEY_LEN]
		byte[] stream = MixMinionCryptoUtil.createPRNG(key, KEY_LEN * (hops + 1));
		m_sharedSecret = ByteArrayUtil.copy(stream, hops * KEY_LEN, KEY_LEN );

		// For i in 1 .. PATH_LEN
		// Let MS_i = STREAM[(PATH_LEN-i)*KEY_LEN : KEY_LEN] STREAM = ENC(K, Z(MAX_PATH * KEY_LEN)) steht woanders
		// Generate a reply block using MS_i as the master secret for the
		// i'th node in the hop, SEED as the tag, and SHARED_SECRET as the
		// end-to-end shared secret.

		Vector master_secret = new Vector();

		//mastersecrets
		for (int i = 1; i <= hops; i++) {
			master_secret.addElement(ByteArrayUtil.copy(stream, (hops-i) * KEY_LEN, KEY_LEN ));
		}

		//ExitInfo bauen
		ExitInformation exitInfo = new ExitInformation();
		String[] destination = new String[1];
		destination[0] = m_myaddress;
		exitInfo = MMRDescription.getExitInformation(destination,seed);

		//build replyHeader
		Header replyHeader = new Header(m_path, master_secret, exitInfo);
		m_headerbytes = replyHeader.getAsByteArray();

		//info about the first router for crossoverpoint
		m_myrouting.m_Type = RoutingInformation.TYPE_SWAP_FORWARD_TO_HOST;
		m_myrouting.m_Content = ((MMRDescription)m_path.elementAt(0)).getRoutingInformation().m_Content;

		//timetolive
		//time in seconds since midnight 1.Jan 1970
		Calendar cal = Calendar.getInstance();
		cal.setTime(new java.util.Date());
		int day = cal.get(Calendar.DAY_OF_YEAR);
		int year = cal.get(Calendar.YEAR);
		m_timetolive = ((((year-1970-1)*365)+day)*24*60*60)+KEY_LIFETIME;
		}

	/**
	 * Returns the ReplyHeader as Bytes
	 * @return the replyHeader
	 */
	public byte[] getHeaderBytes() {
		return m_headerbytes;
	}

	/**
	 * Returns the End-to End shared Secret
	 * @return sharedSecret End to End
	 */
	public byte[] getSharedSecret() {
		return m_sharedSecret;
	}

	/**
	 * Returns the Replyblock as described underwards
	 * @return ReplyBlock as Bytes
	 */
	public byte[] getReplyBlockasBytes() {

//		# A reply block is: the string "SURB", a major number, a minor number,
//		#   a 4-byte "valid-until" timestamp, a 2K header, 2 bytes of routingLen for
//		#   the last server in the first leg; 2 bytes of routingType for the last
//		#   server in the first leg; a 16-byte shared end-to-end key, and the
//		#   routingInfo for the last server.

		byte[] beginmarker = {0x53, 0x55, 0x52, 0x42, 0x01,0x00}; //=="SURB"+1+0 We use Version 1.0

        return ByteArrayUtil.conc(beginmarker,
                           ByteArrayUtil.inttobyte(m_timetolive,4), m_headerbytes,
                           ByteArrayUtil.inttobyte(m_myrouting.m_Content.length,2),
                           ByteArrayUtil.inttobyte(4,2),
                           ByteArrayUtil.conc(m_sharedSecret, m_myrouting.m_Content));
	}

	/**
	 *
	 * @return Base64-encoded ReplyBlock with ascii armor
	 *
	 */
	public String getReplyBlockasString() {
		return "\n\n:-----BEGIN TYPE III REPLY BLOCK-----\nVERSION: 0.2\n"
				+ Base64.encodeBytes(getReplyBlockasBytes())
				+ "\n:-----END TYPE III REPLY BLOCK-----";
	}

	/**
	 * Returns the RoutingInformation of the First Server in the Path of the ReplyBlock, needed for the Crossover-Point
	 * @return
	 */
	public RoutingInformation getRouting() {
		return m_myrouting;
	}

	/**
	 * Parses a given payload for a ReplyBlock and returns it,
	 * return null, when no ReplyBlock is detected
	 * @param message
	 * @param block
	 * @return
	 * @throws IOException
	 */
	public static Vector parseReplyBlocks(String message, byte[] block ) throws IOException {
//sk13 parsing the message as mime message
		// Get a Properties object needed for initalising a mail session object
/*		Properties props = System.getProperties();
		 // Get a mail  Session object
	Session session = Session.getInstance(props, null);
	try
	{
		MimeMessage msg = new MimeMessage(session, new ByteArrayInputStream(message.getBytes()));
		Object o=msg.getContent();
		if(o instanceof String)
			message=(String)o;
		else if(o instanceof MimeMultipart)
		{//more todo here...
			MimeMultipart multi=(MimeMultipart)o;
			message=(String)multi.getBodyPart(0).getContent();
		}


	}
	catch (MessagingException ex)
	{
	}
	*/
	Vector blocks = new Vector();

		message = message + "\n-----END OF PLAINTEXT MESSAGE-----"; //mark Message end

		LineNumberReader reader = new LineNumberReader(new StringReader(message));
		String aktLine = reader.readLine();

		while (true)
		{
			while (!aktLine.endsWith("-----BEGIN TYPE III REPLY BLOCK-----")) {
				aktLine = reader.readLine();
				if (aktLine.startsWith("-----END OF PLAINTEXT MESSAGE-----")) {
					return blocks;
				}
			}

			if (!reader.readLine().startsWith(">")) //skip Version and test wheter wrong format due to inreply
			{
				aktLine = reader.readLine();
				String myBlock = "";
				while (!aktLine.trim().endsWith("-----END TYPE III REPLY BLOCK-----"))
				{
					myBlock = myBlock + aktLine + "\n";
					aktLine = reader.readLine();
				}

				myBlock = myBlock.substring(0,myBlock.length()-1);
				//decode
				byte[] mybyteblock = Base64.decode(myBlock);
				//key-lifetime
				byte[] tl = new byte[4];
				for(int i = 0 ; i < 4 ; i++) {
					tl[i] = mybyteblock[6 + i];
				}
				long time = byteToInt(tl,0);
				//contentsize
				byte[] cs = new byte[2]; cs[0]= mybyteblock[2058]; cs[1]=mybyteblock[2059];
				int size = byteToInt(cs,0);
				//routinginfo
				RoutingInformation rInfo = new RoutingInformation();
				rInfo.m_Type = RoutingInformation.TYPE_SWAP_FORWARD_TO_HOST;
				//content
				byte[] content = new byte[size];
				for (int i = 2078; i<2078+size; i++) {
					content[i-2078] = mybyteblock[i];
				}
				rInfo.m_Content = content;
				//header
				byte[] h = new byte[2048];
				for (int i = 0; i<2048; i++) {
					h[i] = mybyteblock[i+10];
				}
				//sharedsecret
				byte[] secret = new byte[16];
				for (int i = 0; i<16; i++) {
					secret[i] = mybyteblock[2062+i];
				}

				blocks.addElement(new ReplyBlock(rInfo, h, secret,time));
			}
			else
			{
				aktLine = reader.readLine();
			}

		}
	}

	/**
	 * Removes a ascii-armored replyblock from a given payload
	 * @param message
	 * @return message without the replyblock
	 * @throws IOException
	 */
	public static String removeRepyBlocks(String message) throws IOException {
		LineNumberReader reader = new LineNumberReader(new StringReader(message));
		String aktLine = reader.readLine();
		String myBlock = "";
		boolean rb = false;
		while (aktLine != null) {
			if (aktLine.trim().endsWith("-----BEGIN TYPE III REPLY BLOCK-----"))
			{
				rb = true;
			}
			if (!rb)
			{
				myBlock = myBlock + "\n" + aktLine;
			}
			if (aktLine.trim().endsWith("-----END TYPE III REPLY BLOCK-----")) {
				rb = false;
			}

			aktLine = reader.readLine();
		}

		return myBlock;
	}

	/**
	 * tests whether the replyblock is not older then 3 months
	 * @return
	 */
	public boolean timetoliveIsOK() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new java.util.Date());
		int day = cal.get(Calendar.DAY_OF_YEAR);
		int year = cal.get(Calendar.YEAR);
		long now = (((year-1970-1)*365)+day)*24*60*60;
		return (now<m_timetolive);
	}
	/**
	 * Calculates the int value of a given ByteArray
	 * @param b
	 * @param offset
	 * @return int value of the bytearray
	 */
	private static int byteToInt(byte[] b, int offset) {
		int value = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (b.length - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
	}
}
