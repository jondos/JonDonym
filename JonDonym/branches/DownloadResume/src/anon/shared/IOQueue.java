/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package anon.shared;

import java.io.IOException;

final class IOQueue
{
	private byte[] buff;
	private int readPos;
	private int writePos;
	private boolean bWriteClosed;
	private boolean bReadClosed;
	private final static int BUFF_SIZE = 10000;
	private boolean bFull;

	public IOQueue()
	{
		buff = new byte[BUFF_SIZE];
		readPos = 0;
		writePos = 0;
		bWriteClosed =false;
		bReadClosed = false;
		bFull = false;
	}

	public synchronized void write(byte[] in, int pos, int len) throws IOException
	{ //RingBuffer: (Start)
		//	--------------------------------------------
		//	^
		//  |Writepos
		//  |RreadPos
		//
		//After some Write
		//	--------------------------------------------
		//	^                ^
		//                    |Writepos
		//  |ReadPos
		int toCopy;
		while (len > 0)
		{
			if (bReadClosed || bWriteClosed)
			{
				throw new IOException("IOQueue closed");
			}
			if (bFull)
			{
				notify(); //awake readers
				try
				{
					wait();
				}
				catch (InterruptedException e)
				{
					throw new IOException("IOQueue write interrupted");
				} //wait;
				continue ;
			}
			if (readPos <= writePos)
			{
				toCopy = BUFF_SIZE - writePos;
			}
			else
			{
				toCopy = readPos - writePos;
			}
			if (toCopy > len)
			{
				toCopy = len;
			}
			System.arraycopy(in, pos, buff, writePos, toCopy);
			pos += toCopy;
			writePos += toCopy;
			len -= toCopy;
			if (writePos >= BUFF_SIZE)
			{
				writePos = 0;
			}
			if (readPos == writePos)
			{
				bFull = true;
			}
		} //End while
		notify(); //awake Readers
	}

	public synchronized int read() throws IOException
	{
		while (true)
		{
			if (bReadClosed)
			{
				throw new IOException("IOQueue closed");
			}
			if (readPos == writePos && !bFull) //IOQueue is empty
			{
				if (bWriteClosed)
				{
					return -1;
				}
				else
				{
					notify(); //awake Writers;
					try
					{
						wait();
					}
					catch (InterruptedException e)
					{
						throw new IOException("IOQueue read() interrupted");
					}
					continue ;
				}
			}
			int i = buff[readPos++] & 0xFF;
			if (readPos >= BUFF_SIZE)
			{
				readPos = 0;
			}
			if (bFull)
			{
				bFull = false;
				notify(); //awake Writers;
			}
			return i;
		}
	}

	public synchronized int read(byte[] in, int pos, int len) throws IOException
	{
		if (len <= 0)
		{
			return 0;
		}
		while (true)
		{
			if (bReadClosed)
			{
				throw new IOException("IOQueue closed");
			}
			if (readPos == writePos && !bFull) //IOQueue is empty
			{
				if (bWriteClosed)
				{
					return -1;
				}
				else
				{
					notify(); //awake Writers;
					try
					{
						wait();
					}
					catch (InterruptedException e)
					{
						throw new IOException("IOQueue read() interrupted");
					}
					continue ;
				}
			}
			int toCopy;
			if (writePos <= readPos)
			{
				toCopy = BUFF_SIZE - readPos;
			}
			else
			{
				toCopy = writePos - readPos;
			}
			if (toCopy > len)
			{
				toCopy = len;
			}
			System.arraycopy(buff, readPos, in, pos, toCopy);
			readPos += toCopy;
			if (readPos >= BUFF_SIZE)
			{
				readPos = 0;
			}
			if (bFull)
			{
				bFull = false;
				notify(); //awake Writers;
			}
			return toCopy;
		}

	}

	public synchronized int available()
	{
		if (bFull)
		{
			return BUFF_SIZE;
		}
		if (readPos == writePos && !bFull) //IOQueue is empty
		{
			return 0;
		}
		if (writePos <= readPos)
		{
			return BUFF_SIZE - readPos;
		}
		else
		{
			return writePos - readPos;
		}
	}

	public synchronized void closeWrite()
	{
		bWriteClosed = true;
		notify();
	}

	public synchronized void closeRead()
	{
		bReadClosed = true;
		notify();
	}

	public synchronized void finalize() throws Throwable
	{
		bReadClosed = true;
		bWriteClosed = true;
		notify();
		buff = null;
		super.finalize();
	}
}
