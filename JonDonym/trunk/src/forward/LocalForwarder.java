package forward;

import anon.transport.address.IAddress;
import anon.transport.connection.ChunkConnectionAdapter;
import anon.transport.connection.CommunicationException;
import anon.transport.connection.ConnectionException;
import anon.transport.connection.IConnection;
import anon.transport.connection.IStreamConnection;
import anon.transport.connection.RequestException;
import anon.transport.connection.util.QueuedChunkConnection;
import anon.transport.connector.IConnector;
import anon.util.ObjectQueue;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

//import transport.connection.IConnection.ConnectionState;
import forward.server.ForwardScheduler;
import forward.server.ForwardServerManager;
import forward.server.IServerManager;

/**
 * Sammlung aller fuer die Behandlung von lokalen Forwarding noetigen Methoden.
 */
public class LocalForwarder implements IServerManager,
		IConnector/*<LocalAddress, IStreamConnection>*/ {

	/** Die Singleton Instanz */
	private static LocalForwarder m_instance = new LocalForwarder();

	/** Die ID wenn wir als ServerManager registriert sind, ansonsten null. */
	private static Object m_currentServerManagerId = null;

	/**
	 * Die noetigen Methoden um den lokalen Forwarder serverseitig einzurichten.
	 */
	public static void registerLocalForwarder(int a_bandwith) {
		// taken from JAPRoutingSettings
		// we neat to bypass this, because it can't handle
		// Client- and Servermode at the same time
		if (m_currentServerManagerId != null)
			return; // we have allready registert
		ForwardServerManager.getInstance().startForwarding();
		ForwardServerManager.getInstance().setNetBandwidth(a_bandwith);
		ForwardServerManager.getInstance().setMaximumNumberOfConnections(1);
		m_currentServerManagerId = ForwardServerManager.getInstance()
				.addServerManager(m_instance);
		LogHolder.log(LogLevel.NOTICE, LogType.TRANSPORT,
				"Local Forwarder registert");
	}

	/**
	 * Die noetigen Schritte um die Einrichtung des lokalen Forwarders rueckgaengig
	 * zu machen.
	 */
	public static void unregisterLocalForwarder() {
		// taken from JAPRoutingSettings
		// we neat to bypass this, because it can't handle
		// Client- and Servermode at the same time
		if (m_currentServerManagerId == null)
			return; // not registert
		ForwardServerManager.getInstance().shutdownForwarding();
		ForwardServerManager.getInstance().removeServerManager(
				m_currentServerManagerId);
		m_currentServerManagerId = null;
		LogHolder.log(LogLevel.NOTICE, LogType.TRANSPORT,
				"Local Forwarder removed");
	}

	/**
	 * Liefert den Server Manager fuer lokales Forwarding.
	 */
	public static IServerManager getServerManager() {
		return m_instance;
	}

	/**
	 * Liefert den Connector um eine lokale Verbindung einzurichten.
	 */
	public static IConnector/*<LocalAddress, IStreamConnection>*/ getConnector() {
		return m_instance;
	}

	/**
	 * Der Scheduler, welcher eingehende Verbindungen behandelt.
	 */
	private ForwardScheduler m_scheduler;

	/**
	 * Sind wir bereit Verbindungen anzunehmen.
	 */
	private boolean m_isListing;

	private LocalForwarder() {
		m_isListing = false;
	}

	/**
	 * Die ID, sofern wird als Servermanager agieren.
	 */
	public Object getId() {
		return this.getClass().getName(); // Name should be enough as we are
		// singleton
	}

	/**
	 * Soll der Servermanager heruntergefahren werden.
	 */
	public void shutdown() {
		m_isListing = false;
		m_scheduler = null;

	}

	public void startServerManager(ForwardScheduler scheduler) throws Exception {
		m_scheduler = scheduler;
		m_isListing = true;
		LogHolder.log(LogLevel.NOTICE, LogType.TRANSPORT,
				"Local Forwarder listning");

	}

	/**
	 * Zentrale Methode um eine neue lokale Verbindung einzurichten.
	 */
	public IStreamConnection connect(LocalAddress a_address)
			throws ConnectionException {
		// Are we even Listening
		if (!m_isListing)
			throw new CommunicationException("Remoteend could not be reached");

		ObjectQueue/*<byte[]>*/ channel1 = new ObjectQueue/*<byte[]>*/(), channel2 = new ObjectQueue/*<byte[]>*/();
		IStreamConnection serverConnection = new ChunkConnectionAdapter(
				new QueuedChunkConnection(channel1, channel2));
		// IMPORTANT: Take care to swap channels
		IStreamConnection clientConnection = new ChunkConnectionAdapter(
				new QueuedChunkConnection(channel2, channel1));
		// give the scheduler the connection
		m_scheduler.handleNewConnection(serverConnection);

		// when Servermanager denied. throw Exception
		if (serverConnection.getCurrentState() == IStreamConnection.ConnectionState_CLOSE)
			throw new RequestException("Reques denied for unknown Reason");
		return clientConnection;
	}

	public IConnection connect(IAddress a_address)
			throws ConnectionException {
		// TODO Auto-generated method stub
		return null;
	}

}
