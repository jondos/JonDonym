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

import forward.server.ServerSocketPropagandist;


/**
 * This class is used for synchronization between the threads, when startPropaganda() is called.
 */
public class JAPRoutingSettingsPropagandaThreadLock
{

	/**
	 * Stores whether the propaganda thread has come to the end (true) or not (false).
	 */
	private boolean m_propagandaThreadReady;

	/**
	 * Stores the status of the forwarding server registration process at the infoservices. See
	 * the REGISTRATION constants in JAPRoutingSettings;
	 * @see JAPRoutingSettings
	 */
	private int m_registrationStatus;

	/**
	 * This constructs a new JAPRoutingSettingsPropagandaThreadLock. The propagandaThreadReady
	 * value is set to false. The registration status is set to
	 * JAPRoutingSettings.REGISTRATION_NO_INFOSERVICES.
	 */
	public JAPRoutingSettingsPropagandaThreadLock()
	{
		m_propagandaThreadReady = false;
		m_registrationStatus = JAPRoutingSettings.REGISTRATION_NO_INFOSERVICES;
	}

	/**
	 * Sets the propagandaThreadReady value to the ready state (propaganda thread has come to the
	 * end.
	 */
	public void propagandaThreadIsReady()
	{
		m_propagandaThreadReady = true;
	}

	/**
	 * Returns whether the propaganda thread has come to the end (registered the forwarding server
	 * at all specified infoservices or was interrupted while registering).
	 *
	 * @return True, if the propaganda thread is ready or false, if it is still working.
	 */
	public boolean isPropagandaThreadReady()
	{
		return m_propagandaThreadReady;
	}

	/**
	 * Updates the stored registration status according to the status of the supplied
	 * ServerSocketPropagandist.
	 *
	 * @param a_currentPropagandist The ServerSocketPropagandist, which is used for updating the
	 *                              internal stored registration status. So it is considered, if
	 *                              you call getRegistrationStatus() later.
	 */
	public void updateRegistrationStatus(ServerSocketPropagandist a_currentPropagandist)
	{
		if (m_registrationStatus == JAPRoutingSettings.REGISTRATION_NO_INFOSERVICES)
		{
			/* we have one propagandist -> we have tried at least one infoservice -> set the status to
			 * unknown error (minimum reached status with this propagandist)
			 */
			m_registrationStatus = JAPRoutingSettings.REGISTRATION_UNKNOWN_ERRORS;
		}
		if (a_currentPropagandist.getCurrentState() == ServerSocketPropagandist.STATE_REGISTERED)
		{
			/* registration was successful at least at one infoservice -> set the success status */
			m_registrationStatus = JAPRoutingSettings.REGISTRATION_SUCCESS;
		}
		else
		{
			/* registration was not successful -> find out the reason */
			if ( (a_currentPropagandist.getCurrentErrorCode() ==
				  ServerSocketPropagandist.RETURN_INFOSERVICE_ERROR) &&
				(m_registrationStatus == JAPRoutingSettings.REGISTRATION_UNKNOWN_ERRORS))
			{
				/* we have until yet the unknown error state, but now we have a more concrete error code
				 * (problems reaching one infoservice) -> set this error code
				 */
				m_registrationStatus = JAPRoutingSettings.REGISTRATION_INFOSERVICE_ERRORS;
			}
			if ( (a_currentPropagandist.getCurrentErrorCode() ==
				  ServerSocketPropagandist.RETURN_VERIFICATION_ERROR) &&
				( (m_registrationStatus == JAPRoutingSettings.REGISTRATION_UNKNOWN_ERRORS) ||
				 (m_registrationStatus == JAPRoutingSettings.REGISTRATION_INFOSERVICE_ERRORS)))
			{
				/* now we could reach an infoservice successful, but the infoservice could not verify
				 * the local forwarding server -> update the error code, if there were only unknown
				 * or infoservice communication errors until yet
				 */
				m_registrationStatus = JAPRoutingSettings.REGISTRATION_VERIFY_ERRORS;
			}
		}
	}

	/**
	 * Sets the internal registration status to the interrupted state.
	 * @see JAPRoutingSettings.REGISTRATION_INTERRUPTED
	 */
	public void registrationWasInterrupted()
	{
		m_registrationStatus = JAPRoutingSettings.REGISTRATION_INTERRUPTED;
	}

	/**
	 * Returns the status of the registration process of the local forwarding server at the
	 * infoservices. See the REGISTRATION constants in JAPConfRouting.
	 *
	 * @return The status of the registration process.
	 *
	 * @see JAPConfRouting
	 */
	public int getRegistrationStatus()
	{
		return m_registrationStatus;
	}

}
