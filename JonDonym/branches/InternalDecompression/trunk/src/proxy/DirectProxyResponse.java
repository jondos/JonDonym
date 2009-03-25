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
package proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

//import jap.JAPUtil;
final class DirectProxyResponse implements Runnable
{
	private int m_threadNumber;
	private static int ms_threadCount;

	private OutputStream m_outputStream;
	private InputStream m_inputStream;

	public DirectProxyResponse(InputStream in, OutputStream out)
	{
		m_inputStream = in;
		m_outputStream = out;
	}

	public void run()
	{
		m_threadNumber = getThreadNumber();
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "R(" + m_threadNumber + ") - Response thread started.");
		try
		{
//---to be removed!!!
			/*			String nextLine = JAPUtil.readLine(m_inputStream);
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

				LogHolder.log(LogLevel.DEBUG, LogType.NET,
					 "R(" + m_threadNumber + ") - Header: >" + nextLine + "<");
				while (nextLine.length() != 0)
				{
				  // write single lines to server
				  m_outputStream.write( (nextLine + "\r\n").getBytes());
				 nextLine = JAPUtil.readLine(m_inputStream);
				 LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "R(" + m_threadNumber + ") - Header: >" + nextLine + "<");
				}

				// send final CRLF --> server
				m_outputStream.write("\r\n".getBytes());
				m_outputStream.flush();
			 */
//TO be removed end

			byte[] buff = new byte[1000];
			int len;
			while ( (len = m_inputStream.read(buff)) != -1)
			{
				if (len > 0)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET, "R(" + m_threadNumber + ") - "+new String(buff,0,len));
					m_outputStream.write(buff, 0, len);
					m_outputStream.flush();
				}
			}
			//-----------------------------------------------
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "R(" + m_threadNumber + ") - EOF from Server.");
		}
		catch (IOException ioe)
		{
			// this is normal when we get killed
			// so just do nothing...
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.NET,
						  "R(" + m_threadNumber + ") - Exception during transmission: " + e);
		}
		try
		{
			m_inputStream.close();
			m_outputStream.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
						  "R(" + m_threadNumber + ") - Exception while closing: " + e.toString());
		}
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "R(" + m_threadNumber + ") - Response thread stopped.");
	}

	private synchronized int getThreadNumber()
	{
		return ms_threadCount++;
	}
}
