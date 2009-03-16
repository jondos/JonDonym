/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package forward.server;

import java.util.Observable;


import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.infoservice.InfoServiceDBEntry;
import anon.util.XMLUtil;

/**
 * This class registers the port of a ServerSocketManager at the infoservice (the IP is detected
 * by the infoservice). So the blockees can find that interface and can connect to. This class
 * is also observable. When the registration state or the current error code changes, the
 * observers are notified (there is no message sent -> message = null). See the STATE and RETURN
 * constants and the getCurrentState() and getCurrentErrorCode() method for more information.
 */
public class ServerSocketPropagandist extends Observable implements Runnable
{

	/**
	 * This is the state, when we are registerd at the infoservice.
	 */
	public static final int STATE_REGISTERED = 0;

	/**
	 * This is the state, when we were never registered at the infoservice and we are trying to
	 * register the first time.
	 */
	public static final int STATE_CONNECTING = 1;

	/**
	 * This is the state, when we were already registered at the infoservice, but the registration
	 * was lost and we are trying to register again.
	 */
	public static final int STATE_RECONNECTING = 2;

	/**
	 * This is the state, when the propaganda thread was stopped.
	 */
	public static final int STATE_HALTED = 3;

	/**
	 * This value is returned, if announcing or renewing of the forwarder entry at the infoservice
	 * was successful.
	 */
	public static final int RETURN_SUCCESS = 0;

	/**
	 * This value is returned, if the infoservice could not verify the local forwarding server.
	 */
	public static final int RETURN_VERIFICATION_ERROR = 1;

	/**
	 * This value is returned, if we could not reach the infoservice or the infoservice has no
	 * forwarder list.
	 */
	public static final int RETURN_INFOSERVICE_ERROR = 2;

	/**
	 * This value is returned, if there was an unexpected error while infoservice communication.
	 */
	public static final int RETURN_UNKNOWN_ERROR = 3;

	/**
	 * This value is returned, if renewing of our forwarding entry fails at the infoservice because
	 * the infoservice does not know our forwarder id any more. This value is only for internal
	 * usage.
	 */
	private static final int RETURN_FORWARDERID_ERROR = 4;

	/**
	 * This is the error code the infoservice returns, if verifying of our forwarding server was not
	 * successful.
	 */
	private static final int FORWARDER_VERIFY_ERROR_CODE = 1;

	/**
	 * This is the error code the infoservice returns, if the registration was lost and the
	 * infoservice doesn't know any mor our forwarding id.
	 */
	private static final int FORWARDER_RENEW_ERROR_CODE = 11;

	/**
	 * The period in milliseconds for renewing the forwarder entry at the infoservice. The default
	 * is 10 minutes.
	 */
	private static final long FORWARDER_RENEW_PERIOD = 10 * 60 * (long) 1000;

	/**
	 * Stores the local port nmuber which we have to announce to the infoservice.
	 */
	private int m_portNumber;

	/**
	 * Stores the infoservice where we have to announce the local port number.
	 */
	private InfoServiceDBEntry m_infoService;

	/**
	 * Stores our by the infoservice assigned id, if the announcement was successful.
	 */
	private String m_forwarderId;

	/**
	 * Stores the error code of the first announcement try. If there was an error while the first
	 * announcement try, this is normally because of a configuration error. With that error code it
	 * should be easier to find the configuration problem. See the RETURN constants in this class
	 * for a description of the values.
	 */
	private int m_currentErrorCode;

	/**
	 * Stores the instance of the propaganda thread.
	 */
	private Thread m_propagandaThread;

	/**
	 * Stores the current connection state. See the STATE constants in this class for a description
	 * of the values.
	 */
	private int m_currentConnectionState;

	/**
	 * Creates a new ServerSocketPropagandist. The instance will register a local forwarding service
	 * at the infoservice. If the registration gets lost, a re-registration is tried automatically.
	 * This method blocks until the first registration try is done. The error code of that first
	 * try can be obtained via the getFirstErrorCode() method. If that first try was not successful,
	 * normally that's caused by configuration problem.
	 *
	 * @param a_portNumber The port number of a local ServerSocketManager.
	 * @param a_infoService The infoservice (which must have a forwarder list) where we shall get
	 *                      registered.
	 */
	public ServerSocketPropagandist(int a_portNumber, InfoServiceDBEntry a_infoService)
	{
		m_portNumber = a_portNumber;
		m_infoService = a_infoService;
		m_propagandaThread = new Thread(this);
		m_propagandaThread.setDaemon(true);
		m_currentErrorCode = announceNewForwarder();
		if (m_currentErrorCode != RETURN_SUCCESS)
		{
			m_currentConnectionState = STATE_CONNECTING;
		}
		else
		{
			m_currentConnectionState = STATE_REGISTERED;
		}
		/* there can be no observers yet -> no need for notification */
		m_propagandaThread.start();
	}

	/**
	 * This will stop the propaganda thread. This method doesn't block, so we set only a flag and
	 * don't wait for the end of the thread.
	 */
	public void stopPropaganda()
	{
		synchronized (m_propagandaThread)
		{
			try
			{
				m_propagandaThread.interrupt();
				/* the thread will come to an end automatically, don't wait for it */
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Returns the current registration state at the infoservice. See the STATE constants in this
	 * class. Observers should call this method, after they have received a update message from this
	 * instance. So they can get the new registration state.
	 *
	 * @return The current infoservice registration state.
	 */
	public int getCurrentState()
	{
		return m_currentConnectionState;
	}

	/**
	 * Returns the error code of the last announcement try. If there was an error while the last
	 * announcement try, this is normally because of a configuration error. With that error code it
	 * should be easier to find the configuration problem. See the RETURN constants in this class
	 * for a description of the values. Only RETURN_SUCCESS, RETURN_VERIFICATION_ERROR,
	 * RETURN_INFOSERVICE_ERROR or RETURN_UNKNOWN_ERROR are possible here. In the states
	 * STATE_REGISTERED or STATE_HALTED, always RETURN_SUCCESS is returned.
	 *
	 * @return The error code of the last announcement try.
	 */
	public int getCurrentErrorCode()
	{
		return m_currentErrorCode;
	}

	/**
	 * Returns the infoservice, where this propagandist is trying to get registrated.
	 *
	 * @return The infoservice, which is updated by this propagandist.
	 */
	public InfoServiceDBEntry getInfoService()
	{
		return m_infoService;
	}

	/**
	 * This is the implementation of the propaganda thread. It will register and (if needed)
	 * re-register the local forwarder at the infoservice.
	 */
	public void run()
	{
		boolean stopPropaganda = false;
		while (!stopPropaganda)
		{
			synchronized (m_propagandaThread)
			{
				try
				{
					stopPropaganda = Thread.interrupted();
					if (!stopPropaganda)
					{
						/* only wait, if we are not already interrupted */
						m_propagandaThread.wait(FORWARDER_RENEW_PERIOD);
					}
				}
				catch (InterruptedException e)
				{
					/* stopPropaganda() was called */
					stopPropaganda = true;
				}
			}
			if (!stopPropaganda)
			{
				boolean notifyObservers = false;
				if (m_currentConnectionState == STATE_REGISTERED)
				{
					/* try to renew the forwarder entry */
					if (renewForwarder() != RETURN_SUCCESS)
					{
						/* there was an error -> registration is lost -> we have to register again */
						m_currentConnectionState = STATE_RECONNECTING;
						/* we have notify the observers after the next try */
						notifyObservers = true;
					}
				}
				if ( (m_currentConnectionState == STATE_CONNECTING) ||
					(m_currentConnectionState == STATE_RECONNECTING))
				{
					/* try to register at the infoservice */
					int currentErrorCode = announceNewForwarder();
					if (currentErrorCode == RETURN_SUCCESS)
					{
						/* we are registered */
						m_currentConnectionState = STATE_REGISTERED;
						/* notify the observers */
						notifyObservers = true;
					}
					if (currentErrorCode != m_currentErrorCode)
					{
						/* if the errorcode is another one than the last time, we have also to notify the
						 * observers
						 */
						notifyObservers = true;
					}
					/* store the current errorcode */
					m_currentErrorCode = currentErrorCode;
					/* notify the observers, if necessary */
					if (notifyObservers == true)
					{
						setChanged();
						notifyObservers(null);
					}
				}
			}
		}
		/* propaganda was stopped */
		m_currentConnectionState = STATE_HALTED;
		m_currentErrorCode = RETURN_SUCCESS;
		/* notify the observers */
		setChanged();
		notifyObservers(null);
	}

	/**
	 * This method announces the local forwarding server with the specified port to the specified
	 * infoservice. Thie method returns one of the return constants in this class.
	 *
	 * @return RETURN_SUCCESS, if we could announce the forwarder to the infoservice successfully.
	 *                         <br>
	 *         RETURN_VERIFICATION_ERROR, if verification of the local forwarding server by the
	 *                                    infoservice failed.<br>
	 *         RETURN_INFOSERVICE_ERROR, if the connection to the infoservice failed or if the
	 *                                   infoservice has no forwarder list.<br>
	 *         RETURN_UNKNOWN_ERROR, if there was an unexpected error, like an error while parsing
	 *                               the answer from the infoservice.<br>
	 */
	private int announceNewForwarder()
	{
		int returnValue = RETURN_UNKNOWN_ERROR;
		/* create the announcement document */
		try
		{
			Document doc =XMLUtil.createDocument();
			Element japForwarderNode = doc.createElement("JapForwarder");
			Element plainInformationNode = doc.createElement("PlainInformation");
			Element portNode = doc.createElement("Port");
			portNode.appendChild(doc.createTextNode(Integer.toString(m_portNumber)));
			plainInformationNode.appendChild(portNode);
			japForwarderNode.appendChild(plainInformationNode);
			doc.appendChild(japForwarderNode);
			try
			{
				/* send the announcement to the infoservice and wait for the answer */
				Element answerJapForwarderNode = m_infoService.postNewForwarder(japForwarderNode);
				/* check the answer */
				NodeList answerPlainInformationNodes = answerJapForwarderNode.getElementsByTagName(
					"PlainInformation");
				if (answerPlainInformationNodes.getLength() == 0)
				{
					/* no PlainInformation node -> there occured an error */
					NodeList answerErrorInformationNodes = answerJapForwarderNode.getElementsByTagName(
						"ErrorInformation");
					if (answerErrorInformationNodes.getLength() == 0)
					{
						/* there should be an ErrorInformation node -> return an unknown error */
						returnValue = RETURN_UNKNOWN_ERROR;
					}
					else
					{
						Element answerErrorInformationNode = (Element) (answerErrorInformationNodes.item(0));
						NodeList answerErrorNodes = answerErrorInformationNode.getElementsByTagName("Error");
						if (answerErrorNodes.getLength() == 0)
						{
							/* there should be an Error node -> return an unknown error */
							returnValue = RETURN_UNKNOWN_ERROR;
						}
						else
						{
							Element answerErrorNode = (Element) (answerErrorNodes.item(0));
							try
							{
								if (Integer.parseInt(answerErrorNode.getAttribute("code")) ==
									FORWARDER_VERIFY_ERROR_CODE)
								{
									/* the verification by the infoservice was not successful, the infoservice could
									 * not reach the local forwarding server
									 */
									returnValue = RETURN_VERIFICATION_ERROR;
								}
								else
								{
									/* we don't know this errorcode */
									returnValue = RETURN_UNKNOWN_ERROR;
									LogHolder.log(LogLevel.ERR, LogType.NET,
												  "ServerSocketPropagandist: announceNewForwarder: The infoservice returned an unknwon error: Errorcode " +
												  Integer.parseInt(answerErrorNode.getAttribute("code")) +
												  ": " + answerErrorNode.getFirstChild().getNodeValue());
								}
							}
							catch (Exception e)
							{
								/* something was wrong with the error node */
								returnValue = RETURN_UNKNOWN_ERROR;
								LogHolder.log(LogLevel.ERR, LogType.NET,
											  "ServerSocketPropagandist: announceNewForwarder: Error while parsing the error information returned by the infoservice: " +
											  e.toString());
							}
						}
					}
				}
				else
				{
					/* there is a PlainInformation node -> try to read our forwarder id */
					Element answerPlainInformationNode = (Element) (answerPlainInformationNodes.item(0));
					NodeList answerForwarderNodes = answerPlainInformationNode.getElementsByTagName(
						"Forwarder");
					if (answerForwarderNodes.getLength() == 0)
					{
						returnValue = RETURN_UNKNOWN_ERROR;
						LogHolder.log(LogLevel.ERR, LogType.NET,
									  "ServerSocketPropagandist: announceNewForwarder: Error while parsing the infoservice answer (Forwarder node).");
					}
					else
					{
						Element answerForwarderNode = (Element) (answerForwarderNodes.item(0));
						String answerId = answerForwarderNode.getAttribute("id");
						if ( (answerId == null) || ( (new String("")).equals(answerId)))
						{
							returnValue = RETURN_UNKNOWN_ERROR;
							LogHolder.log(LogLevel.ERR, LogType.NET,
										  "ServerSocketPropagandist: announceNewForwarder: Got an invalid id from the infoservice.");
						}
						else
						{
							m_forwarderId = answerId;
							returnValue = RETURN_SUCCESS;
						}
					}
				}
			}
			catch (Exception e)
			{
				/* normally this exception is thrown by the infoservice class, if we can't get a connection
				 * to the infoservice or if the infoservice has no forwarder list
				 */
				returnValue = RETURN_INFOSERVICE_ERROR;
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "ServerSocketPropagandist: announceNewForwarder: InfoService communication error: " +
							  e.toString());
			}
		}
		catch (Exception e)
		{
			/* unexpected error while creating the request document */
			returnValue = RETURN_UNKNOWN_ERROR;
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "ServerSocketPropagandist: announceNewForwarder: Unexpected error while creating the request document: " +
						  e.toString());
		}
		return returnValue;
	}

	/**
	 * This method renews our forwarding entry at the infoservice. The infoservice will throw away
	 * every entry after a timeout, so we have to renew it early enough.
	 *
	 * @return RETURN_SUCCESS, if we could announce the forwarder to the infoservice successfully.
	 *                         <br>
	 *         RETURN_FORWARDERID_ERROR, if the registration was lost (the infoservice does not know
	 *                                   our forwarder id any more) and we have to re-register.<br>
	 *         RETURN_INFOSERVICE_ERROR, if the connection to the infoservice failed or if the
	 *                                   infoservice has no forwarder list.<br>
	 *         RETURN_UNKNOWN_ERROR, if there was an unexpected error, like an error while parsing
	 *                               the answer from the infoservice.<br>
	 */
	private int renewForwarder()
	{
		int returnValue = RETURN_UNKNOWN_ERROR;
		try
		{
			/* create the renew document */
			Document doc = XMLUtil.createDocument();
			Element japForwarderNode = doc.createElement("JapForwarder");
			Element plainInformationNode = doc.createElement("PlainInformation");
			Element forwarderNode = doc.createElement("Forwarder");
			forwarderNode.setAttribute("id", m_forwarderId);
			plainInformationNode.appendChild(forwarderNode);
			japForwarderNode.appendChild(plainInformationNode);
			doc.appendChild(japForwarderNode);
			try
			{
				/* send the announcement to the infoservice and wait for the answer */
				Element answerJapForwarderNode = m_infoService.postRenewForwarder(japForwarderNode);
				/* check the answer for an ErrorInformation node -> there was an error, if we get an empty
				 * JapForwarder node -> everything is ok
				 */
				NodeList answerErrorInformationNodes = answerJapForwarderNode.getElementsByTagName(
					"ErrorInformation");
				if (answerErrorInformationNodes.getLength() == 0)
				{
					/* if there is no ErrorInformation node -> everything is ok */
					returnValue = RETURN_SUCCESS;
				}
				else
				{
					Element answerErrorInformationNode = (Element) (answerErrorInformationNodes.item(0));
					NodeList answerErrorNodes = answerErrorInformationNode.getElementsByTagName("Error");
					if (answerErrorNodes.getLength() == 0)
					{
						/* there should be an Error node -> return an unknown error */
						returnValue = RETURN_UNKNOWN_ERROR;
					}
					else
					{
						Element answerErrorNode = (Element) (answerErrorNodes.item(0));
						try
						{
							if (Integer.parseInt(answerErrorNode.getAttribute("code")) ==
								FORWARDER_RENEW_ERROR_CODE)
							{
								/* registration was lost -> we have to re-register */
								returnValue = RETURN_FORWARDERID_ERROR;
							}
							else
							{
								/* we don't know this errorcode */
								returnValue = RETURN_UNKNOWN_ERROR;
								LogHolder.log(LogLevel.ERR, LogType.NET,
											  "ServerSocketPropagandist: renewForwarder: The infoservice returned an unknwon error: Errorcode " +
											  Integer.parseInt(answerErrorNode.getAttribute("code")) + ": " +
											  answerErrorNode.getFirstChild().getNodeValue());
							}
						}
						catch (Exception e)
						{
							/* something was wrong with the error node */
							returnValue = RETURN_UNKNOWN_ERROR;
							LogHolder.log(LogLevel.ERR, LogType.NET,
										  "ServerSocketPropagandist: renewForwarder: Error while parsing the error information returned by the infoservice: " +
										  e.toString());
						}
					}
				}
			}
			catch (Exception e)
			{
				/* normally this exception is thrown by the infoservice class, if we can't get a connection
				 * to the infoservice or if the infoservice has no forwarder list
				 */
				returnValue = RETURN_INFOSERVICE_ERROR;
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "ServerSocketPropagandist: renewForwarder: InfoService communication error: " +
							  e.toString());
			}
		}
		catch (Exception e)
		{
			/* unexpected error while creating the request document */
			returnValue = RETURN_UNKNOWN_ERROR;
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "ServerSocketPropagandist: renewForwarder: Unexpected error while creating the request document: " +
						  e.toString());
		}
		return returnValue;
	}

}
