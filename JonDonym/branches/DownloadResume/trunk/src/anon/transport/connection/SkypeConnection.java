package anon.transport.connection;

import java.io.IOException;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.transport.address.IAddress;
import anon.transport.address.SkypeAddress;
import anon.transport.connection.util.QueuedChunkReader;
import anon.util.Base64;
import anon.util.ObjectQueue;

import com.skype.Application;
import com.skype.ApplicationListener;
import com.skype.Skype;
import com.skype.SkypeException;
import com.skype.Stream;
import com.skype.StreamListener;

/**
 * Implementierung einer Chunk basierten Verbindung, welche Skype fuer den
 * Transport der Daten verwendet.
 * <p>
 * Aenderungen des internen Zustandes (offen, geschlossen) von aussen, sprich
 * durch Skype oder den Kommunikationspartner werden wahrgenommen und
 * entsprechend ausgewertet (@see {@link #m_listner}).
 */
public class SkypeConnection implements IChunkConnection {

	/**
	 * Das inaktivitaets Time Out, nach welchem zu erwarten ist, das eine
	 * Verbindung zwangsweise geschlossen wird.
	 * 
	 * @see https://developer.skype.com/Docs/ApiDoc/Application_to_application_commands
	 */
	public final static int IDLE_TIME_OUT = 8 * 60 * 1000; // milliseconds

	/**
	 * Umsetzung eines {@link IChunkReader} welcher speziell auf die Eigenheiten
	 * der Skype Verbindung zugeschnitten ist.
	 * <p>
	 * Da weder Skype noch Skype4Java einen gepufferten Empfang von Nachrichen
	 * unterstuetzen, erzeugt die Implementierung intern eine Instanz eines
	 * {@link QueuedChunkReader} um diesen asynchron von Skype fuellen zulassen.
	 */
	private static class SkypeReader implements IChunkReader {

		/**
		 * Die maximal Laenge einer Nachricht, wie sie ueber Skype verschickt
		 * werden darf.
		 * 
		 * @see https://developer.skype.com/Docs/ApiDoc/Application_to_application_commands
		 */
		public final static int MAX_MESSAGE_LENGTH = 0xFFFF;

		/**
		 * Der Empfangspuffer
		 */
		private ObjectQueue/*<byte[]>*/ m_readBuffer;

		/**
		 * Das Skype Stream Object, welches den Kanal ueber Skype repraesentiert.
		 */
		private Stream m_appStream;

		/**
		 * Der Listner fuer ankommende Nachrichten
		 */
		private StreamListener m_listner;

		/**
		 * Der eigentliche Reader, welcher den gepufferten Empfang und die
		 * Auslieferung von Chunks umsetzt.
		 */
		private QueuedChunkReader m_baseReader;

		/**
		 * Erstellt einen neuen reader auf Basis des uebergeben Skype Stream
		 * 
		 * @param a_appStream
		 *            Der Skype Strom, welcher als Datenkanal genutzt werden
		 *            soll.
		 */
		public SkypeReader(Stream a_appStream) {
			m_appStream = a_appStream;
			// we use an quasi infinite Buffer to avoid blocking in the listener
			// Thread
			// and possible missing of Messages
			m_readBuffer = new ObjectQueue();
			m_baseReader = new QueuedChunkReader(m_readBuffer);
			m_listner = new StreamListener() {

				public void textReceived(String aa_message)
						throws SkypeException {
					// all data is transported in base64
					byte[] packet = Base64.decode(aa_message);
					if (packet != null && packet.length > 0)
							// the packet presents directly the transmitted
							// chunk
							m_readBuffer.push(packet);
				}

				public void datagramReceived(String arg0) throws SkypeException {
					LogHolder
							.log(LogLevel.WARNING, LogType.TRANSPORT,
									"Received Datagram from Skype, but we only expect Streams.");
				}

			};
			m_appStream.addStreamListener(m_listner);
		}

		public int availableChunks() throws ConnectionException {
			return m_baseReader.availableChunks();
		}

		public byte[] readChunk() throws ConnectionException {
			return m_baseReader.readChunk();
		}

		/**
		 * Entfernt den Listener und schliesst den internen Reader. Der Stream
		 * beleibt allerdings unberuehrt, da dieser die komplette Verbindung
		 * repraesentiert.
		 */
		public void close() throws IOException {
			m_appStream.removeStreamListener(m_listner);
			m_baseReader.close();
		}

		/**
		 * Entfernt den Listener und faehrt den internen Reader runter. Der
		 * Stream beleibt allerdings unberuehrt, da dieser die komplette
		 * Verbindung repraesentiert.
		 */
		public void tearDown() throws IOException {
			m_appStream.removeStreamListener(m_listner);
			m_baseReader.tearDown();
		}

	}

	/**
	 * Umsetzung eines {@link IChunkWriter} welcher speziell auf die Eigenheiten
	 * der Skype Verbindung zugeschnitten ist.
	 * <p>
	 * Die Uebertragung erfolgt indem die eigentlichen binaer Daten mit Hilfe von
	 * Base64 in einen String umgewandelt werden. Dies erfolgt in Anlehnung an
	 * den Hinweis in
	 * {@link https://developer.skype.com/Docs/ApiDoc/Application_to_application_commands}
	 */
	private static class SkypeWriter implements IChunkWriter {

		/**
		 * Das Skype Stream Object, welches den Kanal ueber Skype repraesentiert.
		 */
		private Stream m_appStream;

		/**
		 * gibt an ob der Writer offen oder geschlossen ist.
		 */
		private boolean m_isClosed;

		/**
		 * Erstellt einen neuen Writer af Basis des uebergebenen Skype Stream
		 * 
		 * @param a_appStream
		 *            Der Skype Stream, welcher zur Uebertragung von Daten
		 *            genutzt werden soll.
		 */
		public SkypeWriter(Stream a_appStream) {
			m_appStream = a_appStream;
			m_isClosed = false;
		}

		/**
		 * Versucht einen Chunk mit Hilfe von Skype zu uebertragen. Nach
		 * {@link IChunkWriter#writeChunk(byte[])} blockiert der Aufruf, bis der
		 * Chunk in den Kanal eingefuegt werden konnte oder gibt das Scheitern
		 * dieses Versuches durch eine entsprechende Ausnahme an.
		 * <p>
		 * die Uebertragung erfolgt dabei, indem der Inhalt des Chunks Base64
		 * kodiert uebertragen werden.
		 */
		public void writeChunk(byte[] a_chunk) throws ConnectionException {
			if (!m_isClosed) {
				String message = Base64.encode(a_chunk, false);
				try {
					m_appStream.write(message);
				} catch (SkypeException e) {
					throw new ConnectionException(e);
				}
			}
		}

		/**
		 * Schliesst der Reader aber laesst den internen Stream unberuehrt, da
		 * dieser die komplette Verbindung repraesentiert.
		 */
		public void close() throws IOException {
			m_isClosed = true;
		}

	}

	/**
	 * Der Reader fuer die Verbindung.
	 */
	private final SkypeReader m_reader;

	/** Der Writer der Verbindung */
	private final SkypeWriter m_writer;

	/** Die Adresse des lokalen Endunktes der Verbindung */
	private final SkypeAddress m_localAddress;

	/** Die Adresse des entfernten Endpunktes des Verbindung */
	private final SkypeAddress m_remoteAddress;

	/**
	 * Das ApplicationsObject welches mit dem uebergebene Steam Verbunden ist
	 */
	private final Application m_application;

	/** Das Stream Object, welches die Basis der verbindung Bildet */
	private final Stream m_appStream;

	/** Der interne Zustand der Verbindung. */
	private int m_state;

	/**
	 * Der Listner fuer Veraenderungen an dem Zustand der Verbindung, sofern diese
	 * von aussen (Skype, RemoteEnd) ausgeloesst werden.
	 */
	private ApplicationListener m_listner;

	/**
	 * Erstellt eine neue Verbindung auf Basis eines bereits offenen Skype
	 * Streams.
	 * <p>
	 * Adresse des lokalen und entfernten Endpunktes werden dynamisch bestimmt
	 * und Instanzen von Reade und Writer initalisiert.
	 * 
	 * @param a_appStream
	 *            Der Stream welcher als Basis der Verbindung agiert.
	 */
	public SkypeConnection(Stream a_appStream) {
		if (a_appStream == null)
			throw new NullPointerException("No Application Stream provided");
		m_appStream = a_appStream;
		m_reader = new SkypeReader(m_appStream);
		m_writer = new SkypeWriter(m_appStream);
		m_application = m_appStream.getApplication();
		String appName = m_application.getName();
		String remoteUserID = m_appStream.getFriend().getId();
		String localUserID = "<unresolved>";
		try {
			localUserID = Skype.getProfile().getId();
		} catch (SkypeException e) {
			LogHolder.log(LogLevel.WARNING, LogType.TRANSPORT,
					"Unable to resolve local Skype User ID.");
		}
		m_localAddress = new SkypeAddress(localUserID, appName);
		m_remoteAddress = new SkypeAddress(remoteUserID, appName);
		m_state = ConnectionState_OPEN;
		m_listner = new ApplicationListener() {

			public void connected(Stream arg0) throws SkypeException {
				// we do not handle new connection requests

			}

			public void disconnected(Stream a_disconnectingStream)
					throws SkypeException {
				if (a_disconnectingStream.getId().equals(m_appStream.getId()))
					try {
						close(false);
					} catch (IOException e) {
						// just ignore
					}

			}

		};
		m_application.addApplicationListener(m_listner);
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

	/**
	 * Da Skype nicht das setzen eines TimeOut unterstuetzt, wird entsprechend
	 * ein unendliches TimeOut mit der Verbindung verknuepft.
	 * 
	 * @return Immer 0
	 */
	public int getTimeout() throws ConnectionException {
		// always unlimited
		return 0;
	}

	/**
	 * Wirft immer eine {@link UnsuportedCommandException}, da Skype keine
	 * Unterstuetzung fuer Timeout besitzt.
	 */
	public void setTimeout(int value) throws ConnectionException {
		throw new UnsuportedCommandException(
				"Timeout could not be changed for Connection of Skype");

	}

	/**
	 * Schliesst die Verbindung, indem der Listner entfernt wird und Reader und
	 * Writer geschlossen werden.
	 * <p>
	 * Gegebenfalls wird noch versucht den Stream zu schliessen.
	 * 
	 * @param a_disconnectStream
	 *            Gibt an ob auch Versucht werden soll den Stream zu schliessen.
	 */
	public void close(boolean a_disconnectStream) throws IOException {
		if (m_state != ConnectionState_CLOSE) {
			m_state = ConnectionState_CLOSE; // only done ones
			m_application.removeApplicationListener(m_listner);
			m_reader.tearDown();
			m_writer.close();
			try {
				if (a_disconnectStream)
					m_appStream.disconnect();
			} catch (SkypeException e) {
				throw new IOException(e.getMessage());
			}
		}
	}

	/**
	 * Schliesst die Verbindung, indem der Listner entfernt wird und Reader und
	 * Writer geschlossen werden.
	 * <p>
	 * Es wird auf jeden Fall versucht den Stream zu schliessen.
	 */
	public void close() throws IOException {
		close(true);
	}

}
