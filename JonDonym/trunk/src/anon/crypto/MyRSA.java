package anon.crypto;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;


/** Encryption/Decryption using RSA*/
public class MyRSA
{
	RSAEngine m_RSAEngine;
	OAEPEncoding m_OAEP;
	PKCS1Encoding m_PKCS1;
	public MyRSA()
	{
		m_RSAEngine = new RSAEngine();
		
		m_OAEP=new OAEPEncoding(m_RSAEngine);
		m_PKCS1=new PKCS1Encoding(m_RSAEngine);
		
	}
	
	//by Stefan Roenisch
	public MyRSA(Digest digest)
	{
		m_RSAEngine = new RSAEngine();
		
		m_OAEP=new OAEPEncoding(m_RSAEngine, digest);
		
		
	}

	/** inits the cipher for encryption*/
	public void init(MyRSAPublicKey key) throws Exception
	{
		synchronized (m_RSAEngine)
		{
			m_RSAEngine.init(true, key.getParams());
			m_PKCS1.init(true,key.getParams());
			m_OAEP.init(true,key.getParams());
		}
	}

	/** inits the cipher for decryption*/
	public void init(MyRSAPrivateKey key) throws Exception
	{
		synchronized (m_RSAEngine)
		{
			m_RSAEngine.init(false, key.getParams());
			m_PKCS1.init(false,key.getParams());
			m_OAEP.init(false,key.getParams());
		}
	}

	/** encrypts/decrypts one  block using Plain RSA*/
	public byte[] processBlock(byte[] plain, int offset, int len) throws Exception
	{
		synchronized (m_RSAEngine)
		{
			return m_RSAEngine.processBlock(plain, offset, len);
		}
	}

	/** encrypts/decrypts one  block using OAEP padding*/
	public byte[] processBlockOAEP(byte[] plain, int offset, int len) throws Exception
	{
		synchronized (m_RSAEngine)
		{
			return m_OAEP.encodeBlock(plain, offset, len);
		}
	}

	/** encrypts/decrypts one  block using PKCS1 padding*/
	public byte[] processBlockPKCS1(byte[] plain, int offset, int len) throws Exception
	{
		synchronized (m_RSAEngine)
		{
			return m_PKCS1.processBlock(plain, offset, len);
		}
	}
	
	
}
