package anon.transport.address;

/**
 * Spezialisierung von {@link Endpoint} fuer die Bestimmung von Endpunkten auf
 * Basis von Skype.
 */

public class SkypeAddress implements IAddress {

	public static final String TRANSPORT_IDENTIFIER = "skype";

	private static final String USER_PARAMETER = "user";
	private static final String APP_PARAMETER = "application";
	/**
	 * Die Benutzer-ID
	 */
	protected String m_user;

	/**
	 * Der Applications Name
	 */
	protected String m_app;

	/**
	 * Erstellt eine neue Skype Adresse auf Basis der uebergebenen Benutzer-ID
	 * und dem zugehoerigen Applications Namen.
	 */
	public SkypeAddress(String a_userID, String a_applicationName) {
		m_user = a_userID;
		m_app = a_applicationName;
	}

	/**
	 * Erstellt eine neue {@link SkypeAddress}, wobei die Belegung fuer
	 * Benutzekennung und Application Name aus den entsprechenden Parametern
	 * eines uebergebenen Endpoint ermittelt wird.
	 * 
	 * @throws AddressMappingException
	 */
	public SkypeAddress(Endpoint a_baseAddress) throws AddressMappingException {

		m_user = a_baseAddress.getParameter(USER_PARAMETER);
		if (m_user == null)
			throw new AddressMappingException("User-ID Parameter is missing");
		m_app = a_baseAddress.getParameter(APP_PARAMETER);
		if (m_app == null)
			throw new AddressMappingException(
					"Applicationname Parameter is missing");
	}

	/**
	 * Liefert die Benutzer ID der Adresse zurueck.
	 */
	public String getUserID() {
		return m_user;
	}

	/**
	 * Liefert den Applications Namen der Adresse.
	 */
	public String getApplicationName() {
		return m_app;
	}

	public String getTransportIdentifier() {
		return TRANSPORT_IDENTIFIER;
	}

	public AddressParameter[] getAllParameters() {
		AddressParameter[] result = new AddressParameter[2];
		result[0] = new AddressParameter(USER_PARAMETER, m_user);
		result[1] = new AddressParameter(APP_PARAMETER, m_app);
		return result;
	}
}
