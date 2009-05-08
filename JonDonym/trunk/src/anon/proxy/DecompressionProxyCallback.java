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
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class provides functionality for inflating zlib- or gzip-compressed content.
 * Due to http header replacement the JonDo may specify some content-encodings
 * which a client (i.e wget) does not support. In this case the JonDo decodes the content 
 * itself using this class. To use its functionality, an object of this class must be registered 
 * in the PoxyCallback-framework.
 * @author Simon Pecher
 */
public class DecompressionProxyCallback implements ProxyCallback
{
	Hashtable decompressionKits = new Hashtable();

	/**
	 * these constants are copied from GZIPInputstream
	 * because they are declared private there.
	 */
	private final static int FHCRC	= 2;	// Header CRC
	private final static int FEXTRA	= 4;	// Extra field
	private final static int FNAME	= 8;	// File name
	private final static int FCOMMENT	= 16;	// File comment
	
	private final int MAX_DECOMPRESSION_OUTPUT = 10000;
	
	/**
	 * slightly modified version of private method GZIPInputStream.readHeader to extract
	 * the GZIP header fields.
	 * returns the length of the header in bytes.
	 */
	private int readGZIPHeader(byte[] data, int offset, int length) 
			throws DataFormatException
	{
		int index = offset;
		int headerMagic = toUnsignedShort(data[index++], data[index++]);
		if (headerMagic != GZIPInputStream.GZIP_MAGIC) 
		{
		    throw new DataFormatException("Not in GZIP format");
		}
		// Check compression method
		if (toUnsignedByte(data[index++]) != 8) 
		{
		    throw new DataFormatException("Unsupported compression method");
		}
		// Read flags
		int flg = toUnsignedByte(data[index++]);
		// Skip MTIME, XFL, and OS fields
		index += 6;
		// Skip optional extra field
		if ((flg & FEXTRA) == FEXTRA) 
		{
			index += toUnsignedShort(data[index++], data[index++]);
		}
		// Skip optional file name
		if ((flg & FNAME) == FNAME) 
		{		
			while (toUnsignedShort(data[index++], data[index++]) != 0) ;
		}
		// Skip optional file comment
		if ((flg & FCOMMENT) == FCOMMENT) 
		{
			 while (toUnsignedShort(data[index++], data[index++]) != 0) ;
		}
		// Check optional header CRC
		if ((flg & FHCRC) == FHCRC) 
		{
			//skip CRC
			index += 2;
		}
		if(index >= offset+length)
		{
			throw new ArrayIndexOutOfBoundsException("index "+index+" exceeds "+(offset+length));
		}
		return (index - offset);
	}

    /*
     * Reads unsigned short in Intel byte order.
     */
	private int toUnsignedShort(byte lower, byte upper) 
	{
		 return (toUnsignedByte(upper) << 8) | lower;
	}

	private int toUnsignedByte(byte nByte)
	{
		 return ((nByte < 0) ? 128 : 0) + (0x7F & nByte);
	}
	    
	public void closeRequest(AnonProxyRequest anonRequest) 
	{
		DecompressionKit kit = (DecompressionKit) decompressionKits.remove(anonRequest);
		if(kit != null)
		{
			if(kit.getGzipInflater() != null)
			{
				kit.getGzipInflater().end();
			}
			if(kit.getZLibInflater() != null)
			{
				kit.getZLibInflater().end();
			}
		}
	}

	public synchronized int handleDownstreamChunk(AnonProxyRequest anonRequest,
			ProxyCallbackBuffer buffer)
			throws ProxyCallbackNotProcessableException 
	{
		if(buffer.getModificationStartOffset() < buffer.getPayloadLength() )	
		{
			String[] contentEncodingValues = anonRequest.getContentEncodings();
			if(contentEncodingValues != null)
			{
				Vector compressionSequence = new Vector(); 
				StringTokenizer valueTokenizer = null;
				for (int i = 0; i < contentEncodingValues.length; i++) 
				{
					valueTokenizer = new StringTokenizer(contentEncodingValues[i], "");
					String currentEncodingToken = null;
					while (valueTokenizer.hasMoreTokens()) 
					{
						currentEncodingToken = valueTokenizer.nextToken();
						if(currentEncodingToken.trim().equals(JonDoFoxHeader.HTTP_ENCODING_GZIP) ||
							currentEncodingToken.trim().equals(JonDoFoxHeader.HTTP_ENCODING_DEFLATE) )
						{
							compressionSequence.addElement(currentEncodingToken);
						}
						else 
						{
							LogHolder.log(LogLevel.WARNING, LogType.NET, "The Content-Encoding "+currentEncodingToken+" is not supported.");
						}
					}
				}
			
				if( compressionSequence.size()  > 0 )
				{
					String currentEncoding = null;
					
					Inflater inflater = null;
					DecompressionKit decompressionKit = null;
					
					int resultLength = 0;
					int leadingDataLength = 0;
					int trailingDataLength = 0;
					int inflateLength = 0;
					
					try
					{	
						for(int i = 0; i < compressionSequence.size(); i++)
						{
							currentEncoding = (String) compressionSequence.elementAt(i);
							int length = buffer.getModificationEndOffset() - 
								buffer.getModificationStartOffset() + 1;
							
							int gzipHeaderOffset = 0;
							boolean gzipEncoding = currentEncoding.equals(JonDoFoxHeader.HTTP_ENCODING_GZIP);
							decompressionKit = (DecompressionKit) decompressionKits.get(anonRequest);
							
							if(decompressionKit == null)
							{
								decompressionKit = new DecompressionKit();
								decompressionKit.setNewInflater(gzipEncoding);
								decompressionKit.setResult(new byte[MAX_DECOMPRESSION_OUTPUT]);
								gzipHeaderOffset = gzipEncoding ?
									readGZIPHeader(buffer.getChunk(), buffer.getModificationStartOffset(), length) : 0;
								decompressionKits.put(anonRequest, decompressionKit);
							}
							inflater = gzipEncoding ? decompressionKit.getGzipInflater() : decompressionKit.getZLibInflater();
							
							if(inflater.needsInput())
							{
								leadingDataLength = buffer.getLeadingDataLength();
								trailingDataLength = buffer.getTrailingDataLength();
								inflateLength = decompressionKit.getResult().length - trailingDataLength - leadingDataLength;
								
								//take the input directly from the corresponding chunk, which is most likely http-content data.
								inflater.setInput(buffer.getChunk(), 
										buffer.getModificationStartOffset() + gzipHeaderOffset,
										length - gzipHeaderOffset);
								
								//also consider the leading and trailing space of the input data buffer
								ByteArrayOutputStream bout = null;
								resultLength = inflater.inflate(decompressionKit.getResult(), leadingDataLength, inflateLength);
								while( (resultLength == inflateLength) && !inflater.needsInput())
								{
									/* result-buffer is too small:
									 * prepare a ByteArrayOutputStream to handle all bytes.
									 * Note that it is better if only the prepared
									 * result-buffers are needed because they can be reused.
									 * So always set MAX_DECOMPRESSION_OUTPUT appropriate to avoid this case. 
									 */
									if(bout == null)
									{
										bout = new ByteArrayOutputStream();
										buffer.copyLeadingData(bout);
										bout.write(decompressionKit.getResult(), leadingDataLength, inflateLength);
									}
									//this time: use the whole buffer
									inflateLength = decompressionKit.getResult().length;
									resultLength = inflater.inflate(decompressionKit.getResult());
									bout.write(decompressionKit.getResult(), 0, resultLength);
								}
								
								if(bout == null)
								{
									buffer.copyLeadingData(decompressionKit.getResult());
									buffer.copyTrailingData(decompressionKit.getResult(), (leadingDataLength+resultLength));
								
									buffer.setChunk(decompressionKit.getResult());
									buffer.setModificationStartOffset(leadingDataLength+resultLength);
									buffer.setModificationEndOffset(buffer.getModificationStartOffset());
									buffer.setPayloadLength(buffer.getModificationStartOffset() + trailingDataLength);
								}
								else
								{
									buffer.copyTrailingData(bout);
									byte[] newChunk = bout.toByteArray();
									buffer.setChunk(newChunk);
									buffer.setModificationStartOffset(newChunk.length - trailingDataLength);
									buffer.setModificationEndOffset(buffer.getModificationStartOffset());
								}
								
								if(inflater.finished())
								{
									LogHolder.log(LogLevel.INFO, LogType.NET,
											"finish connection after decompressing.");
									inflater.reset();
									buffer.setStatus(STATUS_FINISHED);
									return STATUS_FINISHED;
								}
							}
						}
					}
					catch ( DataFormatException e) 
					{
						if(inflater != null) inflater.reset();
						LogHolder.log(LogLevel.WARNING, LogType.NET,
								"compressed data has invalid format.", e);
					}
					catch (ArrayIndexOutOfBoundsException e) 
					{
						if(inflater != null) inflater.reset();
						LogHolder.log(LogLevel.ERR, LogType.NET, 
								"indexing error occured while decompressing data." +
								" Maybe the result buffer is too small?", e);
					}
				}
			}
		}
		return STATUS_PROCESSABLE;
	}

	public int handleUpstreamChunk(AnonProxyRequest anonRequest,
			ProxyCallbackBuffer buffer)
			throws ProxyCallbackNotProcessableException 
	{
		//ignore upstream traffic
		return STATUS_PROCESSABLE;
	}

	private static class DecompressionKit
	{
		private Inflater gzipInflater = null;
		private Inflater zLibInflater = null;
		private byte[] result = null;
	
		public byte[] getResult()
		{
			return result;
		}
		
		public void setResult(byte[] result)
		{
			this.result = result;
		}
		
		public Inflater getGzipInflater() 
		{
			return gzipInflater;
		}
		
		public void setGzipInflater(Inflater gzipInflater) 
		{
			this.gzipInflater = gzipInflater;
		}
		
		public Inflater getZLibInflater() 
		{
			return zLibInflater;
		}
		
		public void setZLibInflater(Inflater libInflater) 
		{
			zLibInflater = libInflater;
		}
		
		private void setNewInflater(boolean gzipInflater)
		{
			if(gzipInflater) setGzipInflater(new Inflater(true));
			else setZLibInflater(new Inflater());
		}
	}
	
}
