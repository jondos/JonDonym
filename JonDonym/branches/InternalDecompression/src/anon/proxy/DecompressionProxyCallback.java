package anon.proxy;

import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class DecompressionProxyCallback implements ProxyCallback
{
	Hashtable decompressionKits = new Hashtable();

	/**
	 * these constants are copied from GZIPInputstream
	 * because they are declared private there.
	 */
	private final static int FTEXT	= 1;	// Extra text
	private final static int FHCRC	= 2;	// Header CRC
	private final static int FEXTRA	= 4;	// Extra field
	private final static int FNAME	= 8;	// File name
	private final static int FCOMMENT	= 16;	// File comment
	
	private final int MAX_DECOMPRESSION_OUTPUT = 40000;
	
	/**
	 * slightly modified version of private method GZIPInputStream.readHeader to extract
	 * the GZIP header fields.
	 * returns the length of the header in bytes.
	 */
	private int readGZIPHeader(byte[] data, int offset, int length) 
			throws DataFormatException
	{
		int index = offset;
		int headerMagic = toUShort(data[index++], data[index++]);
		if (headerMagic != GZIPInputStream.GZIP_MAGIC) 
		{
		    throw new DataFormatException("Not in GZIP format");
		}
		// Check compression method
		if (toUByte(data[index++]) != 8) 
		{
		    throw new DataFormatException("Unsupported compression method");
		}
		// Read flags
		int flg = toUByte(data[index++]);
		// Skip MTIME, XFL, and OS fields
		index += 6;
		// Skip optional extra field
		if ((flg & FEXTRA) == FEXTRA) 
		{
			index += toUShort(data[index++], data[index++]);
		}
		// Skip optional file name
		if ((flg & FNAME) == FNAME) 
		{		
			while (toUShort(data[index++], data[index++]) != 0) ;
		}
		// Skip optional file comment
		if ((flg & FCOMMENT) == FCOMMENT) 
		{
			 while (toUShort(data[index++], data[index++]) != 0) ;
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
	private int toUShort(byte lower, byte upper) 
	{
		 return (toUByte(upper) << 8) | lower;
	}

	private int toUByte(byte nByte)
	{
		 return ((nByte < 0) ? 128 : 0) + (0x7F & nByte);
	}
	    
	public void closeRequest(AnonProxyRequest anonRequest) 
	{
		
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
					Hashtable inflatersTable = null;
					
					Inflater inflater = null;
					DecompressionKit decompressionKit = null;
					byte[] decompressionInput = null;
					
					int resultLength = 0;
					int oldStartOffset = 0;
					int oldEndOffset = 0;
					int oldPayloadLength = 0;
					int trailingDataLength = 0;
					
					try
					{	
						for(int i = 0; i < compressionSequence.size(); i++)
						{
							currentEncoding = (String) compressionSequence.elementAt(i);
							System.out.println("content-encoding "+currentEncoding+" found.");
						
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
								oldStartOffset = buffer.getModificationStartOffset();
								oldEndOffset = buffer.getModificationEndOffset();
								oldPayloadLength = buffer.getPayloadLength();
								trailingDataLength = (oldPayloadLength - (oldEndOffset + 1));
								
								inflater.setInput(buffer.getChunk(), 
										buffer.getModificationStartOffset()+gzipHeaderOffset,
										length-gzipHeaderOffset);
								
								//also consider the leading and trailing space of the 
								//input data buffer
								resultLength = inflater.inflate(
										decompressionKit.getResult(), 
										oldStartOffset, 
										(decompressionKit.getResult().length - trailingDataLength - oldStartOffset) );
								byte[] newChunk = new byte[oldPayloadLength-length+
								                           resultLength+trailingDataLength];
								
								//copy bytes that were modified before this callback was invoked.
								System.arraycopy(buffer.getChunk(), 0, decompressionKit.getResult(), 0, oldStartOffset);
		
								//copy any trailing content that may appear between modificationEndOffset 
								//and the end of the payload area most unlikely case.
								if((oldStartOffset+resultLength) < newChunk.length)
								{
									System.arraycopy(buffer.getChunk(), oldEndOffset, 
											decompressionKit.getResult(), (oldStartOffset+resultLength), 
											trailingDataLength);
								}
								buffer.setChunk(decompressionKit.getResult());
								buffer.setModificationStartOffset(oldStartOffset+resultLength);
								buffer.setModificationEndOffset(buffer.getModificationStartOffset());
								buffer.setPayloadLength(oldStartOffset+resultLength+trailingDataLength);
								
								if(inflater.finished())
								{
									inflater.reset();
								}
							}
							else
							{
								//TODO: chunk is too small to contain all gzip headers.
								System.out.println("need more data ");
							}
						}
					}
					catch ( DataFormatException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					
					}
					catch (ArrayIndexOutOfBoundsException aioobe) 
					{
						// TODO Auto-generated catch block
						aioobe.printStackTrace();
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
		//ignore
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
