/*
Copyright (c) 2000, The JAP-Team
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

/**@todo Temporary removed - needs to be rewritten.. */

//import jap.JAPController;
//import jap.JAPModel;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Vector;
import anon.shared.AbstractChannel;

/**
 * @author Stefan Roenisch
 *
 *
 */
public class MixminionPOPChannel extends AbstractChannel {

		/** the current State of the POPChannel **/
		private int m_state = -1;
		private Vector m_messages = new Vector(); //Vector with messages as Strings
		private String[] m_deleted = null;

		public MixminionPOPChannel()
		{
			super();
			m_state = 0;

			try
			{
				/**@todo Temporary removed - needs to be rewritten.. */
				//m_messages = JAPModel.getMixminionMessages();
				m_deleted = new String[m_messages.size()];
				for (int i = 0; i < m_messages.size(); i++) {
					m_deleted[i] = (String)m_messages.elementAt(i);
				}
				String first = "+OK JAP POP3 server ready\r\n";
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
//	        System.out.print("Sende zum Client: " + message);
	        recv(message.getBytes(),0,message.length());
		}

		// 'send' empfaengt die Daten vom eMail-Progi
	    protected void send(byte[] buff, int len) throws IOException {
			String s = new String(buff, 0, len);
//	        System.out.print("empfange vom Client: " + s);

//	        Server: +OK POP3 server ready
//			Client: user plate
//			Server: +OK
	        if (m_state==0)
			{
	            if(s.toUpperCase().startsWith("USER"))
				{
					m_state = 1;
					toClient("+OK\r\n");
				}
	            else if (s.toUpperCase().startsWith("AUTH") || s.toUpperCase().startsWith("CAPA"))
	            {
	            	//m_state = 0;
					toClient("-ERR unrecognized\r\n");
	            }
	            else
				{
					//There is an Error!!!
	                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
				}
			}

//			Client: pass xyz1230
//			Server: +OK

	        else if (m_state==1)
			{
	            if(s.toUpperCase().startsWith("PASS"))
				{
					m_state = 2;
					toClient("+OK\r\n");
				}
				else
				{
					throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
				}
			}
//			Client: LIST
//			Server: +OK 3 messages (520 octets)
//			Server: 1 120
//			Server: 2 190
//			Server: 3 210
//			Server: .
	        else if (m_state==2)
			{
	        	if(s.toUpperCase().startsWith("STAT"))
	        	{

	        		int size = 0;
	        		for (int i = 0; i < m_messages.size(); i++) {
	        			size += ((String) m_messages.elementAt(i)).getBytes().length;
	        		}
	        		toClient("+OK " + m_messages.size() +" " + size +"\r\n");

	        	}
	        	else
	        		{
	        		if(s.toUpperCase().startsWith("LIST"))
	        		{
	        			m_state = 3;
	        			toClient("+OK " + m_messages.size() +" messages" + "\r\n");
	        			for (int i=0; i < m_messages.size(); i++) {
	        				toClient(i+1 + " "+((String) m_messages.elementAt(i)).getBytes().length +"\r\n");
	        			}
	        			toClient(".\r\n");
	        		}
	        		else
	        		{
	        			if (s.toUpperCase().startsWith("QUIT"))
	        			{
	        			//savechanges
	        			//
	        			toClient("+OK\r\n");
	        			}
	        			else
	        			{
	        				throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
	        			}
	        		}
	        		}
			}


//			Client: RETR 1
//			Server: +OK 120 octets
//			Server: <... sendet Nachricht 1>
//			Server: .
//			Client: DELE 1
//			Server: +OK message 1 deleted
	        else if (m_state==3)
			{
				//dont want this extended stuff
	        	if (s.startsWith("UIDL") || (s.startsWith("XTND") ))
				{
					toClient("-ERR unrecognized\r\n");
				}
	        	else if (s.startsWith("TOP"))
	        	{
	        		int id = Integer.parseInt(s.substring(4,5)); //which message
	        		int lines = 1;//Integer.parseInt(s.substring(6));//how many lines
	        		System.out.println("id: " +id + " lines: "+ lines);
	        		toClient("+OK " +((String)m_messages.elementAt(id-1)).getBytes().length + " octets\r\n");
					LineNumberReader reader = new LineNumberReader(new StringReader((String)m_messages.elementAt(id-1)));
					String aktLine = reader.readLine();
					//send the first requested lines of data
					//3 Headers and the \n and the requestes lines...
					for (int i = 0; (i < 4+lines) || (aktLine == null); i++)
					{
						toClient(aktLine + "\r\n");
						aktLine = reader.readLine();
					}
					toClient(".\r\n");
	        	}
				else if (s.startsWith("RETR"))
				{
					int id = 1;//Integer.parseInt(s.substring(5)); //which message
					toClient("+OK " +((String)m_messages.elementAt(id-1)).getBytes().length + " octets\r\n");
					LineNumberReader reader = new LineNumberReader(new StringReader((String)m_messages.elementAt(id-1)));
					String aktLine = reader.readLine();
					while (aktLine != null) {
						toClient(aktLine  + "\r\n");
						aktLine = reader.readLine();
					}

					toClient(".\r\n");
				}
				else if (s.startsWith("DELE"))
				{
					int id = 1;//Integer.parseInt(s.substring(6));
					m_deleted[id-1] = null;
					toClient("+OK message " + id + " deleted\r\n");
				}
//				Client: QUIT
//				Server: +OK
				else if (s.toUpperCase().startsWith("QUIT"))
				{
					//savechanges
					m_messages = new Vector();
					for (int i = 0; i < m_deleted.length; i++ ) {
						if (m_deleted[i] != null) {
							m_messages.addElement(m_deleted[i]);
						}
					}
					if (m_messages.size() == 0) {
						m_messages = null;
					}
					/**@todo Temporary removed - needs to be rewritten.. */
					//JAPController.setMixminionMessages(m_messages);
					toClient("+OK\r\n");

				}
				else
				{
	                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
				}
			}
	    }





	    public int getOutputBlockSize() {
			//
			return 1000;
		}

}
