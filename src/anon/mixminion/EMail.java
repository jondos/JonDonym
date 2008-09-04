/*
Copyright (c) 2005, The JAP-Team
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
package anon.mixminion;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Vector;

import anon.mixminion.message.ReplyBlock;


public class EMail
{
    
	private String[] m_receiver = new String[1]; 
    private String m_payload = null;
    private Vector m_replyblocks = new Vector();
    private String m_type = "";//ENC;NOR;RPL
    private String m_multipartid = "";
    
    

    /**
     * The Constructor of an eMail, which scould be send over the MixMinion-Net
     * @param receiver, a list of receivers of this eMail
     * @param payload, the Payload of this eMail
     */
    public EMail(String[] receiver, String payload)
    {
    	//there are three possibilities:
    	//	1. encrypted Message to decode
    	// 	2. reply  E-Mail recipient specified due Replyblock
    	// 	3. normal E-Mail recipient specified due E-Mail-Header 
    	
    	//test 1.
    	if (testonEncrypted(payload)) {
    		m_type = "ENC";
    		m_payload = payload;
    	}
    	// or 2./3.
    	else  {
    		  		
    		//is/are there rb('s)?
    		try
    		{
    			m_replyblocks = ReplyBlock.parseReplyBlocks(payload, null);
    			
    		} catch (IOException e)
    		{
    			e.printStackTrace();
    		}
    		
    		//remove the replyblock from the payload, he is not needed anymore
    		if (m_replyblocks.size() > 0)
    		{
    			m_type = "RPL";
    			m_receiver[0] = "anonymous@fragmented.de";
    			try
    			{
    				payload = ReplyBlock.removeRepyBlocks(payload);
    			} catch (IOException e1)
				{
    				e1.printStackTrace();
    			}
    		}
    		else
    		{
    			m_type = "NOR";
    			//set receiver
        		this.m_receiver = receiver;
    		}
    		
    		//prepare the payload for normal/reply messages
    		try 
			{
    			//remove the headers which make Probs with the anonymity
    			this.m_payload  = trimPayload(payload);
    		} catch (IOException e) 
			{
    			e.printStackTrace();
    		} 		
    	}
    	
     }
    	

    /**
     * test a given E-Mail payload if its encrypted or not
     * @param p
     * @return
     */
    private boolean testonEncrypted(String p)
    {
    	LineNumberReader reader = new LineNumberReader(new StringReader(p));
    	String aktLine = "start";

    	while (1==1) 
    	{
    		try {
				aktLine = reader.readLine();
			} catch (IOException e) {
				break;
			}
			if (aktLine == null) {
				break;
			}
			else if (aktLine.startsWith("Message-type: encrypted")) {
    			return true;
    		}
    	}
    	return false;
    }
    /**
     * @return the Receivers of this eMail
     */
    public String[] getReceiver()
    {
        return m_receiver;
    }

    /**
     * @return the Payload of this eMail
     */
    public String getPayload()
    {
        return m_payload;
    }
    
    /**
     * Adds the specified Replyblock(as String) to the payload
     * @param p
     */
    public void addRBtoPayload(String p)
    {
    	
    	if (m_multipartid.equals(""))
    	{
    		m_payload += p;
    	}
    	else 
    		{
//    		
//    		int index = m_payload.lastIndexOf("--" + m_multipartid + "\n"+
//											"Content-Type:");
//    		m_payload = m_payload.substring(0, index-1) + p + "\n\n" +
//							m_payload.substring(index);
    		
    		m_payload = m_payload.substring(0, m_payload.indexOf("--" + m_multipartid +"--")) +
						"\n--" + m_multipartid +"\nContent-Type: text/plain; charset=ISO-8859-15\n"+
						"Content-Transfer-Encoding: 7bit\n" + p +"\n--" + m_multipartid +"--";
    		//System.out.println(m_payload);
    		}
    }
    /**
     * 
     * @return String NOR=Normal; RPL = Replymessage; ENC = content to decode locally
     */
    public String getType() 
    {
    	return m_type;
    }

    /**
     * 
     * @return ReplyBlock
     */
    public Vector getReplyBlocks() 
    {
    	return m_replyblocks;
    }
   
    public String toString()
    {
        String ret = "";
        for (int i=0; i<m_receiver.length; i++)
        {
            ret = ret + "[" + m_receiver[i] + "]\n";
        }
        ret = ret + m_payload;

        return ret;
    }
    
    
    /**
     * removes/overwrites non anonym headers
     * brings the ascii armor of an replyblock in the right form
     * @param p
     * @return
     * @throws IOException
     */
    private String trimPayload(String p) throws IOException {
		// from client there is something like this:
//		Message-ID: <453E0340.6050709@biw.de>
//		Date: Tue, 24 Oct 2006 14:12:48 +0200
//		From: Anonymer Stefan <anostef@biw.de>
//		User-Agent: Thunderbird 1.5.0.7 (Windows/20060909)
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
//		mit nem anhang
    	String subject = "";
    	String trimmed = "";
    	String multi = "";
     	//for normal messages:
    	if (m_type.equals("NOR")) 
		{
			trimmed +=  "\nMessage created with JAP/Mixminion Anonymous Mailing\n\n";
			multi = "- ";
		}
		LineNumberReader reader = new LineNumberReader(new StringReader(p));
		String aktLine = reader.readLine();
		while (!aktLine.startsWith("Subject")) {
			aktLine = reader.readLine();
		}
		subject += "Titel: " + aktLine.substring(9) + "\n";
		
		while (aktLine.length() > 0) {
			aktLine = reader.readLine();//Skip other headers
			//if multipart
			if (aktLine.startsWith("Content-Type: multipart/mixed;")) 
			{
				aktLine = reader.readLine();
				trimmed += "MIME-Version: 1.0\n" + subject + "Content-Type: multipart/mixed;\n" +
								aktLine.substring(0,11) + multi +
								aktLine.substring(11)+"\n";
				m_multipartid = aktLine.substring(11,aktLine.length()-1);
			}
		}
		
		if (m_multipartid.equals(""))
		{
			trimmed += subject;
		}
		
		while (aktLine != null) {
			trimmed += aktLine +"\n";
			aktLine = reader.readLine();
		}
		return trimmed;
    }
    
 
}
