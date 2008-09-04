package anon.transport.connection;

/**
 * Spezielle Ausnahme um anzuzeigen, das ein bestimmter Verbindungsbefehl nicht
 * unterstuetzt wird.
 */
public class UnsuportedCommandException extends ConnectionException {

	/**
	 * Initiale Version der Ausnahme
	 */
	private static final long serialVersionUID = 1L;

	public UnsuportedCommandException(Throwable cause) {
		super(cause);
	}

	public UnsuportedCommandException(String message) {
		super(message);
	}

}
