package forward.server;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.transport.connection.ChunkConnectionAdapter;
import anon.transport.connection.IChunkConnection;
import anon.transport.connection.SkypeConnection;

import com.skype.Application;
import com.skype.ApplicationListener;
import com.skype.Skype;
import com.skype.SkypeException;
import com.skype.Stream;

/**
 * {@link IServerManager} um Verbindungsanforderungen ueber Skype
 * entgegenzunehmen.
 */
public class SkypeServerManager implements IServerManager {

	/**
	 * Eigentlicher Listener um ueber Verbindungsanfragen informiert zu werden.
	 * <p>
	 * Wird an eine bestimmte Application gebunden um fuer diese Anfragen
	 * entgegenzunehmen.
	 */
	private class RequestListener implements ApplicationListener {

		/**
		 * Behandelt, seitens Skype, neu eingerichtet Verbindungen.
		 */
		public void connected(Stream a_connectionStream) throws SkypeException {
			// use the same synchronization, as start and shutdown of
			// SkypeServerManager
			synchronized (SkypeServerManager.this) {
				if (!m_isListning || (m_scheduler == null)) {
					a_connectionStream.disconnect();
					return;
				}
				IChunkConnection baseConnection = new SkypeConnection(
						a_connectionStream);
				m_scheduler.handleNewConnection(new ChunkConnectionAdapter(
						baseConnection));
			}
		}

		public void disconnected(Stream a_connectionStream)
				throws SkypeException {
			// disconnects are handled by the Connection itself
		}

	}

	/**
	 * Der Applications Name, und damit die Application an welcher gelauscht
	 * wird.
	 */
	private final String m_appName;

	/** Der Scheduler, welcher die eigentliche Verbindungen verwalted. */
	private ForwardScheduler m_scheduler;

	/** Das Application Object, sofern es eingerichtet werden konnte. */
	private Application m_application;

	/** Der asynchrone Listner fuer neue Verbindungen. */
	private RequestListener m_listner;

	/** Warten wir auf Verbindungsanfragen? */
	private boolean m_isListning;

	/**
	 * Erstellt einen neunen {@link SkypeServerManager} mit dem angegeben
	 * Application Name.
	 * 
	 * @param a_applicationName
	 *            Der Name der Application an welcher auf Verbindungsanfragen
	 *            gelauscht werden soll.
	 */
	public SkypeServerManager(String a_applicationName) {
		m_appName = a_applicationName;
		m_isListning = false;
		;
	}

	/**
	 * Die Eindeutige ID des Servermanager.
	 * <p>
	 * verwendet {@link #toString()}, da diese genuegend Eindeutigkeit umsetzt.
	 */
	public Object getId() {
		return toString();
	}

	/**
	 * Schliesst den {@link ServerManager} indem neue verbindungsanfragen
	 * ignoriert werden.
	 * <p>
	 * Da die Skype API das entfernen einer registrierten Applikation nicht
	 * vorsieht, kann nur der listener entfernt werden. Um die Moeglichkeit des
	 * Kontaktierens vollstaendig auszuschliessen, muesste Skype geschlossen werden.
	 */
	public synchronized void shutdown() {
		if (!m_isListning)
			return;
		m_application.removeApplicationListener(m_listner);
		try {
			m_application.finish();
		} catch (SkypeException e) {
			LogHolder.log(LogLevel.EXCEPTION, LogType.TRANSPORT, e);
		}
		m_scheduler = null;
		m_listner = null;
		m_isListning = false;
	}

	/**
	 * Startet des {@link IServerManager} indem versucht wird eine Appliction
	 * mit dem uebergebene Namen zu registrieren und an dieser auf
	 * Verbindungswuensche zu lauschen.
	 */
	public synchronized void startServerManager(ForwardScheduler a_scheduler)
			throws Exception {
		if (m_isListning)
			return;
		m_scheduler = a_scheduler;
		m_listner = new RequestListener();
		try {
			m_application = Skype.addApplication(m_appName);
			m_application.addApplicationListener(m_listner);
			m_isListning = true;
		} catch (SkypeException e) {
			LogHolder.log(LogLevel.ERR, LogType.TRANSPORT,
					"Could not Start Skype forwarding Server.");
			shutdown();
			throw e;
		}

	}

	/**
	 * String Repraesentation des jeweiligen {@link SkypeServerManager} in der
	 * Form "skype:app(ApplicationName)"
	 */
//	@Override
	public String toString() {
		return "skype:app(" + m_appName + ")";
	}

}
