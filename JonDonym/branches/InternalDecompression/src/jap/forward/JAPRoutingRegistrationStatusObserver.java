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
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import forward.server.ServerSocketPropagandist;

import jap.*;

/**
 * This class is the backend of the forwarding server registration visualization. It can be used
 * for displaying the current registration status of the local forwarding server at the
 * InfoServices. Also if there are registration issues, an errorcode with more detailed
 * information is provided. This class sends a JAPRoutingMessage.REGISTRATION_STATUS_CHANGED to
 * all observers, if the registration state or the errorcode has been changed. So the displayed
 * information can be updated.
 */
public class JAPRoutingRegistrationStatusObserver extends Observable implements Observer
{

	/**
	 * This is the state, if the propaganda for the local forwaring server is not running (usually
	 * this is only the case, when the forwarding server is not running).
	 */
	public static final int STATE_DISABLED = 0;

	/**
	 * This is the state, when all propaganda instances are started (usually shortly after the start
	 * of the local forwarding server). This state prevents, that there are a lot of different
	 * states and errorcodes are displayed while the propaganda is started (because some
	 * registrations may fail, others are successful).
	 */
	public static final int STATE_INITIAL_REGISTRATION = 1;

	/**
	 * This is the state, if the propaganda is running but we have no valid registration at any
	 * infoservice. The getCurrentErrorCode() method provides more information about the reason in
	 * that case.
	 */
	public static final int STATE_NO_REGISTRATION = 2;

	/**
	 * This is the state, if the propaganda is running and we have at least one valid registration
	 * at an InfoService.
	 */
	public static final int STATE_SUCCESSFUL_REGISTRATION = 3;

	/**
	 * This is the errorcode returned in every state except STATE_INITIAL_REGISTRATION.
	 */
	public static final int ERROR_NO_ERROR = 0;

	/**
	 * This is the errorcode, if we don't know any InfoService with a forwarder list. So no
	 * registration is possible (every 10 minutes we will look in the database of all known
	 * InfoServices, whether we can find one then). This errorcode can only occur in
	 * STATE_NO_REGISTRATION.
	 */
	public static final int ERROR_NO_KNOWN_PRIMARY_INFOSERVICES = 1;

	/**
	 * This is the errorcode, if we know at least one InfoService with a forwarder list but cannot
	 * reach any of those InfoServices (we will try it every 10 minutes again). This errorcode can
	 * only occur in STATE_NO_REGISTRATION.
	 */
	public static final int ERROR_INFOSERVICE_CONNECT_ERROR = 2;

	/**
	 * This is the errorcode, if we could reach at least one InfoService with a forwarder list, but
	 * all successful reached InfoServices returned a verification error of our local forwarding
	 * server (maybe because of a running firewall).
	 */
	public static final int ERROR_VERIFICATION_ERROR = 3;

	/**
	 * This is the errorcode, if we know at least one InfoService with a forwarder list but only
	 * unknown errors occured (should not happen).
	 */
	public static final int ERROR_UNKNOWN_ERROR = 4;

	/**
	 * This is the list of all known propaganda instances.
	 */
	private Vector m_propagandaInstances;

	/**
	 * Stores the current forwarding server registration state at the infoservices.
	 */
	private int m_currentState;

	/**
	 * Stores the current error code (only meaningful, if we are in STATE_NO_REGISTRATION).
	 */
	private int m_currentErrorCode;

	/**
	 * Creates a new instance of JAPRoutingRegistrationStatusObserver. We do only some
	 * initialization here.
	 */
	public JAPRoutingRegistrationStatusObserver()
	{
		m_propagandaInstances = new Vector();
		m_currentState = STATE_DISABLED;
		m_currentErrorCode = ERROR_NO_ERROR;
	}

	/**
	 * Returns the current state of the local forwarding server registration at the infoservices.
	 *
	 * @return The current forwarding server registration state.
	 */
	public int getCurrentState()
	{
		return m_currentState;
	}

	/**
	 * Returns the current errorcode, if the current state is STATE_NO_REGISTRATION. In any other
	 * state always ERROR_NO_ERROR is returned. See the ERROR constants in this class to get more
	 * details for the errorcodes. The errorcodes shall help to localize registration issues.
	 *
	 * @return The current error code.
	 */
	public int getCurrentErrorCode()
	{
		return m_currentErrorCode;
	}

	/**
	 * This is the implementation of the observer of the propaganda instances and
	 * JAPRoutingSettings. If the instances reach the state HALTED, they are not observed any more.
	 *
	 * @param a_notifier The propaganda instance, which has changed the state or the
	 *                   JAPRoutingSettings instance.
	 * @param a_message The notification message (should be null for propaganda instances and a
	 *                  JAPRoutingMessage for the JAPRoutingSettings instance).
	 */
	public void update(Observable a_notifier, Object a_message)
	{
		if (a_notifier.getClass().equals(ServerSocketPropagandist.class))
		{
			synchronized (m_propagandaInstances)
			{
				if (m_propagandaInstances.contains(a_notifier))
				{
					/* the notifier is in the list of known forwarding server propagandists */
					if ( ( (ServerSocketPropagandist) a_notifier).getCurrentState() ==
						ServerSocketPropagandist.STATE_HALTED)
					{
						/* Propagandist was halted -> remove it from the list and stop observing the
						 * propagandist
						 */
						a_notifier.deleteObserver(this);
						m_propagandaInstances.removeElement(a_notifier);
						updateCurrentState(false);
					}
				}
			}
		}
		try
		{
			if (a_notifier == JAPModel.getInstance().getRoutingSettings())
			{
				/* message is from JAPRoutingSettings */
				boolean notifyObserversNecessary = false;
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.STOP_PROPAGANDA_CALLED)
				{
					synchronized (this)
					{
						if (m_currentState != STATE_DISABLED)
						{
							m_currentState = STATE_DISABLED;
							m_currentErrorCode = ERROR_NO_ERROR;
							notifyObserversNecessary = true;
						}
					}
				}
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.START_PROPAGANDA_BEGIN)
				{
					synchronized (this)
					{
						if (m_currentState != STATE_INITIAL_REGISTRATION)
						{
							m_currentState = STATE_INITIAL_REGISTRATION;
							m_currentErrorCode = ERROR_NO_ERROR;
							notifyObserversNecessary = true;
						}
					}
				}
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.PROPAGANDA_INSTANCES_ADDED)
				{
					/* update the propagandists in the infoservice registration table */
					updatePropagandaInstancesList( (Vector) ( ( (JAPRoutingMessage) a_message).getMessageData()));
					updateCurrentState(false);
				}
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.START_PROPAGANDA_READY)
				{
					updateCurrentState(true);
				}
				if (notifyObserversNecessary == true)
				{
					setChanged();
					notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.REGISTRATION_STATUS_CHANGED));
				}
			}
		}
		catch (Exception e)
		{
			/* should not happen */
		}
	}

	/**
	 * Updates the current state value and the errorcode value based on the internal stored list of
	 * all propaganda instances.
	 *
	 * @param a_overwriteInitialRegistrationState If we are in STATE_INITIAL_REGISTRATION normally
	 *                                            this method should not update the state and the
	 *                                            errorcode if the registration is not finished yet.
	 *                                            But after the initial registration is done (all
	 *                                            propaganda instances have been started), the state
	 *                                            constant and error code need to be updated, so
	 *                                            also the STATE_INITIAL_REGISTRATION must be
	 *                                            overwritten. If this parameter is true, also
	 *                                            the initial registration state is overwritten, if
	 *                                            this parameter is false, the initial registration
	 *                                            state would not be changed (and also not the
	 *                                            errorcode). If we are in any other state than
	 *                                            STATE_INITIAL_REGISTRATION, we are not affected by
	 *                                            this parameter.
	 */
	private void updateCurrentState(boolean a_overwriteInitialRegistrationState)
	{
		synchronized (m_propagandaInstances)
		{
			synchronized (this)
			{
				if ( ( (m_currentState == STATE_NO_REGISTRATION) ||
					  (m_currentState == STATE_SUCCESSFUL_REGISTRATION)) ||
					( (m_currentState == STATE_INITIAL_REGISTRATION) && (a_overwriteInitialRegistrationState == true)))
				{
					/* we are in a state, where updating the current state is possible */
					int registrationStatus = STATE_NO_REGISTRATION;
					int registrationError = ERROR_NO_KNOWN_PRIMARY_INFOSERVICES;
					if (m_propagandaInstances.size() > 0)
					{
						/* we have at least one propagandist -> we have tried at least one infoservice -> set
						 * the error code to unknown error (minimum reachable error code with one running
						 * propagandist)
						 */
						registrationError = ERROR_UNKNOWN_ERROR;
						Enumeration runningPropagandists = m_propagandaInstances.elements();
						while ( (registrationStatus != STATE_SUCCESSFUL_REGISTRATION) &&
							   (runningPropagandists.hasMoreElements()))
						{
							ServerSocketPropagandist currentPropagandist = (ServerSocketPropagandist) (
								runningPropagandists.nextElement());
							if (currentPropagandist.getCurrentState() ==
								ServerSocketPropagandist.STATE_REGISTERED)
							{
								registrationStatus = STATE_SUCCESSFUL_REGISTRATION;
								registrationError = ERROR_NO_ERROR;
							}
							else
							{
								if ( (currentPropagandist.getCurrentState() ==
									  ServerSocketPropagandist.STATE_CONNECTING) ||
									(currentPropagandist.getCurrentState() ==
									 ServerSocketPropagandist.STATE_RECONNECTING))
								{
									if ( (registrationError == ERROR_UNKNOWN_ERROR) &&
										(currentPropagandist.getCurrentErrorCode() ==
										 ServerSocketPropagandist.RETURN_INFOSERVICE_ERROR))
									{
										registrationError = ERROR_INFOSERVICE_CONNECT_ERROR;
									}
									if ( ( (registrationError == ERROR_UNKNOWN_ERROR) ||
										  (registrationError == ERROR_INFOSERVICE_CONNECT_ERROR)) &&
										(currentPropagandist.getCurrentErrorCode() ==
										 ServerSocketPropagandist.RETURN_VERIFICATION_ERROR))
									{
										registrationError = ERROR_VERIFICATION_ERROR;
									}
								}
							}
						}
					}
					if ( (registrationStatus != m_currentState) || (registrationError != m_currentErrorCode))
					{
						m_currentState = registrationStatus;
						m_currentErrorCode = registrationError;
						setChanged();
						notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.REGISTRATION_STATUS_CHANGED));
					}
				}
			}
		}
	}

	/**
	 * Updates the list of all displayed propaganda instances. We add only new unknown instances
	 * here, because removing of the old ones is done automatically, when they are stopped.
	 *
	 * @param a_newPropagandaInstancesList A Vector with propaganda instances. The new ones are
	 *                                     added to the internal list.
	 */
	private void updatePropagandaInstancesList(Vector a_newPropagandaInstancesList)
	{
		Enumeration propagandists = a_newPropagandaInstancesList.elements();
		synchronized (m_propagandaInstances)
		{
			while (propagandists.hasMoreElements())
			{
				/* removing old propaganda instances is not done here, because they are removed
				 * automatically, when they reach the status HALTED and notify us
				 */
				ServerSocketPropagandist currentPropagandist = (ServerSocketPropagandist) (propagandists.
					nextElement());
				if (m_propagandaInstances.contains(currentPropagandist) == false)
				{
					/* observe the added propagandist, no problem also, if we already observe this
					 * propagandist, then addObserver() does nothing
					 */
					currentPropagandist.addObserver(this);
					if (currentPropagandist.getCurrentState() != ServerSocketPropagandist.STATE_HALTED)
					{
						/* add only the new propagandists to the list of all known propaganda instances */
						m_propagandaInstances.addElement(currentPropagandist);
					}
					else
					{
						/* the propagandist was stopped in the meantime -> don't add it and stop observing */
						currentPropagandist.deleteObserver(this);
					}
				}
			}
		}
	}

}
