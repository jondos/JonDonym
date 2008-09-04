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

import anon.util.AbstractMessage;

/**
 * This is a message implementation used for forwarding-specific messages. It is used from some
 * forwarding related classes to notify the observers. The message code identifies the reason of
 * the notification.
 */
final public class JAPRoutingMessage extends AbstractMessage {

	/**
	 * This message is sent after the routing mode was changed.
	 */
	public static final int ROUTING_MODE_CHANGED = 1;

	/**
	 * This message is sent after there was added a new forwarding server propaganda instance.
	 * If this message is sent, the data must be the list of all propagandists.
	 */
	public static final int PROPAGANDA_INSTANCES_ADDED = 2;

	/**
	 * This message is sent, when JAPRoutingSettings.startPropaganda() is called and the
	 * propaganda is also started (we are in SERVER_ROUTING_MODE).
	 */
	public static final int START_PROPAGANDA_BEGIN = 3;

	/**
	 * This message is sent, when JAPRoutingSettings.startPropaganda() is ready, which means
	 * that all propaganda instances are started. The data must be the list of all started
	 * propaganda instances.
	 */
	public static final int START_PROPAGANDA_READY = 4;

	/**
	 * This message is sent, after JAPRoutingSettings.stopPropaganda() was called. So all
	 * propaganda instances are stopped. Attention: This message can appear without a
	 * prior START_PROPAGANDA_READY or START_PROPAGANDA_BEGIN message, e.g. if the
	 * startPropaganda thread was interrupted while starting all propaganda instances.
	 */
	public static final int STOP_PROPAGANDA_CALLED = 5;

	/**
	 * This message is sent from JAPRoutingConnectionClassSelector when the current connection class
	 * has been changed (switched to another class).
	 */
	public static final int CONNECTION_CLASS_CHANGED = 6;

	/**
	 * This message is sent from JAPRoutingConnectionClassSelector when the forwarding bandwidth
	 * values or the number of simultaneous connections of the current connection class were changed
	 * and the forwarding system is updated to the new values.
	 */
	public static final int CONNECTION_PARAMETERS_CHANGED = 7;

	/**
	 * This message is sent from JAPRoutingUseableMixCascades when the policy of the allowed
	 * mixcascades is switched between restricted-to-list or access-to-all mode.
	 */
	public static final int ALLOWED_MIXCASCADES_POLICY_CHANGED = 9;

	/**
	 * This message is sent from JAPRoutingUseableMixCascades whenever a MixCascade is
	 * added/updated/removed in the list of allowed mixcascades for the restricted mode. Attention:
	 * This can also occur, if access-to-all mode is selected and also doesn't mean that there was
	 * a change in the list of allowed mixcascades for the restricted mode.
	 */
	public static final int ALLOWED_MIXCASCADES_LIST_CHANGED = 10;

	/**
	 * This message is sent from JAPRoutingRegistrationInfoServices when the policy of the
	 * registration at the infoservices is switched between restricted-to-list or register-at-all
	 * mode.
	 */
	public static final int REGISTRATION_INFOSERVICES_POLICY_CHANGED = 11;

	/**
	 * This message is sent from JAPRoutingRegistrationInfoServices whenever an InfoService is
	 * added/updated/removed in the list of registration infoservices for the registration
	 * restricted-to-list mode. Attention: This can also occur, if register-at-all mode is selected
	 * and also doesn't mean that there was a change in the list of registration infoservices for
	 * the restricted-to-list mode.
	 */
	public static final int REGISTRATION_INFOSERVICES_LIST_CHANGED = 12;

	/**
	 * This message is sent from JAPRoutingServerStatisticsListener when new server statistics are
	 * available.
	 */
	public static final int SERVER_STATISTICS_UPDATED = 13;

	/**
	 * This message is sent from JAPRoutingRegistrationStatusObserver, if the registration status
	 * or the reason why the registration failed has been changed.
	 */
	public static final int REGISTRATION_STATUS_CHANGED = 14;

	/**
	 * This message is sent from JAPRoutingSettings, if the port where the forwarding server is
	 * listening on for client requests, was changed successfully (regardless whether the forwarding
	 * server is currently running or not).
	 */
	public static final int SERVER_PORT_CHANGED = 15;

	/**
	 * This message is sent from JAPRoutingSettings, if the client settings were changed (whether
	 * new connections to the anonymity servers shall use a forwarder, whether it is possible to
	 * obtain the information about a forwarder directly from the InfoServices or the mail-gateway
	 * has to be used and the InfoService requests need also forwarding).
	 */
	public static final int CLIENT_SETTINGS_CHANGED = 16;


	/**
	 * This creates a new JAPRoutingMessage. The message data is set to null.
	 *
	 * @param a_messageCode The message code. See the constants in this class.
	 */
	public JAPRoutingMessage(int a_messageCode)
	{
		super(a_messageCode);
	}

	/**
	 * This creates a new JAPRoutingMessage.
	 *
	 * @param a_messageCode The message code. See the constants in this class.
	 * @param a_messageData The data to send with the message.
	 */
	public JAPRoutingMessage(int a_messageCode, Object a_messageData)
	{
		super(a_messageCode, a_messageData);
	}

}
