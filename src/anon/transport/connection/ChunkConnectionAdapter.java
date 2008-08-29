package anon.transport.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import anon.transport.address.IAddress;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Adaptiert eine bestehende {@link IChunkConnection} um darauf aufbauend, eine
 * {@link IStreamConnection} bereitzustellen. Hierzu exitieren interne
 * Implentierung von {@link InputStream} und {@link OutputStream} welche den
 * Paketbasierten Transport von byte-Stroemen organisieren.
 */
public class ChunkConnectionAdapter implements IStreamConnection {

	/**
	 * Die Menge der moegliche Zustaende, welche die internen Umsetzungen von
	 * {@link InputStream} und {@link OutputStream} annehmen koennen.
	 */
//	private enum StreamState {
		/** Der Strom ist offen */
	private final static int StreamState_OPEN=1;
		/** Der Strom wird nach Bearbeitung des letzten Bytes geschlossen */
	private final static 	int StreamState_EOF=2;
		/** Der Strom ist geschlossen */
	private final static int	StreamState_CLOSE=3;
//	}

	/**
	 * Konstante um anzuzeigen, das ein Paket nicht das Ende des Stroms
	 * signalisiert
	 */
	private static final byte DATA_PACKET = 0;
	/**
	 * Konstante um anzuzeigen, das ein Paket das letzte uebertragene Paket
	 * darstellt
	 */
	private static final byte EOF_PACKET = -1;

	/**
	 * Die Basisverbindung, welche zur Uebertragung der eigentlichen Daten
	 * genutzt wird
	 */
	private IChunkConnection m_underliningConnection;

	/** Die Eingabestrom der Verbindung */
	private ChunkInputStream m_inputstream;

	/** Der Ausgabestrom der Verbindung */
	private ChunkOutputStream m_outputstream;

	/**
	 * Implentierung eines {@link InputStream}, welcher die durch
	 * {@link #read()} zurueckgegeben Bytes aus den einzelnen Chunks eines
	 * {@link IChunkReader} nimmt.
	 */
	private static class ChunkInputStream extends InputStream {

		/** Der Reader welcher die Chunks und somit die Daten bereitstellt */
		private IChunkReader m_reader;

		/**
		 * Der interne Buffer stellt den zuletzt gelesen Chunk dar und ist die
		 * Basis fuer die durch {@link #read()} zurueckgegeben Byes
		 */
		private byte[] m_buffer;

		/**
		 * Die aktuelle Position innerhalb des Buffer ab welcher Daten
		 * zurueckgegeben werden.
		 */
		private int m_readPos;

		/** Der interne Zustand des Streams */
		private int/*StreamState*/ m_state;

		/**
		 * Erstellt einen neuen {@link ChunkInputStream} auf Basis des uebergeben
		 * Readers
		 * 
		 * @param Der
		 *            Reader, welcher die einezelnen Chunks und somit Daten
		 *            liefert.
		 */
		public ChunkInputStream(IChunkReader a_reader) {
			m_reader = a_reader;
			m_readPos = 0;
			m_buffer = new byte[0];
			m_state = ChunkConnectionAdapter.StreamState_OPEN;
		}

//		@Override
		public synchronized int read() throws IOException {
			if (m_state == ChunkConnectionAdapter.StreamState_CLOSE)
				throw new IOException("Stream is allready closed");
			// last byte readed?
			while (m_readPos == m_buffer.length) {
				// should we deliver EOF and close the stream
				if (m_state == ChunkConnectionAdapter.StreamState_EOF) {
					m_state = ChunkConnectionAdapter.StreamState_CLOSE;
					return -1;
				}
				// or just update the buffer
				updateBuffer();
			}
			// we only get here when at least one byte is in the buffer
			return m_buffer[m_readPos++] & 255; // use the bits to actual cast
												// the byte

		}

		/**
		 * Interne Funktion um den Buffer durch einen neuen Chunk aufzufrischen
		 * und gegebenfalls den internen Zustand des Streams anzupassen.
		 * <p>
		 * Einzige wirklich blockierende Aufruf.
		 */
		private void updateBuffer() throws IOException {
			byte[] packet;
			try {
				packet = m_reader.readChunk();
			} catch (ConnectionException e) {
				throw new IOException(e.getMessage());
			}
			if (packet == null)
				throw new IOException(
						"Wrong implementation of IChunkReader.readChunk()."
								+ "Should never return null.");
			if (packet.length == 0)
				throw new IOException("Recieved Packet is to small");
			if (packet[0] == ChunkConnectionAdapter.EOF_PACKET)
				m_state = ChunkConnectionAdapter.StreamState_EOF;
			m_buffer = new byte[packet.length - 1];
			System.arraycopy(packet, 1, m_buffer, 0, m_buffer.length);
			m_readPos = 0;
		}

		/**
		 * Liefert die Anzahl der verbleibenen Bytes innerhalb des Buffers
		 * zurueck. Sofern der Buffer leer ist und mindestens 1 Chunk aus den
		 * Kanal gelesen werden kann, wird der Buffer aufgefrischt und der
		 * aktuelle Wert zurueckgegeben.
		 */
		public int available() throws IOException {
			try {
				// could we update
				if (m_buffer.length - m_readPos == 0)
					if (m_reader.availableChunks() > 0)
						updateBuffer();
				return m_buffer.length - m_readPos;
			} catch (ConnectionException e) {
				throw new IOException(e.getMessage());
			}
		}

		/**
		 * Schliesst den Strom und den zugrundelegenden Reader
		 */
		public void close() throws IOException {
			m_state = ChunkConnectionAdapter.StreamState_CLOSE;
			m_reader.close();
		}

	}

	/**
	 * Private Implementierung eines {@link OutputStream}, welcher die durch
	 * {@link #write()} uebergeben Bytes als Chunk mit Hilfe eines
	 * {@link IChunkWriter} sendet.
	 */
	private static class ChunkOutputStream extends OutputStream {

		/**
		 * Die Groesse des Buffers und somit auch die Maximalanzahl der in einem
		 * Rutsch uebertragen Bytes.
		 */
		private final static int BUFFER_SIZE = 1000;

		/** Der Writer, ueber welchem die Daten transportiert werden. */
		private IChunkWriter m_writer;

		/** Der interne Buffer, welcher die zu uebertragenen Bytes sammelt. */
		private byte[] m_buffer;

		/**
		 * Die Position, ab welcher neue Bytes in den Buffer eingefuegt werden
		 * koennen.
		 */
		private int m_writePos;

		/** Der interne Zustand des Stroms */
		private int/*StreamState*/ m_state;

		/**
		 * Erstellt einen neuen ChunkOutputStream auf Basis des uebergebenen
		 * Writers.
		 */
		public ChunkOutputStream(IChunkWriter a_writer) {
			m_writer = a_writer;
			m_buffer = new byte[BUFFER_SIZE];
			m_writePos = 0;
			m_state = ChunkConnectionAdapter.StreamState_OPEN;
		}

		/**
		 * Fuegt das Uebergebene Byte in den internen Buffer ein und uebertraegt
		 * diesen, falls er dadurch erschoepft ist.
		 */
		public void write(int b) throws IOException {
			if (m_state == ChunkConnectionAdapter.StreamState_CLOSE)
				throw new IOException("Stream allready closed");
			/*
			 * ignore -1 as EOF if (b == -1) { m_state =
			 * ChunkConnectionAdapter.StreamState.EOF; flush(); // will
			 * automatically set state to closed return; }
			 */
			m_buffer[m_writePos++] = (byte) (b & 255);
			// should we flush
			if (m_writePos == m_buffer.length)
				flush();

		}

		/**
		 * Veranlasst die sofortige Uebertragung des Buffers. Wenn es sich bei dem
		 * aktuellen Paket um das letzte des Stroms handelt, wird diese
		 * zusaetzlich noch als geschlossen markiert.
		 */
		public void flush() throws IOException {
			byte[] packet = new byte[m_writePos + 1];

			// Determinate the type of the packet
			if (m_state == ChunkConnectionAdapter.StreamState_EOF)
				packet[0] = ChunkConnectionAdapter.EOF_PACKET;
			else
				packet[0] = ChunkConnectionAdapter.DATA_PACKET;

			// build packet
			System.arraycopy(m_buffer, 0, packet, 1, m_writePos);

			// and send
			try {
				m_writer.writeChunk(packet);
			} catch (ConnectionException e) {
				throw new IOException(e.getMessage());
			}
			m_buffer = new byte[BUFFER_SIZE];
			m_writePos = 0;

			if (m_state == ChunkConnectionAdapter.StreamState_EOF)
				m_state = ChunkConnectionAdapter.StreamState_CLOSE;
		}

		/**
		 * Schliesst den Strom und den zugrundeliegenen Writer und uebertraegt
		 * zuvor noch den Inhalt des Buffer.
		 */
		public void close() throws IOException {
			if (m_state == ChunkConnectionAdapter.StreamState_CLOSE)
				throw new IOException("Stream allready closed");
			if (m_state == ChunkConnectionAdapter.StreamState_EOF)
				LogHolder
						.log(LogLevel.WARNING, LogType.TRANSPORT,
								"Sync Warning. EOF State should be immediately transfert to CLOSE");
			// try to send what is in the buffer
			m_state = ChunkConnectionAdapter.StreamState_EOF;
			flush();
			// and finally close
			m_state = ChunkConnectionAdapter.StreamState_CLOSE;
			m_writer.close();
		}

	}

	/**
	 * Erstellt einen neuen Adapter auf Basis der uebergebenen
	 * {@link IChunkConnection}.
	 */
	public ChunkConnectionAdapter(IChunkConnection a_underlyingConnection) {
		m_underliningConnection = a_underlyingConnection;
		m_inputstream = new ChunkInputStream(m_underliningConnection
				.getChunkReader());
		m_outputstream = new ChunkOutputStream(m_underliningConnection
				.getChunkWriter());
	}

	public InputStream getInputStream() {
		return m_inputstream;
	}

	public OutputStream getOutputStream() {
		return m_outputstream;
	}

	/**
	 * Liefert den Status der zugrundeliegenen Connection
	 */
	public int getCurrentState() {
		// we simply follow the state of the underlining Connection
		// the only problem could be, that the underlining State changes without
		// our notice
		// but then the read and write Methods will throw an according
		// Exceptions
		return m_underliningConnection.getCurrentState();
	}

	/** Liefert die direkt die Adesse der zugrundeliegenen Verbindung zurueck. */
	public IAddress getLocalAddress() {
		return m_underliningConnection.getLocalAddress();
	}

	/** Liefert die direkt die Adesse der zugrundeliegenen Verbindung zurueck. */
	public IAddress getRemoteAddress() {
		return m_underliningConnection.getRemoteAddress();
	}

	public int getTimeout() throws ConnectionException {
		return m_underliningConnection.getTimeout();
	}

	public void setTimeout(int value) throws ConnectionException {
		m_underliningConnection.setTimeout(value);
	}

	/**
	 * Schliesst die Verbindung in dem nacheinander der Ein- und Ausgabestrom
	 * geschlossen werden und abschliessend die zugrundelegende Verbindung.
	 */
	public void close() throws IOException {
		// try to close the streams
		try {
			m_inputstream.close();
		} catch (IOException e) {
			// just ignore
		}
		try {
			m_outputstream.close();
		} catch (IOException e) {
			// just ignore
		}
		m_underliningConnection.close();
	}

}
