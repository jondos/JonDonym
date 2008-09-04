package anon.transport.connection;

/**
 * Spezielisierung von {@link ConnectionException}, welche den Ursprung eines
 * Problems als Teil der Kommunikation zwischen den beiden Endpunkten bestimmt.
 * Sollte eingesetzt werden, wenn einer der Endpunkte nicht erreichbar ist oder
 * Fehler innerhalb des Transportsystems nicht mehr durch die Implentierung der
 * Verbindung ausgeglichen werden koennen.
 */
public class CommunicationException extends ConnectionException {

	/**
	 * Initiale Version der Ausnahme
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Erstellt eine neue {@link CommunicationException} auf Basis einer
	 * bestimmten Fehlermeldung
	 */
	public CommunicationException(String message) {
		super(message);
	}

	/**
	 * Erstellt eine neue {@link CommunicationException} auf Basis einer bereits
	 * eigetretenen Ausnahme.
	 */
	public CommunicationException(Throwable cause) {
		super(cause);
	}

}
