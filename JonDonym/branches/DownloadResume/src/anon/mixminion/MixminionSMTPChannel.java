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

package anon.mixminion;

import java.io.IOException;
import java.util.Vector;

import anon.mixminion.message.Message;
import anon.shared.AbstractChannel;

/** This class implements a channel,which speaks SMTP*/
public class MixminionSMTPChannel extends AbstractChannel
{
	/** the current State of the SMTPChannel **/
	private int m_state = -1;

	/** a Receiver-List of the eMail **/
	private Vector m_receiver = new Vector();

	/** the Text of the eMail **/
	private String m_text = "";


	public MixminionSMTPChannel()
	{
		super();
		m_state = 0;

		try
		{
			String first = "220 127.0.0.1 SMTP JAP_MailServer\r\n";
			toClient(first);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}


	protected void close_impl()
	{
		

	}

	protected void toClient(String message) throws IOException
	{
        //System.out.print("Sende zum Client: " + message);
        recv(message.getBytes(),0,message.length());
	}

	// 'send' empfaengt die Daten vom eMail-Progi
    protected void send(byte[] buff, int len) throws IOException {
		String s = new String(buff, 0, len);
        //System.out.print("empfange vom Client: " + s);

		// nach dem init //
        if (m_state==0)
		{
            if(s.toUpperCase().startsWith("HELO"))
			{
				m_state = 2;
				toClient("250 OK\r\n");
			}
            else if (m_state==0 && s.toUpperCase().startsWith("EHLO"))
			{
				m_state = 1;
				toClient("503\r\n");
			}
			else
			{
				//no HELO or EHLO !!! There is an Error!!!
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
			}
		}
		// nach dem EHLO //
        else if (m_state==1)
		{
            if(s.toUpperCase().startsWith("HELO"))
			{
				m_state = 2;
				toClient("250 OK\r\n");
			}
			else
			{
				// only HELO is supported
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
			}
		}
		// nach HELO/EHLO bevor MAILFROM
        else if (m_state==2)
		{
            if(s.toUpperCase().startsWith("MAIL FROM"))
			{
                m_receiver.removeAllElements(); // Empfaenger-Liste leeren
				m_text = ""; // Text-Nachricht leeren
				m_state = 3;
				toClient("250 OK\r\n");
			}
			else
			{
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
			}
		}
		// Nach MAILFROM vor RCTPTO //
        else if (m_state==3)
		{
			if (s.toUpperCase().startsWith("RCPT TO"))
			{
				String rec=s.substring(s.indexOf('<')+1,s.indexOf('>'));// RCPT TO:<John@Smith.net> //
                m_receiver.addElement(rec);
				toClient("250 OK\r\n");
			}
			else if (s.toUpperCase().startsWith("DATA"))
			{
				m_state = 4;
				toClient("354 Start mail input; end with <CRLF>.<CRLF>\r\n");
			}
			else
			{
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
			}
		}
		// im DATA -> lesen der Nachricht //
        else if (m_state==4)
		{
			m_text = m_text + s;

			if (m_text.endsWith("\r\n.\r\n")) // wenn "." empfangen //
			{
				//remove "\r\n.\r\n"
                m_text= m_text.substring(0,m_text.length()-5);
				String[] rec = new String[m_receiver.size()];
				m_receiver.copyInto(rec);
                EMail eMail = new EMail(rec,m_text);
                
    
                //test whether replyblock is usable
                boolean ok = true;
// TODO  abgelaufene rbs; rb history?             if (eMail.getType().equals("RPL"))
//                {
//                	Vector...
//                    if (!eMail.getReplyBlock().timetoliveIsOK())
//                    {
//                   	 toClient("554 min one ReplyBlock is too old!\r\n");
//                   	ok = false;
//                    }
//                }
 
                
                String sender = Mixminion.getMyEMail();
                //test whether target for replyblock is specified
                if (sender == "")
                {
                	 toClient("554 Keine Reply-E-Mail im JAP spezifiziert!\r\n");
                	 ok = false;
                }
                
                //all ok? -->go on
                if (ok)
                {
                    boolean success = false;
                    Message m = null;
     
                	//get hops
                    int hops = Mixminion.getRouteLen();

                    //for testing we send for the first specified recipient one message,
                    //later mixminion will support up to 8 recipients in in one packet.
                    //for now there is no such support
                   
                    //Password
                    PasswordManager pwm = new PasswordManager();
                    String password = pwm.getPassword();
             
                    //build message(s)
                    m = new Message(eMail, hops, sender, password, 3);

                    //send 	
                    success = m.send();

    				m_state = 5;
                    
                    if (success==true)
                    {
                    	toClient("250 OK\r\n");
                    }
                    else 
                    {
                    	
                    	//1. something was to decode-->all is ok
                    	//2. other
                    	String clientanswer="";
                    	if (m.getDecoded() != null) 
                    		{
                    		clientanswer="250 OK\r\n" ;
                    		}
                    	else 
                    		{
                    		clientanswer =	"554 Fehler beim Versenden der eMail zum MixMinionServer!\r\n";
                    		}
                    	toClient(clientanswer);
                    }
                }
			}
			}
        else if (m_state==5)
		{
			if (s.toUpperCase().startsWith("QUIT"))
			{
                m_receiver.addElement(s);
				toClient("221 Bye\r\n");
				m_state = 99;
			}
            else if(s.toUpperCase().startsWith("MAIL FROM"))
			{
                m_receiver.removeAllElements(); // Empfaenger-Liste leeren
				m_text = ""; // Text-Nachricht leeren
				m_state = 3;
				toClient("250 OK\r\n");
			}
            else if(s.toUpperCase().startsWith("RSET"))
			{
				m_state = 2;
				toClient("250 OK\r\n");
			}
			else
			{
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
			}
		}
		else
		{
			// Zustand nicht moeglich //
			throw new RuntimeException("(State=" + m_state + ") This State is not possible");
		}
	}


    public int getOutputBlockSize() {
		
		return 1000;
	}

}
