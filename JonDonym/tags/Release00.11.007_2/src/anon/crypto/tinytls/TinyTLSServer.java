package anon.crypto.tinytls;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import anon.crypto.IMyPrivateKey;
import anon.crypto.JAPCertificate;
import anon.crypto.MyDSAPrivateKey;
import anon.crypto.MyRSAPrivateKey;

/**
 * @author stefan
 */
public class TinyTLSServer extends ServerSocket
{

	private JAPCertificate m_Certificate = null;
	private IMyPrivateKey m_PrivateKey = null;
	private MyDSAPrivateKey m_DSSKey = null;
	private MyRSAPrivateKey m_RSAKey = null;
	private JAPCertificate m_DSSCertificate = null;
	private JAPCertificate m_RSACertificate = null;
	private TinyTLSServerSocket tls = null;

	/**
	 * Constructor
	 * @param port
	 * port of the TLS Server
	 * @throws IOException
	 */
	public TinyTLSServer(int port) throws IOException
	{
		super(port);
	}

	/**
	 * Constructor
	 * @param port port of the TLS Server
	 * @param backlog the listen backlog
	 * @param bindAddress the local InetAddress the server will bind to
	 * @throws IOException
	 */
	public TinyTLSServer(int port,int backlog,InetAddress bindAddress) throws IOException
	{
		super(port,backlog,bindAddress);
	}

	/**
	 * sets DSS parameters
	 * @param cert
	 * certificate
	 * @param key
	 * private key
	 */
	public void setDSSParameters(JAPCertificate cert, MyDSAPrivateKey key)
	{
		m_DSSCertificate = cert;
		m_DSSKey = key;
	}

	/**
	 * sets RSA parameters
	 * @param cert
	 * certificate
	 * @param key
	 * private key
	 */
	public void setRSAParameters(JAPCertificate cert, MyRSAPrivateKey key)
	{
		m_RSACertificate = cert;
		m_RSAKey = key;
	}

	public Socket accept() throws IOException
	{
		return accept(0);
	}

	public Socket accept(long a_forceCloseAfterMS) throws IOException
	{
		Socket s = super.accept();

		tls = new TinyTLSServerSocket(s, a_forceCloseAfterMS);
		tls.setDSSParameters(m_DSSCertificate, m_DSSKey);
		tls.setRSAParameters(m_RSACertificate, m_RSAKey);
		return tls;
	}

}
