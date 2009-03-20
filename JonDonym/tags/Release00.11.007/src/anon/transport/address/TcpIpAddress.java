package anon.transport.address;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Eine {@link TcpIpAddress} erlaubt es einen Endunkt auf Basis einer
 * Socketverbindung eindeutig durch das Paar von Ip-Addresse und Port zu
 * bestimmen.
 */
public class TcpIpAddress implements IAddress {

	public static final String TRANSPORT_IDENTIFIER = "tcpip";

	private static final String IP_PARAMETER = "ip-address";
	private static final String PORT_PARAMETER = "port";

	/**
	 * Der Port der Adresse
	 */
	protected int m_port;

	/**
	 * Die IP-Adresse.
	 */
	protected InetAddress m_ipAddress;

	/**
	 * Erstellt eine neue {@link TcpIpAddress} auf Basis des uebergebenen Host
	 * und Portes. Intern wird dabei Versucht des Hostnamen zur nummerischen
	 * Darstellung, der IP-Addresse aufzuloesen. Sollte dies nicht gelingen, ist
	 * diese null.
	 * 
	 * @param a_host
	 *            Der Hostname oder Textdarstellung der nummerischen
	 *            Repraesentation.
	 * @param a_port
	 *            Der Port.
	 */
	public TcpIpAddress(String a_host, int a_port) {
		try {
			m_ipAddress = InetAddress.getByName(a_host);
		} catch (UnknownHostException e) {
			m_ipAddress = null;
		}

		m_port = a_port;
	}

	/**
	 * Erstellt eine neue {@link TcpIpAddress} auf Basis einer uebergebenen
	 * IP-Adresse und eines Portes.
	 */
	public TcpIpAddress(InetAddress a_ipAdress, int a_port) {
		m_ipAddress = a_ipAdress;
		m_port = a_port;
	}

	/**
	 * Erstellt eine neue {@link TcpIpAddress}, wobei die Belegung fuer Port und
	 * IP-Address aus den entsprechenden Paramtern eines uebergebenen Endpoint
	 * ermittelt wird.
	 * 
	 * @throws AddressMappingException
	 */
	public TcpIpAddress(Endpoint a_baseAddress) throws AddressMappingException {
		String value = a_baseAddress.getParameter(IP_PARAMETER);
		if (value == null)
			throw new AddressMappingException("IP Parameter is missing");
		try {
			m_ipAddress = InetAddress.getByName(value);
		} catch (UnknownHostException e) {
			throw new AddressMappingException("IP-Address could not be parsed.");
		}
		value = a_baseAddress.getParameter(PORT_PARAMETER);
		if (value == null)
			throw new AddressMappingException("Port Parameter is missing");
		try {
			m_port = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new AddressMappingException("Port could not be parsed.");
		}
	}

	/**
	 * Liefert die IP-Adresse zurueck.
	 */
	public InetAddress getIPAddress() {
		return m_ipAddress;
	}

	/**
	 * Liefert den Port zurueck.
	 */
	public int getPort() {
		return m_port;
	}

	/**
	 * Versucht ausgehend von der IP-Adresse den Hostname zu ermitteln. Dieser
	 * kann unter Umstaenden einen anderen Wert annehmen, als bei der Erstellung
	 * uebergeben.
	 */
	public String getHostname() {
		return m_ipAddress.getHostName();
	}

	public AddressParameter[] getAllParameters() {
		AddressParameter[] result = new AddressParameter[2];
		result[0] = new AddressParameter(IP_PARAMETER, m_ipAddress
				.getHostAddress());
		result[1] = new AddressParameter(PORT_PARAMETER, String.valueOf(m_port));
		return result;
	}

	public String getTransportIdentifier() {
		return TRANSPORT_IDENTIFIER;
	}
}
