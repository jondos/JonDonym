package anon.crypto;

import java.security.SecureRandom;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.util.ClassUtil;

/**
 * This class starts a thread that will create DSA key pairs in the background with a low priority.
 * The keys may be popped for further use, while new key pairs will always be created to replace 
 * the popped ones.
 * @author Rolf Wendolsky
 *
 */
public class DSAKeyPool 
{
	private Thread m_keyCreationThread;
	private Vector m_keys;
	private boolean m_bInterrupted;
	private int m_poolSize;
	private int m_certainty;
	private int m_keyLength;
	
	public DSAKeyPool(int a_poolsize)
	{
		m_keys = new Vector();
		if (a_poolsize < 0)
		{
			m_poolSize = 0;
		}
		else if (a_poolsize > 1000)
		{
			m_poolSize = 1000;
		}
		m_poolSize = a_poolsize;
		m_certainty = 60;
		m_keyLength = DSAKeyPair.KEY_LENGTH_1024;
	}
	
	public DSAKeyPool()
	{		
		this(1);
	}
	
	public void start()
	{
		synchronized (m_keys)
		{						
			if (m_keyCreationThread == null)
			{
				m_bInterrupted = false;
				m_keyCreationThread = new Thread(new KeyCreationThread(), 
						ClassUtil.getShortClassName(KeyCreationThread.class));
				m_keyCreationThread.setPriority(Thread.MIN_PRIORITY);
				m_keyCreationThread.setDaemon(true);
				m_keyCreationThread.start();
			}
		}
	}
	
	public void stop()
	{
		synchronized (m_keys)
		{
			while (m_keyCreationThread != null && m_keyCreationThread.isAlive())
			{
				m_bInterrupted = true;
				m_keyCreationThread.interrupt();
				m_keys.notifyAll();
				try 
				{
					m_keys.wait(100);
				} 
				catch (InterruptedException e) 
				{
					break;
				}
			}
			m_keyCreationThread = null;
		}
	}
	
	/**
	 * Pop a key pair from the stack. The creation of a new key pair will start immediately in the background.
	 * If no key pair is currently available, this method will block until there is one. It the thread has been stopped,
	 * this method will return null.
	 * @return the first key pair popped from the queue or null if the pool has been stopped
	 */
	public AsymmetricCryptoKeyPair popKeyPair()
	{
		DSAKeyPair keyPair = null;
		synchronized (m_keys)
		{
			boolean bTempPool = false;
			if (m_keyCreationThread == null)
			{
				start();
			}
			if (m_poolSize == 0)
			{
				bTempPool = true;
				m_poolSize = 1;
			}
			while (m_keys.size() == 0 && m_keyCreationThread != null && !m_bInterrupted)
			{
				try
				{
					m_keys.notify();
					m_keys.wait(500);
				} 
				catch (InterruptedException e) 
				{
					break;
				}
			}
			if (m_keys.size() > 0)
			{
				if (bTempPool)
				{
					m_poolSize = 0;
				}
				keyPair = (DSAKeyPair)m_keys.firstElement();
				m_keys.removeElementAt(0);
				m_keys.notify();
			}
		}
		return keyPair;
	}
	
	private class KeyCreationThread implements Runnable
	{
		public void run()
		{
			DSAKeyPair keyPair;
			
			LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Starting DSA key pool...");
			
			while (!m_bInterrupted)
			{
				synchronized (m_keys)
				{
					if (m_bInterrupted)
					{
						return;
					}
					
					if (m_keys.size() >= m_poolSize)
					{
						try 
						{
							m_keys.wait();
						} 
						catch (InterruptedException e) 
						{
							return;
						}
					}
					if (m_keys.size() >= m_poolSize)
					{
						continue;
					}
				}
				System.out.println("test:" + m_poolSize);
				LogHolder.log(LogLevel.INFO, LogType.CRYPTO, 
						"Creating DSA key pair " + (m_keys.size() + 1) + " of " + m_poolSize + "...");
				
				keyPair = DSAKeyPair.getInstance(new SecureRandom(), m_keyLength, m_certainty);
				
				LogHolder.log(LogLevel.INFO, LogType.CRYPTO, 
						"DSA key pair " + (m_keys.size() + 1) + " was created.");
				m_keys.addElement(keyPair);
				LogHolder.log(LogLevel.INFO, LogType.CRYPTO, 
						"DSA key pair " + (m_keys.size()) + " was added to the pool of currently " + m_keys.size() + ".");
			}
		}
	}
}
