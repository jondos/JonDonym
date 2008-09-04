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
 * Created on Jun 13, 2004
 *
 */
package anon.tor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import anon.crypto.X509DistinguishedName;
import anon.crypto.Validity;
import anon.crypto.JAPCertificate;
import anon.crypto.MyRandom;
import anon.crypto.PKCS12;
import anon.crypto.RSAKeyPair;
import anon.crypto.tinytls.TinyTLS;
import anon.tor.cells.Cell;
import anon.tor.ordescription.ORDescriptor;
import anon.infoservice.IMutableProxyInterface;
import anon.infoservice.ImmutableProxyInterface;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.crypto.MyRSAPublicKey;

/**
 *
 */
public class FirstOnionRouterConnection implements Runnable
{

	//Name of the onionproxy
	private static String OP_NAME = "JAPClient";
	private TinyTLS m_tinyTLS;
	private ORDescriptor m_description;
	private Thread m_readDataLoop;
	private InputStream m_istream;
	private OutputStream m_ostream;
	private Hashtable m_Circuits;
	private volatile boolean m_bRun;
	private boolean m_bIsClosed = true;
	private MyRandom m_rand;
	private Object m_oSendSync;
	private long m_inittimeout;
	private Tor m_Tor;
	private RSAKeyPair m_keypairIdentityKey;

	/**
	 * constructor
	 *
	 * creates a FOR from the description
	 * @param d
	 * description of the onion router
	 * @param a_Tor
	 * a tor instance
	 */
	public FirstOnionRouterConnection(ORDescriptor d, Tor a_Tor)
	{
		m_inittimeout = 30000;
		m_readDataLoop = null;
		m_bRun = false;
		m_bIsClosed = true;
		m_description = d;
		m_rand = new MyRandom(new SecureRandom());
		m_oSendSync = new Object();
		m_Tor = a_Tor;
	}

	/**
	 * returns the description of the onion router
	 * @return
	 * OR description
	 */
	public ORDescriptor getORDescription()
	{
		return m_description;
	}

	/**
	 * check if the connection to the first onion router is closed
	 * @return
	 *
	 */
	public boolean isClosed()
	{
		return m_bIsClosed;
	}

	/**
	 * sends a cell
	 * @param cell
	 * cell with data
	 * @throws IOException
	 */
	public void send(Cell cell) throws IOException
	{
		synchronized (m_oSendSync)
		{
			for (; ; )
			{
				try
				{
					m_ostream.write(cell.getCellData());
					m_ostream.flush();
					LogHolder.log(LogLevel.DEBUG, LogType.TOR,
								  "OnionConnection " + m_description.getName() + " Send a cell");
					break;
				}
				catch (InterruptedIOException ex)
				{
				}
			}
		}
	}

	/**
	 * dispatches a cell to the circuits if one is recieved
	 * @param cell
	 * Cell to dispatch
	 */
	private boolean dispatchCell(Cell cell)
	{
		try
		{
			int cid = cell.getCircuitID();
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "OnionProxy read() Tor Cell - Circuit: " + cid + " Type: " + cell.getCommand());
			Circuit circuit = (Circuit) m_Circuits.get(new Integer(cid));
			if (circuit != null)
			{
				circuit.dispatchCell(cell);
			}
			else
			{
				m_Circuits.remove(new Integer(cid));
			}
			return true;
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	/**
	 * connects to the FOR
	 * @throws Exception
	 */
	public synchronized void connect() throws Exception
	{
		IMutableProxyInterface pi = m_Tor.getProxy();
		ImmutableProxyInterface proxy = null;
		if (pi != null)
		{
			proxy = pi.getProxyInterface(false).getProxyInterface();
		}

		FirstOnionRouterConnectionThread forct =
			new FirstOnionRouterConnectionThread(m_description.getAddress(),
												 m_description.getPort(),
												 m_inittimeout,
												 proxy);
		m_tinyTLS = forct.getConnection();
		m_tinyTLS.setRootKey(m_description.getSigningKey());

		//####create client certificate####

		try
		{
			RSAKeyPair kp = RSAKeyPair.getInstance(new BigInteger(new byte[]
				{1, 0, 1}), new SecureRandom(), 1024, 100);

			JAPCertificate cert = JAPCertificate.getInstance(
				new X509DistinguishedName(X509DistinguishedName.LABEL_COMMON_NAME + "=" + OP_NAME),
				kp, new Validity(Calendar.getInstance(), 1));

			m_keypairIdentityKey = RSAKeyPair.getInstance(new BigInteger(new byte[]
				{1, 0, 1}), new SecureRandom(), 1024, 100);
			PKCS12 pkcs12cert = new PKCS12(
				new X509DistinguishedName(X509DistinguishedName.LABEL_COMMON_NAME + "=" + OP_NAME +
										  " <identity>"),
				m_keypairIdentityKey, new Validity(Calendar.getInstance(), 1));

			// sign cert with pkcs12cert
			JAPCertificate cert1 = cert.sign(pkcs12cert);
			JAPCertificate cert2 = JAPCertificate.getInstance(pkcs12cert.getX509Certificate());

			m_tinyTLS.setClientCertificate(new JAPCertificate[]
										   {cert1, cert2}, kp.getPrivate());
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.TOR,
						  "Error while creating Certificates. Certificates are not used.");
		}

		//#######################
		m_tinyTLS.setSoTimeout(30000);
		m_tinyTLS.startHandshake();
		m_istream = m_tinyTLS.getInputStream();
		m_ostream = m_tinyTLS.getOutputStream();
		m_Circuits = new Hashtable();
		m_tinyTLS.setSoTimeout(1000); // this little timeout is ok because we handle it the rigth way!!
		start();
		m_bIsClosed = false;
	}

	/**
	 * Creates a circuit with the given onion routers
	 * @param onionRouters
	 * vector that contains a list of onion routers
	 * @return
	 * on succes : the created circuit
	 * else : null
	 */
	public synchronized Circuit createCircuit(Vector onionRouters)
	{
		int circid = 0;
		try
		{
			/** From Tor spec:  The CircID for a CREATE cell is an arbitrarily chosen 2-byte integer,
			  selected by the node (OP or OR) that sends the CREATE cell.  To prevent
			  CircID collisions, when one OR sends a CREATE cell to another OR, it chooses
			  from only one half of the possible values based on the ORs' public
			  identity keys: if the sending OR has a lower key, it chooses a CircID with
			  an MSB of 0; otherwise, it chooses a CircID with an MSB of 1.

			  Public keys are compared numerically by modulus.
			 ***/
			int iMSB = 0x8000;
			if (m_description.getSigningKey().getModulus().compareTo( ( (MyRSAPublicKey)this.
				m_keypairIdentityKey.getPublic()).getModulus()) > 0)
			{
				iMSB = 0;
			}
			do
			{
				circid = m_rand.nextInt(0x7FFF);
				circid |= iMSB;
			}
			while (m_Circuits.containsKey(new Integer(circid)) && (circid != 0));
			Circuit circ = new Circuit(circid, this, onionRouters);
			m_Circuits.put(new Integer(circid), circ);
			circ.create();
			return circ;
		}
		catch (Exception e)
		{
			m_Circuits.remove(new Integer(circid));
			return null;
		}
	}

	/**
	 * starts the thread that reads from the inputstream and dispatches the cells
	 *
	 */
	private void start()
	{
		if (m_readDataLoop == null)
		{
			m_bRun = true;
			m_readDataLoop = new Thread(this, "FirstOnionRouterConnection - " + m_description.getName());
			m_readDataLoop.setDaemon(true);
			m_readDataLoop.start();
		}
	}

	/**
	 * dispatches cells while the thread, started with start is running
	 */
	public void run()
	{
		Cell cell = null;
		byte[] buff = new byte[512];
		int readPos = 0;

		while (m_bRun)
		{
			readPos = 0;
			while (readPos < 512 && m_bRun)
			{
				int ret = 0;
				try
				{
					ret = m_istream.read(buff, readPos, 512 - readPos);
				}
				catch (InterruptedIOException ioe)
				{
					continue;
				}
				catch (IOException io)
				{
					break;
				}
				if (ret <= 0) //closed
				{
					break;
				}
				readPos += ret;
			}
			if (readPos != 512)
			{
				closedByPeer();
				return;
			}
			LogHolder.log(LogLevel.DEBUG, LogType.TOR,
						  "OnionConnection " + m_description.getName() + " received a Cell!");
			cell = Cell.createCell(buff);
			if (cell == null)
			{
				LogHolder.log(LogLevel.EMERG, LogType.TOR,
							  "OnionConnection " + m_description.getName() + " dont know about this Cell!");
			}
			if (cell == null || !dispatchCell(cell))
			{
				closedByPeer();
				return;
			}
		}
	}

	/**
	 * stops the thread that dispatches cells
	 * @throws IOException
	 */
	private void stop() throws IOException
	{
		if (m_readDataLoop != null && m_bRun)
		{
			try
			{
				m_bRun = false;
				m_readDataLoop.interrupt();
				m_readDataLoop.join();
			}
			catch (Throwable t)
			{
			}
		}
		m_readDataLoop = null;
	}

	/**
	 * closes the connection to the onionrouter
	 *
	 */
	public synchronized void close()
	{
		try
		{
			if (!m_bIsClosed)
			{
				m_bIsClosed = true;
				stop();
				m_tinyTLS.close();
				Enumeration enumer = m_Circuits.elements();
				while (enumer.hasMoreElements())
				{
					( (Circuit) enumer.nextElement()).close();
				}
				m_Circuits.clear();
			}
		}
		catch (Throwable t)
		{
		}
	}

	/**
	 * connection was closed by peer
	 *
	 */
	private void closedByPeer()
	{
		if (m_bIsClosed)
		{
			return;
		}
		synchronized (this)
		{
			try
			{
				stop();
				m_tinyTLS.close();
				Enumeration enumer = m_Circuits.elements();
				while (enumer.hasMoreElements())
				{
					( (Circuit) enumer.nextElement()).destroyedByPeer();
				}
				m_Circuits.clear();
			}
			catch (Throwable t)
			{
			}
			m_bIsClosed = true;
		}
	}

	protected void notifyCircuitClosed(Circuit circ)
	{
		m_Circuits.remove(new Integer(circ.getCircID()));
	}

}
