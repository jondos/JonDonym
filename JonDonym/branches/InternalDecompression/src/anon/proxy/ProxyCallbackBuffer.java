package anon.proxy;

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
			throw new IllegalArgumentException("Illegal modification start index: "+modificationStartOffset+" (chunk length: "+chunk.length+")");
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
			throw new IllegalArgumentException("Illegal modification end index: "+modificationEndOffset+" (chunk length: "+chunk.length+")");
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
			throw new IllegalArgumentException("Illegal payload length: "+payloadLength);
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
}
