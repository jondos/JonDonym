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
package jap.forward;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.util.XMLUtil;
import forward.server.ForwardServerManager;
import jap.JAPModel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class manages the useable mixcascades for the clients of the local forwarding server. So
 * they get always an up-to-date list of running and allowed mixcascades.
 */
final public class JAPRoutingUseableMixCascades extends Observable implements Observer, Runnable
{

	/**
	 * This is the update interval of the mixcascade list. The list update is done by fetching the
	 * mixcascade list from the infoservices (via InfoServiceHolder.getMixCascades()). Then the
	 * internal stored list (in ForwardCascadeDatabase) is updated by adding the new (allowed)
	 * cascades and removing that ones, which are not in the list of currently running cascades.
	 * So the client always gets an up-to-date list of available (and allowed) cascades. The
	 * default update interval is 10 minutes.
	 */
	private static final long MIXCASCADELIST_UPDATE_INTERVAL = 10 * 60 * (long) 1000;

	/**
	 * This stores the list of allowed mixcascades. This list is used, if the access to the
	 * mixcascades shall be restricted for the clients of the local forwarding server. So if
	 * access to all available mixcascades is enabled, this list is not used. The usage of
	 * the hashtable has only comfort reasons. So accessing the cascades by the ID is much
	 * easier.
	 */
	Hashtable m_allowedMixCascades;

	/**
	 * This stores, whether the access to all available mixcascades shall be allowed for the clients
	 * of the local forwarding server (true) or it shall be restricted to some cascades (false).
	 */
	boolean m_allowAllAvailableCascades;

	/**
	 * This stores the list of currently running mixcascades. This list is updated once within an
	 * update interval. The usage of the hashtable has only comfort reasons. So accessing the
	 * cascades by the ID is much easier.
	 */
	Hashtable m_currentlyRunningMixCascades;

	/**
	 * This stores the instance of the update thread for the mixcascades. It updates the list of
	 * currently running mixcascades and also the database of useable mixcascades for the clients
	 * of the local forwarding server is updated. This thread is executed while we are in server
	 * routing mode. If the thread is not running, this value is null. There can be only one
	 * running instance of that thread at one time.
	 */
	Thread m_updateMixCascadesListThread;

	/**
	 * This creates a new instance of JAPRoutingUseableMixCascades. Some initialization is done
	 * here. The new instance is configured for allowing access to all available mixcascades.
	 */
	public JAPRoutingUseableMixCascades()
	{
		m_allowedMixCascades = new Hashtable();
		m_allowAllAvailableCascades = true;
		m_currentlyRunningMixCascades = new Hashtable();
		m_updateMixCascadesListThread = null;
	}

	/**
	 * This is the observer implementation to observe the instance of JAPRoutingSettings. We handle
	 * the messages about changes of the routing mode here. If the forwarding server is started, also
	 * we start also the mixcascades management thread. If it is stopped, we stop also that thread.
	 *
	 * @param a_notifier The observed Object. This should always be JAPRoutingSettings at the moment.
	 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
	 *                  at the moment.
	 */
	public void update(Observable a_notifier, Object a_message)
	{
		if (a_notifier == JAPModel.getInstance().getRoutingSettings())
		{
			try
			{
				/* message is from JAPRoutingSettings */
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.ROUTING_MODE_CHANGED)
				{
					synchronized (this)
					{
						if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
							JAPRoutingSettings.ROUTING_MODE_SERVER)
						{
							/* look, whether the update-thread is running */
							if (m_updateMixCascadesListThread == null)
							{
								/* we have to start it */
								startMixCascadesListUpdateThread();
							}
						}
						else
						{
							/* look, whether the update-thread is running */
							if (m_updateMixCascadesListThread != null)
							{
								/* we have to stop it */
								stopMixCascadesListUpdateThread();
							}
						}
					}
				}
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * This changes the list of allowed mixcascades for the clients of the local forwarding server.
	 * If we are in the restricted mode (accessing all available mixcascades is not enabled),
	 * clients can only connect to the mixcascades specified here, which are also running at the
	 * moment of the client connection. If JAPRoutingSettings is in server routing mode (mixcascade
	 * management thread is running) and we are in the restricted mode, also the database of
	 * useable mixcascades for the client is updated immediately. Attention: Calling this method
	 * does not automatically activate the restricted mode. So you have to do this by the call
	 * of the setAllowAllMixCascades() method explicitly.
	 *
	 * @param a_mixCascades A Vector of mixcascades, which shall be allowed for the clients of the
	 *                      local forwarding server in the restricted mode.
	 */
	public void setAllowedMixCascades(Vector a_mixCascades)
	{
		synchronized (m_allowedMixCascades)
		{
			m_allowedMixCascades.clear();
			Enumeration newAllowedCascades = a_mixCascades.elements();
			while (newAllowedCascades.hasMoreElements())
			{
				MixCascade currentMixCascade = (MixCascade) (newAllowedCascades.nextElement());
				m_allowedMixCascades.put(currentMixCascade.getId(), currentMixCascade);
			}
		}
		synchronized (this)
		{
			if ( (m_updateMixCascadesListThread != null) && (m_allowAllAvailableCascades == false))
			{
				/* the mixcascades management thread is running and we are in restricted mode -> update
				 * the useable cascades database
				 */
				updateUseableCascadesDatabase();
			}
			setChanged();
			notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.ALLOWED_MIXCASCADES_LIST_CHANGED));
		}
	}

	/**
	 * This adds a MixCascade to the list of allowed mixcascades for the clients of the local
	 * forwarding server. If we are in the restricted mode (accessing all available mixcascades is
	 * not enabled), clients can only connect to the mixcascades specified in that list, which are
	 * also running at the moment of the client connection. If JAPRoutingSettings is in server
	 * routing mode (mixcascade management thread is running) and we are in the restricted mode,
	 * also the database of useable mixcascades for the client is updated immediately. Attention:
	 * Calling this method does not automatically activate the restricted mode. So you have to do
	 * this by the call of the setAllowAllMixCascades() method explicitly.
	 *
	 * @param a_mixCascade The MixCascade which should added to the list of allowed mixcascades for
	 *                     the restricted mode. If there is already a mixcascade with the same ID in
	 *                     the list, it is updated to this new cascade value.
	 */
	public void addToAllowedMixCascades(MixCascade a_mixCascade)
	{
		if (a_mixCascade != null)
		{
			synchronized (m_allowedMixCascades)
			{
				m_allowedMixCascades.put(a_mixCascade.getId(), a_mixCascade);
			}
			synchronized (this)
			{
				if ( (m_updateMixCascadesListThread != null) && (m_allowAllAvailableCascades == false))
				{
					/* the mixcascades management thread is running and we are in restricted mode -> update
					 * the useable cascades database
					 */
					updateUseableCascadesDatabase();
				}
				setChanged();
				notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.ALLOWED_MIXCASCADES_LIST_CHANGED));
			}
		}
	}

	/**
	 * This removes a MixCascade from the list of allowed mixcascades for the clients of the local
	 * forwarding server. If we are in the restricted mode (accessing all available mixcascades is
	 * not enabled), clients can only connect to the mixcascades specified in that list, which are
	 * also running at the moment of the client connection. If JAPRoutingSettings is in server
	 * routing mode (mixcascade management thread is running) and we are in the restricted mode,
	 * also the database of useable mixcascades for the client is updated immediately. Attention:
	 * Calling this method does not automatically activate the restricted mode. So you have to do
	 * this by the call of the setAllowAllMixCascades() method explicitly.
	 *
	 * @param a_mixCascadeId The MixCascade which should removed from the list of allowed
	 *                       mixcascades for the restricted mode. If there is no mixcascade with
	 *                       this ID is in the list, nothing is done.
	 */
	public void removeFromAllowedMixCascades(String a_mixCascadeId)
	{
		if (a_mixCascadeId != null)
		{
			boolean cascadeRemoved = false;
			synchronized (m_allowedMixCascades)
			{
				if (m_allowedMixCascades.remove(a_mixCascadeId) != null)
				{
					cascadeRemoved = true;
				}
			}
			if (cascadeRemoved == true)
			{
				synchronized (this)
				{
					if ( (m_updateMixCascadesListThread != null) && (m_allowAllAvailableCascades == false))
					{
						/* the mixcascades management thread is running and we are in restricted mode -> update
						 * the useable cascades database
						 */
						updateUseableCascadesDatabase();
					}
					setChanged();
					notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.ALLOWED_MIXCASCADES_LIST_CHANGED));
				}
			}
		}
	}

	/**
	 * Returns a clone of the list of allowed mixcascades for the forwarding server.
	 *
	 * @return A clone of the allowed mixcascades list for the clients of the local forwarding
	 *         server.
	 */
	public Vector getAllowedMixCascades()
	{
		Vector resultValue = new Vector();
		synchronized (m_allowedMixCascades)
		{
			Enumeration allowedCascades = m_allowedMixCascades.elements();
			while (allowedCascades.hasMoreElements())
			{
				resultValue.addElement(allowedCascades.nextElement());
			}
		}
		return resultValue;
	}

	/**
	 * This changes the restriction mode for the clients between no restriction (access to all
	 * running mixcascades is allowed) or restriction to the list of allowed mixcascades, which
	 * needs to be also running. If the forwarding server is running and the mode was changed,
	 * also the database of useable mixcascades for the forwarding server is updated.
	 *
	 * @param a_allowAllAvailableCascades Whether access to all available mixcascades shall be
	 *                                    granted (true) or only to set of explicitly allowed
	 *                                    mixcascades (false).
	 */
	public void setAllowAllAvailableMixCascades(boolean a_allowAllAvailableCascades)
	{
		synchronized (this)
		{
			if (m_allowAllAvailableCascades != a_allowAllAvailableCascades)
			{
				m_allowAllAvailableCascades = a_allowAllAvailableCascades;
				if (m_updateMixCascadesListThread != null)
				{
					/* the mode was changed and the management thread is running (-> forwarding server is
					 * running), so update the database of useable mixcascades
					 */
					updateUseableCascadesDatabase();
				}
				setChanged();
				notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.ALLOWED_MIXCASCADES_POLICY_CHANGED));
			}
		}
	}

	/**
	 * Returns the restriction mode. This method returns true, if the clients of the local
	 * forwarding server have access to all available mixcascades or false, if they have only access
	 * to a set of allowed mixcascades.
	 *
	 * @return Whether all mixcascades are allowed to access for the clients of the local forwarding
	 *         server.
	 */
	public boolean getAllowAllAvailableMixCascades()
	{
		boolean returnValue = false;
		synchronized (this)
		{
			returnValue = m_allowAllAvailableCascades;
		}
		return returnValue;
	}

	/**
	 * Returns the current settings for the allowed forwarding mixcascades (allowed cascades,
	 * whether all running mixcascades are allowed) for storage within an XML document.
	 *
	 * @param a_doc The context document for the forwarding mixcascades settings.
	 *
	 * @return An XML node (AllowedMixCascadesSettings) with the allowed forwarding mixcascades
	 *         related settings.
	 */
	public Element getSettingsAsXml(Document a_doc)
	{
		Element allowedMixCascadesSettingsNode = a_doc.createElement("AllowedMixCascadesSettings");
		Element allowAllAvailableMixCascadesNode = a_doc.createElement("AllowAllAvailableMixCascades");
		Element allowedMixCascadesNode = a_doc.createElement("AllowedMixCascades");
		synchronized (this)
		{
			XMLUtil.setValue(allowAllAvailableMixCascadesNode, getAllowAllAvailableMixCascades());
			Enumeration allowedMixCascades = getAllowedMixCascades().elements();
			while (allowedMixCascades.hasMoreElements())
			{
				allowedMixCascadesNode.appendChild( ( (MixCascade) (allowedMixCascades.nextElement())).
					toXmlElement(a_doc));
			}
		}
		allowedMixCascadesSettingsNode.appendChild(allowAllAvailableMixCascadesNode);
		allowedMixCascadesSettingsNode.appendChild(allowedMixCascadesNode);
		return allowedMixCascadesSettingsNode;
	}

	/**
	 * This method loads all settings for the allowed mixcascades for the forwarding server from a
	 * prior created XML structure. If there is an error while loading the settings, it is still
	 * tried to load as much settings as possible.
	 *
	 * @param a_infoServiceRegistrationSettingsNode The AllowedMixCascadesSettings XML node,
	 *                                              which was created by the getSettingsAsXml()
	 *                                              method.
	 *
	 * @return True, if there was no error while loading the settings and false, if there was one.
	 */
	public boolean loadSettingsFromXml(Element a_allowedMixCascadesSettingsNode)
	{
		/* store, whether there were some errors while loading the settings */
		boolean noError = true;
		/* get the AllowAllAvailableMixCascades settings */
		Element allowAllAvailableMixCascadesNode = (Element) (XMLUtil.getFirstChildByName(
			a_allowedMixCascadesSettingsNode, "AllowAllAvailableMixCascades"));
		if (allowAllAvailableMixCascadesNode == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingUseableMixCascades: loadSettingsFromXml: Error in XML structure (AllowAllAvailableMixCascades node): Using default setting.");
			noError = false;
		}
		else
		{
			setAllowAllAvailableMixCascades(XMLUtil.parseValue(allowAllAvailableMixCascadesNode,
				getAllowAllAvailableMixCascades()));
		}
		/* load the list of allowed mixcascades for the case, that not all available mixcascades are
		 * allowed
		 */
		Element allowedMixCascadesNode = (Element) (XMLUtil.getFirstChildByName(
			a_allowedMixCascadesSettingsNode, "AllowedMixCascades"));
		if (allowedMixCascadesNode == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Error in XML structure (AllowedMixCascades node): Skip loading of allowed mixcascades.");
			noError = false;
		}
		else
		{
			NodeList mixCascadeNodes = allowedMixCascadesNode.getElementsByTagName("MixCascade");
			Vector allowedMixCascades = new Vector();
			for (int i = 0; i < mixCascadeNodes.getLength(); i++)
			{
				Element mixCascadeNode = (Element) (mixCascadeNodes.item(i));
				try
				{
					MixCascade currentMixCascade = new MixCascade(mixCascadeNode);
					allowedMixCascades.addElement(currentMixCascade);
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC,
								  "Error while loading one allowed MixCascade: Skipping this MixCascade (" +
								  e.toString() + ").");
					noError = false;
				}
			}
			setAllowedMixCascades(allowedMixCascades);
		}
		return noError;
	}

	/**
	 * This is the implementation of the mixcascades management thread for the local forwarding
	 * server. It fetches the currently running mixcascades once per update interval from the
	 * infoservices and updates with that information the database of currently running and allowed
	 * mixcascades for the clients of the local forwarding server.
	 */
	public void run()
	{
		boolean stopThread = false;
		while (stopThread == false && !Thread.currentThread().isInterrupted())
		{
			/* get all running mixcascades */
			Hashtable runningMixCascadesList = InfoServiceHolder.getInstance().getMixCascades();
			if (runningMixCascadesList == null)
			{
				/* handle communication errors like no cascades are currently running */
				runningMixCascadesList = new Hashtable();
			}
			synchronized (m_currentlyRunningMixCascades)
			{
				/* update the list of currently running mixcascades */
				m_currentlyRunningMixCascades.clear();
				Enumeration runningMixCascades = runningMixCascadesList.elements();
				while (runningMixCascades.hasMoreElements())
				{
					MixCascade currentMixCascade = (MixCascade) (runningMixCascades.nextElement());
					m_currentlyRunningMixCascades.put(currentMixCascade.getId(), currentMixCascade);
				}
			}
			/* now update the database of useable mixcascades for the clients of the forwarding
			 * server
			 */
			updateUseableCascadesDatabase();
			synchronized (m_updateMixCascadesListThread)
			{
				stopThread = Thread.interrupted();
				if (stopThread == false)
				{
					try
					{
						m_updateMixCascadesListThread.wait(MIXCASCADELIST_UPDATE_INTERVAL);
					}
					catch (Exception e)
					{
						/* there was an exception, this should only be an interrupted exception */
						stopThread = true;
					}
				}
			}
		}
		/* the update mixcascades list thread was stopped -> clear the list of currently running
		 * mixcascades and also the database of useable mixcascades for the clients of the local
		 * forwarding server
		 */
		synchronized (m_currentlyRunningMixCascades)
		{
			m_currentlyRunningMixCascades.clear();
		}
		ForwardServerManager.getInstance().getAllowedCascadesDatabase().removeAllCascades();
	}

	/**
	 * Updates the ForwardServerDatabase, which contains the currently useable mixcascades for the
	 * clients of the local forwarding server. This method adds new and allowed cascades to that
	 * database and removes all currently not running or not allowed cascades from there.
	 */
	private void updateUseableCascadesDatabase()
	{
		synchronized (m_currentlyRunningMixCascades)
		{
			synchronized (m_allowedMixCascades)
			{
				/* we need exclusiv access while changing the mixcascade lists */
				boolean useAllAvailableCascades = m_allowAllAvailableCascades;
				/* clear the list of currently running mixcascades and rebuild it */
				Enumeration runningMixCascades = m_currentlyRunningMixCascades.elements();
				while (runningMixCascades.hasMoreElements())
				{
					MixCascade currentMixCascade = (MixCascade) (runningMixCascades.nextElement());
					if (useAllAvailableCascades == true)
					{
						/* add / update this cascade in the database of all useable cascades */
						ForwardServerManager.getInstance().getAllowedCascadesDatabase().addCascade(
							currentMixCascade);
					}
					else
					{
						/* look, whether a cascade with this id is in the list of allowed mixcascades */
						if (m_allowedMixCascades.containsKey(currentMixCascade.getId()) == true)
						{
							/* add / update this cascade in the database of all useable cascades */
							ForwardServerManager.getInstance().getAllowedCascadesDatabase().addCascade(
								currentMixCascade);
						}
					}
				}
				/* now remove all cascades, which are not running or not allowed from the useable
				 * cascades database of the forwarding server
				 */
				Enumeration forwardingCascades = ForwardServerManager.getInstance().
					getAllowedCascadesDatabase().getEntryList().elements();
				while (forwardingCascades.hasMoreElements())
				{
					MixCascade currentForwardingMixCascade = (MixCascade) (forwardingCascades.nextElement());
					if (m_currentlyRunningMixCascades.containsKey(currentForwardingMixCascade.getId()))
					{
						/* the mixcascade is currently running -> ok */
						if (useAllAvailableCascades == false)
						{
							/* we have to look, whether the mixcascade is allowed */
							if (m_allowedMixCascades.containsKey(currentForwardingMixCascade.getId()) == false)
							{
								/* the mixcascade is not allowed -> remove it */
								ForwardServerManager.getInstance().getAllowedCascadesDatabase().removeCascade(
									currentForwardingMixCascade.getId());
							}
						}
					}
					else
					{
						/* the mixcascade is not running any more -> remove it */
						ForwardServerManager.getInstance().getAllowedCascadesDatabase().removeCascade(
							currentForwardingMixCascade.getId());
					}
				}
			}
		}
	}

	/**
	 * This starts the management thread for the useable mixcascades of the local forwarding server,
	 * if it is not already running.
	 */
	private void startMixCascadesListUpdateThread()
	{
		synchronized (this)
		{
			if (m_updateMixCascadesListThread == null)
			{
				LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: startMixCascadesListUpdateThread: The mixcascade management thread of the forwarding server is started.");
				m_updateMixCascadesListThread = new Thread(this);
				m_updateMixCascadesListThread.setDaemon(true);
				m_updateMixCascadesListThread.start();
			}
			else
			{
				LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: startMixCascadesListUpdateThread: The mixcascade management thread of the forwarding server was already started.");
			}
		}
	}

	/**
	 * This stops the management thread for the useable mixcascades of the local forwarding server,
	 * if it is running.
	 */
	private void stopMixCascadesListUpdateThread()
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: stopMixCascadesListUpdateThread: Shutdown the mixcascade management thread of the forwarding server...");
		synchronized (this)
		{
			if (m_updateMixCascadesListThread != null)
			{
				synchronized (m_updateMixCascadesListThread)
				{
					m_updateMixCascadesListThread.interrupt();
				}
				try
				{
					m_updateMixCascadesListThread.join();
					LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: stopMixCascadesListUpdateThread: Mixcascade management thread of the forwarding server halted.");
				}
				catch (Exception e)
				{
				}
				m_updateMixCascadesListThread = null;
			}
			else
			{
				LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: stopMixCascadesListUpdateThread: The mixcascade management thread of the forwarding server was not running.");
			}
		}
	}

}
