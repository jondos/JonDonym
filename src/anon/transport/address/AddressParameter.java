package anon.transport.address;

/**
 * Ein {@link AddressParameter} stellt einen der Parameter des jeweiligen
 * Transporsystems dar, welche noetig sind, um eindeutig einen Endpunkt zu
 * definieren.
 * <p>
 * So werden beispielsweise fuer den Transport von Daten mittels Sockets, die
 * IP-Adresse sowie der Port benoetigt um auf dieser Abstraktionschicht einen
 * Endpunkt genau zu bestimmen. IP-Address und Port waehren in diesem Beispiel
 * Parameter der jeweiligen Adresse.
 * <p>
 * Paramter besitzen einen Namen ({@link #getName()}) in Form einer
 * Zeichenkette, welcher die Bedeutung des einzelnen Parameters bestimmt, und
 * einen Wert ({@link #getValue()}, welcher die aktuelle Belegung
 * wiederspiegelt.
 * <p>
 * Der Wert wird dabei ebenfalls durch eine Zeichenkette angegeben, und es ist
 * die Aufgabe des interpretierenden Objektes, diese evtl. in andere Typen
 * umzuformen. Dies kann bis zum bestimmten Masse durch Subklassen von
 * {@link Endpoint} automatisiert werden.
 * 
 * 
 * @see Endpoint
 * @see TcpIpAddress
 */
public class AddressParameter {

	/**
	 * Der Name des jeweiligen Parameters.
	 */
	private String m_name;

	/**
	 * Der momentane Wert des Paramters.
	 */
	private String m_value;

	/**
	 * Erzeugt einen neuen {@link AddressParameter} mit dem uebergebenen Namen
	 * und Wert.
	 * 
	 * @param a_name
	 *            Der Name (Schluessel) des Parameters
	 * @param a_value
	 *            Der Wert der Parameter
	 */
	public AddressParameter(String a_name, String a_value) {
		m_name = a_name;
		m_value = a_value;
	}

	/**
	 * Erzeugt einen neuen Parameter mit dem uebergebenen Namen. Als Wert wird
	 * die leere Zeichenkette verwendet.
	 */
	public AddressParameter(String a_name) {
		this(a_name, "");
	}

	/**
	 * Liefert den Namen des Parameters.
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * Liefert den Wert des Parameters.
	 */
	public String getValue() {
		return m_value;
	}

	/**
	 * Der Hashcode des Paramters ergibt sich aus dem Hashcode des Namen, da im
	 * allgemeinen nur eindeutigkeit des Namens gefordert wird.
	 */
	public int hashCode() {
		return m_name.hashCode();
	}

}
