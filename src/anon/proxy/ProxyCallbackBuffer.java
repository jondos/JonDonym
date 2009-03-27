/*
Copyright (c) 2008 The JAP-Team, JonDos GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
       the JonDos GmbH, nor the names of their contributors may be used to endorse or
       promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package anon.proxy;

import java.io.ByteArrayOutputStream;

public class ProxyCallbackBuffer 
{	
	private byte[] chunk = null;
	private int modificationStartOffset = 0;
	private int modificationEndOffset = 0;
	private int payloadLength = 0;
	private int status = ProxyCallback.STATUS_PROCESSABLE;
	
	public ProxyCallbackBuffer()
	{
		this(new byte[1000]);
	}
	
	public ProxyCallbackBuffer(byte[] chunk)
	{
		this(chunk, 0, chunk.length-1);
	}
	
	public ProxyCallbackBuffer(byte[] chunk, int modifcationStartOffset,
			int payloadLength) 
	{
		setChunk(chunk);
		setModificationStartOffset(modifcationStartOffset);
		setPayloadLength(payloadLength);
		setModificationEndOffset(payloadLength-1);
		
		this.status = ProxyCallback.STATUS_PROCESSABLE;
	}
	
	public ProxyCallbackBuffer(byte[] chunk, int modifcationStartOffset,
			int modifcationEndOffset, int payloadLength) 
	{
		setChunk(chunk);
		setModificationStartOffset(modifcationStartOffset);
		setModificationEndOffset(modifcationEndOffset);
		
		this.status = ProxyCallback.STATUS_PROCESSABLE;
	}

	public byte[] getChunk() 
	{
		return chunk;
	}

	public void setChunk(byte[] chunk) 
	{
		this.chunk = chunk;
		this.modificationStartOffset = 0;
		this.modificationEndOffset = chunk.length-1;
		this.payloadLength = chunk.length;
	}

	public int getModificationStartOffset() 
	{
		return modificationStartOffset;
	}

	public void setModificationStartOffset(int modificationStartOffset) 
	{
		if( (modificationStartOffset < 0) || (modificationStartOffset > chunk.length) )
		{
			throw new ArrayIndexOutOfBoundsException("Illegal modification start index: "+modificationStartOffset+" (chunk length: "+chunk.length+")");
		}
		this.modificationStartOffset = modificationStartOffset;
	}

	public int getModificationEndOffset() 
	{
		return modificationEndOffset;
	}

	public void setModificationEndOffset(int modificationEndOffset) 
	{
		if( (modificationEndOffset < 0) || (modificationEndOffset > chunk.length) )
		{
			throw new ArrayIndexOutOfBoundsException("Illegal modification end index: "+modificationEndOffset+" (chunk length: "+chunk.length+")");
		}
		this.modificationEndOffset = modificationEndOffset;
	}
	
	public int getPayloadLength() 
	{
		return payloadLength;
	}

	public void setPayloadLength(int payloadLength) 
	{
		if( (payloadLength < 0) || (payloadLength > chunk.length) )
		{
			throw new ArrayIndexOutOfBoundsException("Illegal payload length: "+payloadLength);
		}
		this.payloadLength = payloadLength;
	}

	public int getStatus() 
	{
		return status;
	}

	public void setStatus(int status) 
	{
		if( (status < ProxyCallback.STATUS_FINISHED) || 
			(status > ProxyCallback.STATUS_PROCESSABLE) )
		{
			throw new IllegalArgumentException("Illegal status specified: "+status);
		}
		this.status = status;
	}
	
	public void copyLeadingData(ByteArrayOutputStream dest)
	{
		if(modificationStartOffset > 0)
		{
			dest.write(chunk, 0, modificationStartOffset);
		}
	}
	
	public void copyLeadingData(byte[] dest)
	{
		copyLeadingData(dest, 0);
	}
	
	public void copyLeadingData(byte[] dest, int destPos)
	{
		if((destPos + modificationStartOffset) > dest.length)
		{
			throw new ArrayIndexOutOfBoundsException("leading data length "+modificationStartOffset+
					" excceeds destination array");
		}
		if(modificationStartOffset > 0)
		{
			System.arraycopy(chunk, 0, dest, destPos, modificationStartOffset);
		}
	}
	
	public void copyTrailingData(ByteArrayOutputStream dest)
	{
		int trailingDataLength = getTrailingDataLength();
		if(trailingDataLength > 0)
		{
			dest.write(chunk, (modificationEndOffset+1), trailingDataLength);
		}
	}
	
	public void copyTrailingData(byte[] dest, int destPos)
	{
		int trailingDataLength = getTrailingDataLength();
		if((destPos + trailingDataLength) > dest.length)
		{
			throw new ArrayIndexOutOfBoundsException("trailing data length "+trailingDataLength+
					" excceeds destination array");
		}
		if(trailingDataLength > 0)
		{
			System.arraycopy(chunk, (modificationEndOffset+1), dest, destPos, trailingDataLength);
		}
	}
	
	public int getLeadingDataLength()
	{
		return modificationStartOffset;
	}
	
	public int getTrailingDataLength()
	{
		return payloadLength - (modificationEndOffset + 1);
	}
}
