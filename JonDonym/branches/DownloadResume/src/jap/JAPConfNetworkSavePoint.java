/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package jap;



/**
 * This is the implementation for the forwarding client savepoint. It is needed for restoring an
 * old or the default configuration, if the user presses "Cancel" or "Reset to defaults".
 */
public class JAPConfNetworkSavePoint implements IJAPConfSavePoint
{

	/**
	 * Stores whether a forwarder is needed to access the anonymity services.
	 */
	private boolean m_connectViaForwarder;

	/**
	 * Stores whether connections to the InfoService needs also forwarding.
	 */
	private boolean m_forwardInfoService;

	/**
	 * This method will store the current forwarding client configuration in this savepoint.
	 */
	public void createSavePoint()
	{
		m_connectViaForwarder = JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder();
		m_forwardInfoService = JAPModel.getInstance().getRoutingSettings().getForwardInfoService();
	}

	/**
	 * Restores the old forwarding client configuration (stored with the last call of createSavePoint()).
	 */
	public void restoreSavePoint()
	{
		JAPModel.getInstance().getRoutingSettings().setConnectViaForwarder(m_connectViaForwarder);
		JAPModel.getInstance().getRoutingSettings().setForwardInfoService(m_forwardInfoService);
	}

	/**
	 * Loads the default forwarding client configuration.
	 */
	public void restoreDefaults()
	{
		/* default is no forwarding */
		JAPModel.getInstance().getRoutingSettings().setConnectViaForwarder(false);
		JAPModel.getInstance().getRoutingSettings().setForwardInfoService(false);
	}

}
