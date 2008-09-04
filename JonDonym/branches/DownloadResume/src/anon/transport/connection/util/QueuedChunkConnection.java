package anon.transport.connection.util;

import java.io.IOException;

import anon.transport.address.AddressParameter;
import anon.transport.address.IAddress;
import anon.transport.connection.ConnectionException;
import anon.transport.connection.IChunkConnection;
import anon.transport.connection.IChunkReader;
import anon.transport.connection.IChunkWriter;
import anon.transport.connection.UnsuportedCommandException;
import anon.util.ObjectQueue;


/**
 * Implementierung einer {@link IChunkConnection}, welcher die 2 Kanaele der
 * bidirektionalen Kommunikation durch zwei {@link BlockingQueue} ueber byte[]
 * umsetzt.
 */
public class QueuedChunkConnection implements IChunkConnection {

	private class QueuedAddress implements IAddress {

		private String m_identifier;

		public QueuedAddress(String a_identifier) {
			m_identifier = a_identifier;
		}

		public AddressParameter[] getAllParameters() {
			return new AddressParameter[0];
		}

		public String getTransportIdentifier() {
			return m_identifier;
		}

	}

	/** Der reader der Verbindung */
	private final QueuedChunkReader m_reader;

	/** Der Writer der Verbindung */
	private final IChunkWriter m_writer;

	/** Der interne Zustand der Verbindung */
	private int m_state;

	/** Die lokale Addresse */
	protected IAddress m_localAddress;

	/** Die Adresse des entfernten Endpunkt */
	protected IAddress m_remoteAddress;

	/**
	 * Erstellt einen neue {@link QueuedChunkConnection} bei der Ein- und
	 * Ausgabequeue durch die selbe {@link BlockingQueue} repraesentiert werden
	 * und die Connection entsprechend mit sich selbst verbunden ist.
	 * <p>
	 * Die Adresse fuer lokales und entferntes Ende werden auf die selbe Instanz
	 * gesetzt und erhalten den Identifier "loopback".
	 * 
	 * @param a_loopbackQueue
	 *            Die BlockingQueue welche als Ein- und Ausgabequeue dient
	 */
	public QueuedChunkConnection(ObjectQueue/*<byte[]>*/ a_loopbackQueue) {
		this(a_loopbackQueue, a_loopbackQueue);
		m_localAddress = new QueuedAddress("loopback");
		m_remoteAddress = m_localAddress;
	}

	/**
	 * Erstell eine neue {@link QueuedChunkConnection}, bei welcher Ein- und
	 * Ausgabekanal durch entsprechende {@link BlockingQueue} repraesentiert
	 * sind.
	 * <p>
	 * Die Adressen des lokalen, wie des entfernten, Endpunktes erhalten den
	 * Identifier "queue".
	 * 
	 * @param a_readingQueue
	 *            Die Queue, welche als Grundlage der lesenden Operationen der
	 *            Verbindung Verwendung findet.
	 * 
	 * @param a_writingQueue
	 *            Die Queue, welche als Grundlage der schreiben Operationen der
	 *            Verbindung Verwendung findet.
	 */
	public QueuedChunkConnection(ObjectQueue/*<byte[]>*/ a_readingQueue,
			ObjectQueue/*<byte[]>*/ a_writingQueue) {
		m_reader = new QueuedChunkReader(a_readingQueue);
		m_writer = new QueuedChunkWriter(a_writingQueue);
		m_state = ConnectionState_OPEN;
		m_localAddress = new QueuedAddress("queue");
		m_remoteAddress = new QueuedAddress("queue");
	}

	public IChunkReader getChunkReader() {
		return m_reader;
	}

	public IChunkWriter getChunkWriter() {
		return m_writer;
	}

	public int getCurrentState() {
		return m_state;
	}

	public IAddress getLocalAddress() {
		return m_localAddress;
	}

	public IAddress getRemoteAddress() {
		return m_remoteAddress;
	}

	public int getTimeout() throws ConnectionException {
		return 0;
	}

	public void setTimeout(int value) throws ConnectionException {
		throw new UnsuportedCommandException("Timeout is not changeable");
		// TODO: we actuall could set it, as the queued Reader and Writer
		// support it.
		// But the actuell behavior works and it's to dangerous to break
		// something.
	}

	/**
	 * Schliesst die Verbindung, indem der interne Zustand auf geschlossen
	 * gesetzt wird.
	 * <p>
	 * Da noch Leseoperationen anstehen koennen, wird der Reader nur
	 * heruntergefahren um die Leerung der Buffers zu erlauben.
	 */
	public void close() throws IOException {
		if (m_state != ConnectionState_OPEN) {
			m_state = ConnectionState_CLOSE;
			m_writer.close();
			m_reader.tearDown();
		}
	}
}
