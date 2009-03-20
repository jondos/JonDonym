package anon.transport.connection;

/**
 * Allgemeine Oberklasse aller Ausnahme im Zusamenhang mit Verbindungen.
 */
public class ConnectionException extends Exception {

	/**
	 * Initiale Version der Ausnahme
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Erstellt eine Verbindungsausnahme, auf Basis einer anderen Ausnahme.
	 */
	public ConnectionException(Throwable cause) {
		super((cause==null ? null : cause.toString()) );
	}

	/**
	 * Erstellt eine Verbindungsausnahme, mit der angegeben Begruendung.
	 */
	public ConnectionException(String message) {
		super(message);
	}

}
