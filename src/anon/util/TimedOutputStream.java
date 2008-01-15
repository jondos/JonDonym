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
package anon.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InterruptedIOException;
import java.util.Hashtable;
import java.util.Enumeration;

/** This class implements an OutputStream, where a timeout for the write() and flush () operations can be set.
 *
 */

final public class TimedOutputStream extends OutputStream
{
	private OutputStream m_Out;
	private long m_TimeoutInTicks;
	private volatile long m_TimeOutTick;
	private volatile boolean m_bTimedOut;
	private static Thread ms_threadInterrupt;
	private static Hashtable ms_hashtableOutputStreams;
	private static volatile long ms_currentTick;
	final static long MS_PER_TICK = 5000;

	final static private class TimedOutputStreamInterrupt implements Runnable
	{
		//private volatile boolean m_bRun = true;
		public void run()
		{
			ms_currentTick = 0;
			while (true)
			{
				try
				{
					Thread.sleep(MS_PER_TICK);
				}
				catch (InterruptedException ex)
				{
					continue;
				}
				ms_currentTick++;
				try
				{
					Enumeration elements = ms_hashtableOutputStreams.elements();
					while (elements.hasMoreElements())
					{
						TimedOutputStream elem = (TimedOutputStream) elements.nextElement();
						if (ms_currentTick > elem.m_TimeOutTick)
						{
							try
							{
								elem.m_bTimedOut=true;
								elem.close();
							}
							catch (Throwable t)
							{
							}
						}
					}
				}
				catch (Exception ex1)
				{
				}
			}
		}

	}

	private TimedOutputStream()
	{
	}

	public static void init()
	{
		ms_hashtableOutputStreams = new Hashtable(1000);
		ms_threadInterrupt = new Thread(new TimedOutputStreamInterrupt(),"TimedOutputStream");
		ms_threadInterrupt.setDaemon(true);
		ms_threadInterrupt.start();
	}

	/**
	 *
	 * @param parent OutputStream the outputstrem which will be used for I/O operations
	 * @param msTimeout long the timeout in milli seconds for the write operations (zero means blocking I/O)
	 */
	public TimedOutputStream(OutputStream parent, long msTimeout)
	{
		m_Out = parent;
		m_TimeoutInTicks = msTimeout / MS_PER_TICK;
	}

	/**
	 * Writes the specified byte to this output stream.
	 *
	 * @param b the <code>byte</code>.
	 * @throws IOException if an I/O error occurs. In particular, an
	 *   <code>IOException</code> may be thrown if the output stream has
	 *   been closed.
	 * @todo Diese java.io.OutputStream-Methode implementieren
	 */
	public void write(int b) throws IOException
	{
		m_TimeOutTick = ms_currentTick + m_TimeoutInTicks;
		ms_hashtableOutputStreams.put(this, this);
		m_bTimedOut = false;
		try
		{
			m_Out.write(b);
		}
		catch (IOException e)
		{
			ms_hashtableOutputStreams.remove(this);
			if (m_bTimedOut) //I/O was interrupted
			{
				throw new InterruptedIOException("TimedOutputStream: write() timed out!");
			}
			else
			{
				throw e;
			}
		}
		ms_hashtableOutputStreams.remove(this);
	}

	public void write(byte[] b) throws IOException
	{
		write(b, 0, b.length);
	}

	public void write(byte[] b, int i1, int i2) throws IOException
	{
		m_TimeOutTick = ms_currentTick + m_TimeoutInTicks;
		ms_hashtableOutputStreams.put(this, this);
		m_bTimedOut = false;
		try
		{
			m_Out.write(b, i1, i2);
		}
		catch (IOException e)
		{
			ms_hashtableOutputStreams.remove(this);
			if (m_bTimedOut) //I/O was interrupted
			{
				throw new InterruptedIOException("TimedOutputStream: write() timed out!");
			}
			else
			{
				throw e;
			}
		}
		ms_hashtableOutputStreams.remove(this);
	}

	public void close() throws IOException
	{
		m_Out.close();
	}

	public void flush() throws IOException
	{
		m_TimeOutTick = ms_currentTick + m_TimeoutInTicks;
		ms_hashtableOutputStreams.put(this, this);
		m_bTimedOut = false;
		try
		{
			m_Out.flush();
		}
		catch (IOException e)
		{
			ms_hashtableOutputStreams.remove(this);
			if (m_bTimedOut) //I/O was interrupted
			{
				throw new InterruptedIOException("TimedOutputStream: flush() timed out!");
			}
			else
			{
				throw e;
			}
		}
		ms_hashtableOutputStreams.remove(this);
	}

}
