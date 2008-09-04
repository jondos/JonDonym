package anon.transport.address;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Ein {@link Endpoint} stellt die Schnittstelle zwischen einer {@link IAddress}
 * und ihrer URN-Repraesentation da.
 * <p>
 * Dazu stellt sie einerseits Mechanismen bereit, eine URN zu Parsen und auf das
 * Ergebniss strukturiert zuzugreifen und andererseits eine Methode um eine
 * {@link IAddress} in eine URN umzuformen.
 */
public class Endpoint {

	/**
	 * Der Transportidentifer wie er aus der URN ermittelt wurde.
	 */
	protected String m_transportIdentifier;

	/**
	 * Liste der Parameter vom Type String,AddressParameter.
	 */
	protected Hashtable m_paramters;

	/**
	 * Liefer den Namen des Transportidentifer.
	 */
	public String getTransportIdentifier() {
		return m_transportIdentifier;
	}

	/**
	 * Wandelt eine Uebergebene {@link IAddress} in ihre URN Darstellung um. Die
	 * Umwandlung erfolgt dabei nach folgender Regel.
	 * 
	 * <pre>
	 *   EndpointURN ::= &quot;urn:endpoint:&quot;  Identifier ParameterGroup
	 *   Identifier == TransportIdentifier
	 *   ParameterGroup = Parameter | Parameter ParamterGroup
	 *   Parameter = &quot;:&quot;  ParamterName &quot;(&quot; ParamterValue  &quot;)&quot;
	 * </pre>
	 * 
	 * Identifier bezeichnet dabei den Transportidentifier der {@link IAddress}
	 * und ParameterName bzw. ParameterValue den Name bzw. Wert eines
	 * Parameters, wobei saemtliche Parameter der {@link IAddress} in die URN
	 * uebernommen werde.
	 * 
	 * @param a_address
	 *            Die Adresse welche als URN dargestellt werden soll.
	 */
	public static String toURN(IAddress a_address) {
		String identifier = a_address.getTransportIdentifier();
		StringBuffer builder = new StringBuffer();
		builder.append("urn:endpoint:");
		builder.append(identifier);
		AddressParameter[] parameters = a_address.getAllParameters();
		for (int i = 0, length = parameters.length; i < length; i++) {
			builder.append(":");
			builder.append(parameters[i].getName());
			builder.append("(");
			builder.append(parameters[i].getValue());
			builder.append(")");
		}
		return builder.toString();
	}

	/**
	 * Versucht ausgehend von der uebergeben URN eine neue Instanz von
	 * {@link Endpoint} zu erstellen, wobei Identfier und Paramter gemaess der URN
	 * bestimmt sind.
	 * 
	 * @param a_theURN
	 *            Die URN welche als Basis fuer die Erstellung der Adresse dienen
	 *            soll.
	 * @throws MalformedURNException
	 *             Wenn das Format der URN nicht den Erwartungen entspricht.
	 */
	public Endpoint(String a_theURN) throws MalformedURNException {
		StringTokenizer st=new StringTokenizer(a_theURN,":");
		String[] components = new String[st.countTokens()];
		int i=0;
		while(st.hasMoreElements())
			components[i++]=st.nextToken();
		if (components.length < 3)
			throw new MalformedURNException(
					"A valid Endpoint needs at least 3 Components");
		if (!components[0].equals("urn"))
			throw new MalformedURNException("URN must start with \"urn:\"");
		if (!components[1].equals("endpoint"))
			throw new MalformedURNException(
					"Can only handle Endpoint-Namespace. Is " + components[1]);
		m_transportIdentifier = components[2];

		// get Parameters
		m_paramters = new Hashtable();
		int length=components.length;
		for (i = 3; i < length; i++) {
			int parentheseIndex = components[i].indexOf("(");
			int decrementedLength = components[i].length() - 1;
			String name = components[i].substring(0, parentheseIndex);
			++parentheseIndex;
			String value = components[i].substring(parentheseIndex,
					decrementedLength);
			m_paramters.put(name, new AddressParameter(name, value));
		}
	}

	/**
	 * Gibt einen bestimmten Paramter zurueck.
	 * 
	 * @param a_name
	 *            der Name des gewuenschten Paramters.
	 */
	public String getParameter(String a_name) {
		AddressParameter parameter = (AddressParameter) m_paramters.get(a_name);
		return parameter == null ? null : parameter.getValue();
	}

	/**
	 * Liefert eine Liste aller fuer diese Adresse bestimmten Paramter.
	 * 
	 * @return Eine Liste von Schluessel-Wert-Paren in Form einer Map. Der
	 *         Schluessel bestimmt dabei den Namen des Parametes und Wert seine
	 *         Belegung. Beides ist in Form von Strings anzugeben.
	 */
	public AddressParameter[] getAllParameters() {
		AddressParameter[] result = new AddressParameter[m_paramters.size()];
		Enumeration en=m_paramters.elements();
		int i=0;
		while(en.hasMoreElements())
			{
					result[i++]=(AddressParameter)en.nextElement();
			}
		return result;
	}

}
