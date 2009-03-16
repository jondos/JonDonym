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
package jap.forward;

import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.XMLUtil;

import jap.*;

/**
 * This is the implementation for the forwarding server savepoint. It is needed for restoring an
 * old or the default configuration, if the user presses "Cancel" or "Reset to defaults".
 */
public class JAPConfForwardingServerSavePoint implements IJAPConfSavePoint
{

	/**
	 * Stores the port where the forwarding server shall listen on.
	 */
	private int m_forwardingServerPort;

	/**
	 * Stores the settings of the forwarding server connection classes.
	 */
	private Element m_connectionClassSettings;

	/**
	 * Stores the settings of the available mixcascades for forwarding.
	 */
	private Element m_availableMixCascadesSettings;

	/**
	 * Stores the settings of the registration infoservices.
	 */
	private Element m_registrationInfoServicesSettings;

	/**
	 * Creates a new instance of JAPConfForwardingServerSavePoint. Only some initialization is done
	 * here.
	 */
	public JAPConfForwardingServerSavePoint()
	{
		m_forwardingServerPort = -1;
		m_connectionClassSettings = null;
		m_availableMixCascadesSettings = null;
		m_registrationInfoServicesSettings = null;
	}

	/**
	 * This method will store the current forwarding server configuration in this savepoint.
	 */
	public void createSavePoint()
	{
		m_forwardingServerPort = JAPModel.getInstance().getRoutingSettings().getServerPort();
		/* create a dummy XML document */
		Document doc = XMLUtil.createDocument();
		/* simply store the XML configurations of the forwarding server parts */
		m_connectionClassSettings = JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().
			getSettingsAsXml(doc);
		m_availableMixCascadesSettings = JAPModel.getInstance().getRoutingSettings().
			getUseableMixCascadesStore().getSettingsAsXml(doc);
		m_registrationInfoServicesSettings = JAPModel.getInstance().getRoutingSettings().
			getRegistrationInfoServicesStore().getSettingsAsXml(doc);
		/* that's it */
	}

	/**
	 * Restores the old forwarding server configuration (stored with the last call of createSavePoint()).
	 */
	public void restoreSavePoint()
	{
		if (m_forwardingServerPort != -1)
		{
			JAPModel.getInstance().getRoutingSettings().setServerPort(m_forwardingServerPort);
		}
		if (m_connectionClassSettings != null)
		{
			JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().loadSettingsFromXml(
				m_connectionClassSettings);
		}
		if (m_availableMixCascadesSettings != null)
		{
			JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().loadSettingsFromXml(
				m_availableMixCascadesSettings);
		}
		if (m_registrationInfoServicesSettings != null)
		{
			JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().
				loadSettingsFromXml(m_registrationInfoServicesSettings);
		}
	}

	/**
	 * Restores the default forwarding server configuration.
	 */
	public void restoreDefaults()
	{
		/* restore the connection class settings */
		Enumeration connectionClasses = JAPModel.getInstance().getRoutingSettings().
			getConnectionClassSelector().getConnectionClasses().elements();
		while (connectionClasses.hasMoreElements())
		{
			JAPRoutingConnectionClass currentConnectionClass = (JAPRoutingConnectionClass) (connectionClasses.
				nextElement());
			if (currentConnectionClass.getIdentifier() ==
				JAPRoutingConnectionClassSelector.CONNECTION_CLASS_USER)
			{
				/* restore the default bandwidth (128 kbit/sec) of the user-defined connection class */
				currentConnectionClass.setMaximumBandwidth(16000);
			}
			/* restore the default relative useable bandwidth of each connection class (always 50% of
			 * the maximum possible bandwidth of the class)
			 */
			currentConnectionClass.setRelativeBandwidth(50);
		}
		/* restore the default connection class (DSL128 kbit/sec upload) */
		JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().setCurrentConnectionClass(
			JAPRoutingConnectionClassSelector.CONNECTION_CLASS_DSL128);
		/* allow access to all available mixcascades */
		JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().
			setAllowAllAvailableMixCascades(true);
		JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().setAllowedMixCascades(new
			Vector());
		/* register at all available infoservices with a forwarder list */
		JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().
			setRegisterAtAllAvailableInfoServices(true);
		JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().
			setRegistrationInfoServices(new Vector());
	}

}
