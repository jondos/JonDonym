/*
 Copyright (c) 2004, The JAP-Team
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
/*
 * based on tinySSL
 * (http://www.ritlabs.com/en/products/tinyweb/tinyssl.php)
 *
 */
package anon.crypto.tinytls;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.util.Random;
import java.util.Vector;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import anon.crypto.IMyPrivateKey;
import anon.crypto.IMyPublicKey;
import anon.crypto.JAPCertificate;
import anon.crypto.MyDSAPrivateKey;
import anon.crypto.MyDSASignature;
import anon.crypto.MyRSAPrivateKey;
import anon.crypto.MyRSASignature;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.ListenerInterface;
import anon.shared.ProxyConnection;
import anon.crypto.tinytls.ciphersuites.CipherSuite;
import anon.crypto.tinytls.ciphersuites.DHE_DSS_WITH_3DES_CBC_SHA;
import anon.crypto.tinytls.ciphersuites.DHE_DSS_WITH_AES_128_CBC_SHA;
import anon.crypto.tinytls.ciphersuites.DHE_DSS_WITH_DES_CBC_SHA;
import anon.crypto.tinytls.ciphersuites.DHE_RSA_WITH_3DES_CBC_SHA;
import anon.crypto.tinytls.ciphersuites.DHE_RSA_WITH_AES_128_CBC_SHA;
import anon.crypto.tinytls.ciphersuites.DHE_RSA_WITH_DES_CBC_SHA;
import anon.crypto.tinytls.util.hash;
import anon.util.ByteArrayUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * @author stefan
 *
 *TinyTLS
 */
public class TinyTLS extends Socket
{

	/**
	 * SSL VERSION :
	 *
	 * 3.1 for TLS
	 */
	public static byte[] PROTOCOLVERSION = new byte[]
		{
		0x03, 0x01};

	private static int PROTOCOLVERSION_SHORT = 0x0301;
	private Vector m_supportedciphersuites;
	private CipherSuite m_selectedciphersuite = null;

	private TLSInputStream m_istream;
	private TLSOutputStream m_ostream;

	private boolean m_handshakecompleted;
	private boolean m_serverhellodone;
	private boolean m_certificaterequested;

	private JAPCertificate m_servercertificate;
	private IMyPublicKey m_trustedRoot;
	private boolean m_checkTrustedRoot;
	//private DHParameters m_dhparams;
	//private DHPublicKeyParameters m_dhserverpub;
	//private byte[] m_serverparams;
	private byte[] m_clientrandom;
	private byte[] m_serverrandom;
	private byte[] m_handshakemessages;
	private byte[] m_clientcertificatetypes;
	private IMyPrivateKey m_clientprivatekey;
	private JAPCertificate[] m_clientcertificates;
	private boolean m_certificateverify;
	private boolean m_encrypt;
	private ProxyConnection m_ProxyConnection;

	/**
	 * This needs to be an interface for compilation in JDK 1.1.8.
	 */
	private interface ITLSConstants
	{
		static final int STATE_START = 0;
		static final int STATE_VERSION = 1;
		static final int STATE_LENGTH = 2;
		static final int STATE_PAYLOAD = 3;
	}

	/**
	 *
	 * @author stefan
	 *
	 * TLSInputStream
	 */
	class TLSInputStream extends InputStream implements ITLSConstants
	{

		//private boolean m_closed;
		private DataInputStream m_stream;
		private int m_aktPendOffset; //offest of next data to deliver
		private int m_aktPendLen; // number of bytes we could deliver imedialy
		private TLSPlaintextRecord m_aktTLSRecord;
		private int m_ReadRecordState;

		/**
		 * Constructor
		 * @param istream inputstream
		 */
		public TLSInputStream(InputStream istream)
		{
			m_aktTLSRecord = new TLSPlaintextRecord();
			m_stream = new DataInputStream(istream);
			m_aktPendOffset = 0;
			m_aktPendLen = 0;
			m_ReadRecordState = STATE_START;
		}

		/**
		 * Reads one record if we need more data...
		 * Block until data is available...
		 * @return
		 */
		private synchronized void readRecord() throws IOException
		{
			if (m_ReadRecordState == STATE_START)
			{
				m_aktTLSRecord.clean();
				int contenttype;
				try
				{
					contenttype = m_stream.readByte();
				}
				catch (InterruptedIOException ioe)
				{
					ioe.bytesTransferred = 0;
					throw ioe;
				}

				if (contenttype < 20 || contenttype > 23)
				{
					throw new TLSException("SSL Content typeProtocoll not supported: " + contenttype);
				}
				m_aktTLSRecord.setType(contenttype);
				m_ReadRecordState = STATE_VERSION;
			}
			if (m_ReadRecordState == STATE_VERSION)
			{
				int version;
				try
				{
					version = m_stream.readShort();
				}
				catch (InterruptedIOException ioe)
				{
					ioe.bytesTransferred = 0;
					throw ioe;
				}
				if (version != PROTOCOLVERSION_SHORT)
				{
					throw new TLSException("Protocol version not supported: " + version);
				}
				m_ReadRecordState = STATE_LENGTH;
			}
			if (m_ReadRecordState == STATE_LENGTH)
			{
				int length=0;
				try
				{
					length = m_stream.readShort();
				}
				catch (InterruptedIOException ioe)
				{
					ioe.bytesTransferred = 0;
					throw ioe;
				}
				if(length>TLSPlaintextRecord.MAX_PAYLOAD_SIZE)
				{
					throw new TLSException("Given size of TLSPlaintex record payload exceeds TLSPlaintextRecord.MAX_PAYLOAD_SIZE!");
				}
				m_aktTLSRecord.setLength(length);
				m_ReadRecordState = STATE_PAYLOAD;
				m_aktPendOffset = 0;
			}
			if (m_ReadRecordState == STATE_PAYLOAD)
			{
				int len = m_aktTLSRecord.getLength() - m_aktPendOffset;
				while (len > 0)
				{
					try
					{
						byte[] buff = m_aktTLSRecord.getData();
						int ret = m_stream.read(buff, m_aktPendOffset, len);
						if (ret < 0)
						{
							throw new EOFException();
						}
						len -= ret;
						m_aktPendOffset += ret;
					}
					catch (InterruptedIOException ioe)
					{
						m_aktPendOffset += ioe.bytesTransferred;
						ioe.bytesTransferred = 0;
						throw ioe;
					}
				}
				m_ReadRecordState = STATE_START;
				m_aktPendOffset = 0;
			}
		}

		public int read() throws IOException
		{
			byte[] b = new byte[1];
			if (read(b, 0, 1) < 1)
			{
				return -1;
			}
			return (b[0] & 0x00FF);
		}

		public int read(byte[] b) throws IOException
		{
			return read(b, 0, b.length);
		}

		public int read(byte[] b, int off, int len) throws IOException
		{
			while (m_aktPendLen < 1)
			{
				/*try
				{*/
					readRecord();
				/*}
				catch(EOFException eofe)
				{
					return -1;
				}*/
				try
				{
					switch (m_aktTLSRecord.getType())
					{
						case 23:
						{
							m_selectedciphersuite.decode(m_aktTLSRecord);
							m_aktPendOffset = 0;
							m_aktPendLen = m_aktTLSRecord.getLength();
							break;
						}
						case 21:
						{
							handleAlert();
							break;
						}
						default:
						{
							throw new IOException("Error while decoding application data");
						}
					}

				}
				catch (Throwable t)
				{
					throw new IOException("Exception by reading next TSL record: " + t.getMessage());
				}
			}
			int l = Math.min(m_aktPendLen, len);
			System.arraycopy(m_aktTLSRecord.getData(), m_aktPendOffset, b, off, l);
			m_aktPendOffset += l;
			m_aktPendLen -= l;
			return l;
		}

		public int available()
		{
			return m_aktPendLen;
		}

		/**
		 * process server hello
		 * @param bytes server hello message
		 * @throws IOException
		 */
		private void gotServerHello(TLSHandshakeRecord msg) throws IOException
		{
			byte[] b;
			int aktIndex = 0;
			byte[] dataBuff = msg.getData();

			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_HELLO] SSLVERSION :" + dataBuff[aktIndex] + "." + dataBuff[aktIndex +
						  1]);
			if ( (dataBuff[aktIndex] != PROTOCOLVERSION[0]) ||
				(dataBuff[aktIndex + 1] != PROTOCOLVERSION[1]))
			{
				throw new TLSException("Server replies with wrong protocoll");
			}
			m_serverrandom = ByteArrayUtil.copy(dataBuff, aktIndex + 2, 32);
			byte[] sessionid = new byte[0];
			int sessionidlength = dataBuff[aktIndex + 34];
			if (sessionidlength > 0)
			{
				sessionid = ByteArrayUtil.copy(dataBuff, aktIndex + 35, sessionidlength);
			}
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_HELLO] Laenge der SessionID : " + sessionidlength);
			byte[] ciphersuite = ByteArrayUtil.copy(dataBuff, aktIndex + 35 + sessionidlength, 2);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_HELLO] Ciphersuite : " + ciphersuite[0] + " " + ciphersuite[1]);
			byte[] compression = ByteArrayUtil.copy(dataBuff, aktIndex + 37 + sessionidlength, 1);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_HELLO] Kompression : " + compression[0]);
			CipherSuite cs = null;
			for (int i = 0; i < m_supportedciphersuites.size(); i++)
			{
				cs = (CipherSuite) m_supportedciphersuites.elementAt(i);
				b = cs.getCipherSuiteCode();
				if ( (b[0] == ciphersuite[0]) && (b[1] == ciphersuite[1]))
				{
					break;
				}
				cs = null;
			}
			if (cs == null)
			{
				throw new TLSException("Unsupported Ciphersuite selected");
			}
			m_selectedciphersuite = cs;
			m_supportedciphersuites = null;
		}

		/**
		 * process server certificate
		 * @param bytes server certificate message
		 * @throws IOException
		 */
		private void gotCertificate(TLSHandshakeRecord msg) throws IOException
		{
			byte[] bytes = msg.getData();
			int offset = 0;
			int len = msg.getLength();
			Vector certificates = new Vector();
			byte[] b = ByteArrayUtil.copy(bytes, offset, 3);
			int certificateslength = ( (b[0] & 0xFF) << 16) | ( (b[1] & 0xFF) << 8) | (b[2] & 0xFF);
			int pos = offset + 3;
			b = ByteArrayUtil.copy(bytes, pos, 3);
			pos += 3;
			int certificatelength = ( (b[0] & 0xFF) << 16) | ( (b[1] & 0xFF) << 8) | (b[2] & 0xFF);
			b = ByteArrayUtil.copy(bytes, pos, certificatelength);
			pos += certificatelength;
			JAPCertificate japcert = JAPCertificate.getInstance(b);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_CERTIFICATE] " + japcert.getIssuer().toString());
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_CERTIFICATE] " + japcert.getSubject().toString());
			m_servercertificate = japcert;
			m_selectedciphersuite.setServerCertificate(japcert);
			//certificates.addElement(japcert);
			while (pos - offset < certificateslength)
			{
				b = ByteArrayUtil.copy(bytes, pos, 3);
				pos += 3;
				certificatelength = ( (b[0] & 0xFF) << 16) | ( (b[1] & 0xFF) << 8) | (b[2] & 0xFF);
				b = ByteArrayUtil.copy(bytes, pos, certificatelength);
				pos += certificatelength;
				japcert = JAPCertificate.getInstance(b);
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "[NEXT_CERTIFICATE] " + japcert.getIssuer().toString());
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "[NEXT_CERTIFICATE] " + japcert.getSubject().toString());
				certificates.addElement(japcert);
			}
			//check certificates chain...
			JAPCertificate prevCert = m_servercertificate;
			for (int i = 0; i < certificates.size(); i++)
			{
				JAPCertificate cert = (JAPCertificate) certificates.elementAt(i);
				if (!prevCert.verify(cert.getPublicKey()))
				{
					throw new IOException("TLS Server Certs could not be verified!");
				}
				prevCert = cert;
			}
			//last certificat is checked, if a certificatestore is set
			if (m_checkTrustedRoot)
			{
				if (!prevCert.verify(m_trustedRoot))
				{
					throw new IOException("TLS Server Cert could not be verified to be trusted!");
				}
			}
		}

		/**
		 * process server key exchange message
		 * @param bytes server key exchange message
		 * @throws IOException
		 * @throws Exception
		 */
		private void gotServerKeyExchange(TLSHandshakeRecord msg) throws IOException
		{
			byte[] bytes = msg.getData();
			int offset = 0;
			int len = msg.getLength();
			m_selectedciphersuite.getKeyExchangeAlgorithm().processServerKeyExchange(bytes, offset, len,
				m_clientrandom, m_serverrandom, m_servercertificate);
		}

		/**
		 * handle certificate request
		 * @param bytes certificate request message
		 */
		private void gotCertificateRequest(TLSHandshakeRecord msg)
		{
			byte[] bytes = msg.getData();
			int offset = 0;
			int len = msg.getLength();

			m_certificaterequested = true;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_CERTIFICATE_REQUEST]");
			int length = bytes[offset];
			if (length > 0) //at least one client certificate type
			{
				m_clientcertificatetypes = ByteArrayUtil.copy(bytes, offset + 1, length);
				//the rest of this message contains distinguishedNames of certificate authorities
				//see RFC2246 - 7.4.4 Certificate Request
			}
		}

		/**
		 * handle server hello done message
		 * @param bytes server hello done message
		 */
		private void gotServerHelloDone()
		{
			m_serverhellodone = true;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_HELLO_DONE]");
		}

		/**
		 * handle alert message
		 * @throws IOException
		 */
		private void handleAlert() throws IOException
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[TLS] ALERT!");
			if (m_handshakecompleted)
			{
				m_selectedciphersuite.decode(m_aktTLSRecord);
			}
			byte[] payload = m_aktTLSRecord.getData();
			switch (payload[0])
			{
				// warning
				case 1:
				{
					switch (payload[1])
					{
						case 0:
						{
							LogHolder.log(LogLevel.DEBUG, LogType.MISC,
										  "[RECIEVED-ALERT] TYPE=WARNING ; MESSAGE=CLOSE NOTIFY");
							//LogHolder.log(LogLevel.DEBUG, LogType.MISC,"[RECIEVED-ALERT] TYPE=WARNING ; MESSAGE=CLOSE NOTIFY");
							//m_closed = true;
							break;
						}
						default:
						{
							throw new TLSException("TLSAlert detected!! Level : Warning - Description :" +
								payload[1]);
						}
					}
					break;
				}
				// fatal
				case 2:
				{
					throw new TLSException("TLSAlert detected!! Level : Fatal - Description :" + payload[1]);
				}
				default:
				{
					throw new TLSException("Unknown TLSAlert detected!!");
				}
			}
		}

		/**
		 * read the server handshakes and handle alerts
		 * @throws IOException
		 * @throws CertificateException
		 * @throws Exception
		 * @todo VERY buggy - does not right handle the differnt layers of records!!!
		 */
		protected void readServerHandshakes() throws IOException
		{
			TLSHandshakeRecord handshakeRecord;
			while (!m_serverhellodone)
			{
				if (!m_aktTLSRecord.hasMoreHandshakeRecords())
				{
					readRecord();
					switch (m_aktTLSRecord.getType())
					{
						case 21:
						{
							handleAlert();
							break;
						}
						case TLSPlaintextRecord.CONTENTTYPE_HANDSHAKE:
						{
							break;
						}
						default:
						{
							throw new TLSException("Error while shaking hands");
						}
					}
				}
				handshakeRecord = m_aktTLSRecord.getNextHandshakeRecord();
				byte[] dataBuff = handshakeRecord.getData();

				int type = handshakeRecord.getType();
				int length = handshakeRecord.getLength();
				m_handshakemessages = ByteArrayUtil.conc(m_handshakemessages, handshakeRecord.getHeader(),TLSHandshakeRecord.HEADER_LENGTH);
				m_handshakemessages = ByteArrayUtil.conc(m_handshakemessages, dataBuff,length);
				switch (type)
				{
					//Server hello
					case 2:
					{
						gotServerHello(handshakeRecord);
						break;
					}
					//certificate
					case 11:
					{
						gotCertificate(handshakeRecord);
						break;
					}
					//server key exchange
					case 12:
					{
						gotServerKeyExchange(handshakeRecord);
						break;
					}
					//certificate request
					case 13:
					{
						gotCertificateRequest(handshakeRecord);
						break;
					}
					//server hello done
					case 14:
					{
						gotServerHelloDone();
						break;
					}
					default:
					{
						throw new TLSException("Unexpected Handshake type: " + type);
					}
				}
			}

		}

		/**
		 * wait for server finished message
		 *
		 */
		protected void readServerFinished() throws IOException
		{
			readRecord();
			switch (m_aktTLSRecord.getType())
			{
				case 20:
				{
					if (m_aktTLSRecord.getLength() == 1 && m_aktTLSRecord.getData()[0] == 1)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_CHANGE_CIPHER_SPEC]");
					}
					break;
				}
				case 21:
				{
					handleAlert();
					break;
				}
				default:
				{
					throw new TLSException("Error while shaking hands");
				}
			}
			readRecord();

			switch (m_aktTLSRecord.getType())
			{
				case TLSPlaintextRecord.CONTENTTYPE_HANDSHAKE:
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_FINISHED]");
					m_selectedciphersuite.processServerFinished(m_aktTLSRecord, m_handshakemessages);
					break;
				}
				case 21:
				{
					this.handleAlert();
					break;
				}
				default:
				{
					throw new TLSException("Error while shaking hands");
				}
			}
		}

	}

	/**
	 *
	 * @author stefan
	 *
	 *TLSOutputStream
	 */
	class TLSOutputStream extends OutputStream
	{

		private OutputStream m_stream;
		private TLSPlaintextRecord m_aktTLSRecord;

		/**
		 * Constructor
		 * @param ostream outputstream
		 */
		public TLSOutputStream(OutputStream ostream)
		{
			m_aktTLSRecord = new TLSPlaintextRecord();
			m_stream = ostream;
		}

		/**
		 *
		 */
		public void write(byte[] message) throws IOException
		{
			this.send(23, message, 0, message.length);
		}

		/**
		 *
		 */
		public void write(byte[] message, int offset, int len) throws IOException
		{
			this.send(23, message, offset, len);
		}

		/**
		 *
		 */
		public void write(int i) throws IOException
		{
			write(new byte[]
				  { (byte) i});
		}

		public void close() throws IOException
		{
			sendCloseNotify();
			m_stream.close();
		}

		/**
		 *
		 */
		public void flush() throws IOException
		{
			m_stream.flush();
		}

		/**
		 * send a message to the server
		 * @param type type of the tls message
		 * @param message message
		 * @throws IOException
		 */
		private synchronized void send(int type, byte[] message, int offset, int len) throws IOException
		{
			byte[] dataBuff = m_aktTLSRecord.getData();
			System.arraycopy(message, offset, dataBuff, 0, len);
			m_aktTLSRecord.setLength(len);
			m_aktTLSRecord.setType(type);
			if (m_encrypt)
			{
				m_selectedciphersuite.encode(m_aktTLSRecord);
			}
			m_stream.write(m_aktTLSRecord.getHeader());
			m_stream.write(dataBuff, 0, m_aktTLSRecord.getLength());
			m_stream.flush();
		}

		/**
		 * send a handshake message to the server
		 * @param type handshake type
		 * @param message message
		 * @throws IOException
		 */
		public void sendHandshake(int type, byte[] message) throws IOException
		{
			byte[] senddata = ByteArrayUtil.conc(new byte[]
												 { (byte) type}
												 , ByteArrayUtil.inttobyte(message.length, 3), message);
			send(22, senddata, 0, senddata.length);
			m_handshakemessages = ByteArrayUtil.conc(m_handshakemessages, senddata);
		}

		/**
		 * send a client hello message
		 * @throws IOException
		 */
		public void sendClientHello() throws IOException
		{
			byte[] gmt_unix_time;
			byte[] random = new byte[28];
			byte[] sessionid = new byte[]
				{
				0x00};
			byte[] ciphers = new byte[m_supportedciphersuites.size() * 2];
			int counter = 0;
			for (int i = 0; i < m_supportedciphersuites.size(); i++)
			{
				CipherSuite cs = (CipherSuite) m_supportedciphersuites.elementAt(i);
				ciphers[counter] = cs.getCipherSuiteCode()[0];
				counter++;
				ciphers[counter] = cs.getCipherSuiteCode()[1];
				counter++;

			}
			byte[] ciphersuites = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(m_supportedciphersuites.size() *
				2, 2),
				ciphers);
			byte[] compression = new byte[]
				{
				0x01, 0x00};

			gmt_unix_time = ByteArrayUtil.inttobyte( (System.currentTimeMillis() / (long) 1000), 4);
			Random rand = new Random(System.currentTimeMillis());
			rand.nextBytes(random);

			byte[] message = ByteArrayUtil.conc(PROTOCOLVERSION, gmt_unix_time, random, sessionid,
												ciphersuites,
												compression);

			sendHandshake(1, message);
			m_clientrandom = ByteArrayUtil.conc(gmt_unix_time, random);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_HELLO]");
		}

		/**
		 * send a client certificate message
		 * @throws IOException
		 */
		public void sendClientCertificate() throws IOException
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_CERTIFICATE]");
			if (m_certificaterequested)
			{
				if (m_clientcertificatetypes != null && m_clientcertificates != null)
				{
					for (int i = 0; i < m_clientcertificatetypes.length; i++)
					{
						switch (m_clientcertificatetypes[i])
						{
							case 1: //rsa_sign
							{
								byte[] b = new byte[0];
								for (int i2 = 0; i2 < m_clientcertificates.length; i2++)
								{
									byte[] cert = m_clientcertificates[i2].toByteArray(false);
									b = ByteArrayUtil.conc(b, ByteArrayUtil.inttobyte(cert.length, 3), cert);
								}
								b = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(b.length, 3), b);
								this.sendHandshake(11, b);
								m_certificateverify = true;
								return;
							}
							case 2: //dss_sign
							{
								byte[] b = new byte[0];
								for (int i2 = 0; i2 < m_clientcertificates.length; i2++)
								{
									byte[] cert = m_clientcertificates[i2].toByteArray(false);
									b = ByteArrayUtil.conc(b, ByteArrayUtil.inttobyte(cert.length, 3), cert);
								}
								b = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(b.length, 3), b);
								this.sendHandshake(11, b);
								m_certificateverify = true;
								return;
							}
							case 3: //rsa_fixed_dh
							{
								break;
							}
							case 4: //dss_fixed_dh
							{
								break;
							}
						}
					}
				}
				//no certificate available
				else
				{
					sendHandshake(11, new byte[]
								  {0, 0, 0});
				}
			}
		}

		/**
		 * send a client key exchange message
		 * @throws IOException
		 */
		public void sendClientKeyExchange() throws IOException
		{
			byte[] message = m_selectedciphersuite.calculateClientKeyExchange();
			sendHandshake(16, ByteArrayUtil.conc(ByteArrayUtil.inttobyte(message.length, 2), message));
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_KEY_EXCHANGE]");
		}

		/**
		 * send a certificate verify message if a certificate is used
		 * @throws IOException
		 */
		public void sendCertificateVerify() throws IOException
		{
			if (m_certificateverify)
			{
				if (m_clientprivatekey instanceof MyRSAPrivateKey)
				{
					byte[] signature = ByteArrayUtil.conc(
						hash.md5(m_handshakemessages),
						hash.sha(m_handshakemessages));
					MyRSASignature sig = new MyRSASignature();
					try
					{
						sig.initSign(m_clientprivatekey);
					}
					catch (InvalidKeyException ex)
					{
						throw new TLSException("cannot encrypt signature", 2, 80);
					}
					byte[] signature2 = sig.signPlain(signature);

					signature2 = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(signature2.length, 2), signature2);
					sendHandshake(15, signature2);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_CERTIFICATE_VERIFY_RSA]");
				}
				else if (m_clientprivatekey instanceof MyDSAPrivateKey)
				{
					MyDSASignature sig = new MyDSASignature();
					try
					{
						sig.initSign(m_clientprivatekey);
					}
					catch (InvalidKeyException ex)
					{
					}
					byte[] signature2 = sig.sign(m_handshakemessages);

					signature2 = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(signature2.length, 2), signature2);
					sendHandshake(15, signature2);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_CERTIFICATE_VERIFY_DSA]");
				}

			}
		}

		/**
		 * send a change cipher spec message
		 * now all client data will be encrypted
		 * @throws IOException
		 */
		public void sendChangeCipherSpec() throws IOException
		{
			send(20, new byte[]
				 {1}
				 , 0, 1);
			m_encrypt = true;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_CHANGE_CIPHER_SPEC]");
		}

		/**
		 * send a close notify message to inform the peer that this connection will be closed now message
		 * @throws IOException
		 */
		public void sendCloseNotify() throws IOException
		{
			send(21, new byte[]
				 {1, 0}
				 , 0, 2);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_CLOSE_NOTIFY]");
		}

		/**
		 * send a client finished message
		 * @throws IOException
		 */
		public void sendClientFinished() throws IOException
		{
			sendHandshake(20,
						  m_selectedciphersuite.getKeyExchangeAlgorithm().calculateClientFinished(
							  m_handshakemessages));
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_FINISHED]");
		}
	}

	/**
	 *
	 * TinyTLS creates a TLS Connection to a server
	 *
	 * @param addr
	 * Server Address
	 * @param port
	 * Server's TLS Port
	 */
	public TinyTLS(String addr, int port) throws UnknownHostException, IOException, Exception
	{
		this(addr, port, null);
	}

	/**
	 *
	 * TinyTLS creates a TLS Connection to a server which may use a proxy
	 *
	 * @param addr
	 * Server Address
	 * @param port
	 * Server's TLS Port
	 * @param a_proxyInterface Proxy Settings
	 */
	public TinyTLS(String addr, int port, ImmutableProxyInterface a_proxyInterface) throws
		UnknownHostException, IOException, Exception
	{
		m_ProxyConnection = new ProxyConnection(HTTPConnectionFactory.getInstance().createHTTPConnection(new
			ListenerInterface(addr, port), a_proxyInterface).Connect());
		//super(addr, port);
		m_handshakecompleted = false;
		m_serverhellodone = false;
		m_encrypt = false;
		m_certificaterequested = false;
		m_certificateverify = false;
		m_supportedciphersuites = new Vector();
		m_istream = new TLSInputStream(m_ProxyConnection.getInputStream());
		m_ostream = new TLSOutputStream(m_ProxyConnection.getOutputStream());
		m_trustedRoot = null;
		m_checkTrustedRoot = true;
		m_clientcertificatetypes = null;
		m_clientcertificates = null;
		m_clientprivatekey = null;
	}

	/**
	 * add a ciphersuites to TinyTLS
	 * @param cs ciphersuite you want to add
	 */
	public void addCipherSuite(CipherSuite cs)
	{
		if (!this.m_supportedciphersuites.contains(cs))
		{
			this.m_supportedciphersuites.addElement(cs);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CIPHERSUITE_ADDED] : " + cs.toString());
		}
	}

	/**
	 * start the handshake
	 * @throws IOException
	 * @throws CertificateException
	 * @throws Exception
	 */
	public void startHandshake() throws IOException
	{
		if (m_supportedciphersuites.isEmpty())
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[NO_CIPHERSUITE_DEFINED] : using predefined");
			this.addCipherSuite(new DHE_RSA_WITH_AES_128_CBC_SHA());
			this.addCipherSuite(new DHE_DSS_WITH_AES_128_CBC_SHA());
			this.addCipherSuite(new DHE_RSA_WITH_3DES_CBC_SHA());
			this.addCipherSuite(new DHE_DSS_WITH_3DES_CBC_SHA());
			this.addCipherSuite(new DHE_RSA_WITH_DES_CBC_SHA());
			this.addCipherSuite(new DHE_DSS_WITH_DES_CBC_SHA());
		}
		if (!m_checkTrustedRoot)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[CHECK_TRUSTED_ROOT_DEACTIVATED] : all certificates are accepted");
		}
		else if (m_trustedRoot == null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[TRUSTED_CERTIFICATES_NOT_SET] : cannot verify Certificates");
			throw new TLSException("Please set Trusted Root");
		}
		this.m_handshakemessages = new byte[]
			{};
		this.m_ostream.sendClientHello();
		this.m_istream.readServerHandshakes();
		this.m_ostream.sendClientCertificate();
		this.m_ostream.sendClientKeyExchange();
		this.m_ostream.sendCertificateVerify();
		this.m_ostream.sendChangeCipherSpec();
		this.m_ostream.sendClientFinished();
		this.m_istream.readServerFinished();
		this.m_handshakecompleted = true;
	}

	/**
	 * sets the root key that is accepted
	 * @param rootKey
	 * rootkey
	 */
	public void setRootKey(IMyPublicKey rootKey)
	{
		m_trustedRoot = rootKey;
	}

	/**
	 * check or check not the root certificate
	 * @param check
	 *
	 */
	public void checkRootCertificate(boolean check)
	{
		m_checkTrustedRoot = check;
	}

	public InputStream getInputStream()
	{
		return this.m_istream;
	}

	public OutputStream getOutputStream()
	{
		return this.m_ostream;
	}

	public void setSoTimeout(int i) throws SocketException
	{
		m_ProxyConnection.setSoTimeout(i);
	}

	/**
	 * sets the client certificate
	 * @param cert
	 * certificate
	 * @param key
	 * private key
	 * @throws IOException
	 */
	public void setClientCertificate(JAPCertificate cert, IMyPrivateKey key) throws IOException
	{
		setClientCertificate(new JAPCertificate[]
							 {cert}, key);
	}

	/**
	 * sets a client certificate chain
	 * @param certificates
	 * certificate chain, where the previous certificate is signed with the following
	 * @param key
	 * private key
	 * @throws IOException
	 */
	public void setClientCertificate(JAPCertificate[] certificates, IMyPrivateKey key) throws IOException
	{
		if (certificates != null)
		{
			JAPCertificate prevCert = certificates[0];
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[CLIENT_CERTIFICATE] " + prevCert.getIssuer().toString());
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[CLIENT_CERTIFICATE] " + prevCert.getSubject().toString());
			JAPCertificate cert;
			for (int i = 1; i < certificates.length; i++)
			{
				cert = certificates[i];
				if (!prevCert.verify(cert.getPublicKey()))
				{
					throw new IOException("TLS Server Certs could not be verified!");
				}
				prevCert = cert;
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "[CLIENT_CERTIFICATE] " + prevCert.getIssuer().toString());
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "[CLIENT_CERTIFICATE] " + prevCert.getSubject().toString());
			}
		}
		m_clientcertificates = certificates;
		m_clientprivatekey = key;
	}

	public void close()
	{
		try
		{
			if (m_ostream != null)
			{
				m_ostream.close();
			}
		}
		catch (IOException ex)
		{
		}
		try
		{
			if (m_istream != null)
			{
				m_istream.close();
			}
		}
		catch (IOException ex)
		{
		}

		m_ProxyConnection.close();
	}

	public Socket getSocket()
	{
		return m_ProxyConnection.getSocket();
	}
}
