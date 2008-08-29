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
public class SkypeConnector implements IConnector {

	/**
	 * Versucht eine {@link IStreamConnection} zur angebenen Adresse aufzubauen.
	 * <p>
	 * Die gegebenfalls geworfenen Ausnahme, gibt genauere Hinweise, an welcher
	 * Stelle Probleme mit dem Einrichten der Verbindung auftraten.
	 * 
	 * @param a_address
	 *            Die Adresse wohin eine Verbindung aufgebaut werden soll.
	 */
	public IStreamConnection connect(SkypeAddress a_address)
			throws ConnectionException {
		Application app;

		// are we trying to connect to ourself?
		try {
			String localID = Skype.getProfile().getId();
			if (localID.equals(a_address.getUserID()))
				throw new CommunicationException(
						"No selfconection over Skype allowed");
		} catch (SkypeException e) {
			LogHolder.log(LogLevel.WARNING, LogType.TRANSPORT,
					"Unable to get local Skype User ID");
		}

		// so we try to register the application
		try {
			app = Skype.addApplication(a_address.getApplicationName());
		} catch (SkypeException e) {
			throw new CommunicationException(
					"Unable to create desired Skype Application "
							+ a_address.getApplicationName());
		}

		// did we got the application
		if (app == null)
			throw new CommunicationException(
					"Unable to create desired Skype Application "
							+ a_address.getApplicationName());

		// so we try to get an stream
		Stream[] connectionStreams;
		try {
			connectionStreams = app.connect(new String[] { a_address
					.getUserID() });
		} catch (SkypeException e) {
			throw new CommunicationException(
					"Unable to connect to User with ID "
							+ a_address.getUserID());
		}
		if ((connectionStreams == null) || (connectionStreams.length == 0))
			throw new CommunicationException(
					"Unable to connect to User with ID "
							+ a_address.getUserID());
		// we got at least on stream. let's build the base chunk connection
		IChunkConnection baseConnection = new SkypeConnection(
				connectionStreams[0]);

		// an make an StreamConnection out of it
		return new ChunkConnectionAdapter(baseConnection);
	}

	public IConnection connect(IAddress a_address) throws ConnectionException {
		if (!(a_address instanceof SkypeAddress))
			throw new IllegalArgumentException(
					"Connector can only handel Address of type SkypeAddress");
		return connect((SkypeAddress) a_address);
	}

}
