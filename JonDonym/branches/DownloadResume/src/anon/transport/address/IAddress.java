package anon.transport.address;

/**
 * Ueber eine {@link IAddress} werden die Endpunkte einer
 * Kommunikationsbeziehung, ausgehend von der Bezeichnung des jeweiligen
 * Transportmediums ({@link #getTransportIdentifier()}) und der Liste der
 * notwendigen Parameter ({@link #getAllParameters()}), eindeutigt bestimmt.
 */
public interface IAddress {

	/**
	 * Liefert den Identifier des Transportmediums zurueck.
	 * 
	 * @return Der Identifer des Transportmediums. Es muss dafuer Sorge getragen
	 *         werden, das der Rueckgabewert nie den Wert null annimmt. Im
	 *         Notfall sollte auf den leeren String zurueckgegriffen werden.
	 */
	String getTransportIdentifier();

	/**
	 * Gibt eine Liste saemtlicher Parameter der Adresse zurueck.
	 * 
	 * @return Die Liste aller Parameter der Adresse. Es muss dafuer Sorge
	 *         getragen werden, das der Rueckgabewert nie den Wert null annimmt.
	 *         Im Notfall sollte eine Array der Laenge 0 zurueckgegeben werden..
	 */
	AddressParameter[] getAllParameters();
}
