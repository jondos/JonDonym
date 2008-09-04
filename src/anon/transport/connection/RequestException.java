package anon.transport.connection;

/**
 * Eine Spezialisierung von {@link CommunicationException}, welche angibt, dass
 * der Verbindungswunsch von der Gegenstelle abgelehnt wurde.
 */
public class RequestException extends ConnectionException {

	/**
	 * Auflistung moeglcher Gruende, fuer das Ablehnen der Verbindung.
	 */
	//enum Reason {
		/** Der Grund konnte nicht ermittelt werden */
		public final static int Reason_UNKNOWN=1;
		/** Die Gegenstelle ist ausgelastet */
		public final static int Reason_SERVER_BUSY=2;
		/** Die falschen oder generell fehlende Qualifikationen */
		public final static int Reason_MISSING_CREDENTIALS=3;
		/** jeglicher weitere Grund */
		public final static int Reason_OTHER=4;
	//}

	/**
	 * Initiale Version der Ausnahme
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Der Grund, aus welchem der Verbindungswunsch abgelehnt wurde.
	 */
	private int m_reason;

	/**
	 * Erstellt eine neue {@link RequestException} auf Basis einer bereits
	 * existierenden Ausnahme, mit dem angegeben Grund
	 * 
	 * @param a_reason
	 *            Der Grund fuer die Ablehnung.
	 */
	public RequestException(Throwable cause, int a_reason) {
		super(cause);
		m_reason = a_reason;
	}

	/**
	 * Erstellt eine neue {@link RequestException} auf Basis einer bereits
	 * existierenden Ausnahme.
	 * <p>
	 * Der Grund wird als unbekannt angesehen.
	 */
	public RequestException(Throwable cause) {
		super(cause);
		m_reason = Reason_UNKNOWN;
	}

	/**
	 * Erstellt eine neue {@link RequestException} auf Basis einer Begruendung,
	 * mit dem angegeben Grund
	 * 
	 * @param a_reason
	 *            Der Grund fuer die Ablehnung.
	 */
	public RequestException(String message, int a_reason) {
		super(message);
		m_reason = a_reason;
	}

	/**
	 * Erstellt eine neue {@link RequestException} auf Basis einer Begruendung.
	 * <p>
	 * Der Grund wird als unbekannt angesehen.
	 */
	public RequestException(String message) {
		super(message);
		m_reason = Reason_UNKNOWN;
	}

	/**
	 * Liefert den Grund der Ablehnung zurueck.
	 */
	public int getReason() {
		return m_reason;
	}

}
