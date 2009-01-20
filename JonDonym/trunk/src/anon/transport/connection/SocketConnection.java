package anon.transport.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import anon.transport.address.IAddress;
import anon.transport.address.TcpIpAddress;
import anon.transport.connection.util.ClosedInputStream;
import anon.transport.connection.util.ClosedOutputStream;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Adaptiert ein Socket um es im Sinne einer {@link IStreamConnection} zu
 * verwenden.
 * 
 * Da Ein- und Ausgabestrom nur als Paar auftreten und fester Bestandteil der
 * {@link IStreamConnection} sind, wird bereits waehrend der Initlisierung
 * versucht den Ein- und Ausgabestrom des Sockets zu bekommen. Sollte dies nicht
 * moeglich sein, wird die Verbindung bereits zu begin als geschlossen markiert.
 * Deswegen ist es wichtig SocketConnection nur in Verbindung mit offenen und
 * verbunden Sockets zu verwenden.
 * 
 * Ausserdem wird angennomme das sich Ein- und Ausgabestrom des Sockets nicht
 * aendern.
 */
public class SocketConnection implements IStreamConnection {

	/**
	 * Das Socket auf welchem die Verbindung aufbaut
	 */
	private Socket m_underlyingSocket;
	/**
	 * Der Ausgabestrom der Verbindung. Bleibt ueber einen Zustand hinweg
	 * konstant.
	 */
	private OutputStream m_outputStream;
	/**
	 * Der Eingabstrom der Verbindung. Bleibt ebenfalls konstant.
	 */
	private InputStream m_inputStream;
	/**
	 * Der interne Zustand der Verbindung.
	 */
	private int m_internalState;

	/**
	 * Die Adresse des lokalen Endpunktes
	 */
	private IAddress m_localAddress;

	/**
	 * Die Adresse des entfernten Endpunktes
	 */
	private IAddress m_remoteAddress;

	/**
	 * Erstellt eine neue SocketConnection auf Basis des uebergebenen Sockets.
	 * <p>
	 * Das uebergebene Socket darf nicht geschlossen sein, da im Sinne einer
	 * Verbindung, die Kommunikationsbeziehung bereits eingerichtet sein muss.
	 * 
	 * @param a_underlyingSocket
	 *            Das offene und verbunde Socket, welches als Basis der
	 *            Verbinung genutzt werden soll
	 */
	public SocketConnection(Socket a_underlyingSocket) {
		// we don't deal with closed socket, as they don't
		// represent a connection
		/*if (a_underlyingSocket.isClosed()) {
			setCLOSE();
			LogHolder.log(LogLevel.WARNING, LogType.TRANSPORT,
					"Socket was allready close when creating Connection");
			return;
		}*/

		m_localAddress = new TcpIpAddress(a_underlyingSocket.getLocalAddress(),
				a_underlyingSocket.getLocalPort());
		m_remoteAddress = new TcpIpAddress(a_underlyingSocket.getInetAddress(),
				a_underlyingSocket.getPort());

		try {
			m_underlyingSocket = a_underlyingSocket;
			m_inputStream = m_underlyingSocket.getInputStream();
			m_outputStream = m_underlyingSocket.getOutputStream();
			m_internalState = ConnectionState_OPEN;
		} catch (IOException e) {
			// beim ersten Fehler geben wir auf
			setCLOSE();
		}
	}

	/**
	 * Interne Hilfsfunktion, welche beim Wechsel in den CLOSE-Zustand
	 * augerufen, werden sollte.
	 */
	private void setCLOSE() {
		m_internalState = ConnectionState_CLOSE;
		m_underlyingSocket = null; // let the garbagecolector get it
		m_inputStream = ClosedInputStream.getNotCloseable();
		m_outputStream = ClosedOutputStream.getNotCloseable();
	}

	public InputStream getInputStream() {
		return m_inputStream;
	}

	public OutputStream getOutputStream() {
		return m_outputStream;
	}

	public synchronized int getTimeout() throws ConnectionException {
		if (m_internalState == ConnectionState_CLOSE)
			throw new ConnectionException("Connection is already closed");
		try {
			return m_underlyingSocket.getSoTimeout();
		} catch (SocketException e) {
			throw new ConnectionException(e);
		}
	}

	public synchronized void setTimeout(int value) throws ConnectionException {
		if (m_internalState == ConnectionState_CLOSE)
			throw new ConnectionException("Connection is already closed");
		try {
			m_underlyingSocket.setSoTimeout(value);
		} catch (SocketException e) {
			throw new ConnectionException(e);
		}

	}

	public synchronized void close() throws IOException {
		if (m_internalState == ConnectionState_OPEN) {
			//if (!m_underlyingSocket.isClosed())
				m_underlyingSocket.close();
			setCLOSE();
		}
	}

	public int getCurrentState() {
		return m_internalState;
	}

	public IAddress getLocalAddress() {
		return m_localAddress;
	}

	public IAddress getRemoteAddress() {
		return m_remoteAddress;
	}

}
