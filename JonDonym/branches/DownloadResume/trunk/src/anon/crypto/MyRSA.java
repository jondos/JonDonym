package anon.crypto;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.encodings.OAEPEncoding;


/** Encryption/Decryption using RSA*/
public class MyRSA
{
	RSAEngine m_RSAEngine;
	OAEPEncoding m_OAEP;

	public MyRSA()
	{
		m_RSAEngine = new RSAEngine();
		
		m_OAEP=new OAEPEncoding(m_RSAEngine);
		
		
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
			m_OAEP.init(true,key.getParams());
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


}
