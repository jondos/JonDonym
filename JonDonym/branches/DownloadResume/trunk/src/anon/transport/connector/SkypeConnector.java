/*
 Copyright (c) 2008, The JAP-Team
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
package anon.transport.connector;


import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.transport.address.IAddress;
import anon.transport.address.SkypeAddress;
import anon.transport.connection.ChunkConnectionAdapter;
import anon.transport.connection.CommunicationException;
import anon.transport.connection.ConnectionException;
import anon.transport.connection.IChunkConnection;
import anon.transport.connection.IConnection;
import anon.transport.connection.IStreamConnection;
import anon.transport.connection.SkypeConnection;

import com.skype.Application;
import com.skype.Skype;
import com.skype.SkypeException;
import com.skype.Stream;

/**
 * Connector welche auf Basis einer uebergebenen {@link SkypeAddress} versucht
 * eine Verbindung zum angegeben entfernten Ende aufzubauen.
 * <p>
 * Die zurueckgegeben Verbindung ist dabei Strom basierend.
 */
public class SkypeConnector implements IConnector
	{

		/**
		 * Versucht eine {@link IStreamConnection} zur angebenen Adresse aufzubauen.
		 * <p>
		 * Die gegebenfalls geworfenen Ausnahme, gibt genauere Hinweise, an welcher
		 * Stelle Probleme mit dem Einrichten der Verbindung auftraten.
		 * 
		 * @param a_address
		 *          Die Adresse wohin eine Verbindung aufgebaut werden soll.
		 */
		public IStreamConnection connect(SkypeAddress a_address)
				throws ConnectionException
			{
				Application app=null;
				LogHolder.log(LogLevel.DEBUG, LogType.NET,"Skye Connector - Skype.setDaemon()");
				Skype.setDeamon(false);
				try
					{
						LogHolder.log(LogLevel.DEBUG, LogType.NET,"Skye Connector - Skype.setDebug()");
						Skype.setDebug(true);
						LogHolder.log(LogLevel.DEBUG, LogType.NET,"Skye Connector - Skype.setDebug - finished()");
										}
				catch (SkypeException e1)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.NET,"Skye Connector - exception");
					}
				// are we trying to connect to ourself?
				LogHolder.log(LogLevel.DEBUG, LogType.NET,"Skye Connector - try to get user id");

				try
					{
						String localID = Skype.getProfile().getId();
						if (localID.equals(a_address.getUserID())) throw new CommunicationException(
								"No selfconection over Skype allowed");
					}
				catch (SkypeException e)
					{
						LogHolder.log(LogLevel.WARNING, LogType.TRANSPORT,
								"Unable to get local Skype User ID");
					}

				// so we try to register the application
				LogHolder.log(LogLevel.DEBUG, LogType.TRANSPORT,"Try to register Skype forwarding application");
				try
					{
						app = Skype.addApplication(a_address.getApplicationName());
					}
				catch (SkypeException e)
					{
						throw new CommunicationException(
								"Unable to create desired Skype Application "
										+ a_address.getApplicationName());
					}

				// did we got the application
				if (app == null) throw new CommunicationException(
						"Unable to create desired Skype Application "
								+ a_address.getApplicationName());

				// so we try to get an stream
				LogHolder.log(LogLevel.DEBUG, LogType.TRANSPORT,"Try to get a stream from Skype");
				Stream[] connectionStreams=null;
				try
					{
						connectionStreams = app.connect(new String[] { a_address.getUserID() });
					}
				catch (SkypeException e)
					{
						throw new CommunicationException(
								"Unable to connect to User with ID " + a_address.getUserID());
					}
				if ((connectionStreams == null) || (connectionStreams.length == 0)) throw new CommunicationException(
						"Unable to connect to User with ID " + a_address.getUserID());
				// we got at least on stream. let's build the base chunk connection
				LogHolder.log(LogLevel.DEBUG, LogType.TRANSPORT,"Setup the base Skype connection");
				IChunkConnection baseConnection = new SkypeConnection(
						connectionStreams[0]);

				// and make an StreamConnection out of it
				return new ChunkConnectionAdapter(baseConnection);
			}

		public IConnection connect(IAddress a_address) throws ConnectionException
			{
				if (!(a_address instanceof SkypeAddress)) throw new IllegalArgumentException(
						"Connector can only handel Address of type SkypeAddress");
				return connect((SkypeAddress) a_address);
			}

	}
