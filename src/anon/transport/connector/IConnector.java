package anon.transport.connector;

import anon.transport.address.IAddress;
import anon.transport.connection.CommunicationException;
import anon.transport.connection.ConnectionException;
import anon.transport.connection.IConnection;
import anon.transport.connection.RequestException;

/**
 * Ein Connector stellt die Funktionalitaet bereit um auf Basis eines bestimmten
 * Adresstype Verbindungen einzurichten. Er ist im generellen an einen
 * bestimmten Transportmechanismus gebunden und kapselt die darauf aufbauenden
 * Befehle zur Bereitsstellung und Initialisierung der Verbindung.
 * 
 * @param <AdressType>
 *            Die Klasse von Adressen, welche zur Bestimmung des Endpunktes
 *            herangezogen werden.
 * 
 * @param <ConnectionType>
 *            Eine von {@link IConnection} abgeleitet Schnittstelle, welche die
 *            Art der zurueckgegeben Verbindung bestimmt.
 */
public interface IConnector {

	/**
	 * Zentrale Methode der Schnittstelle, welche auf Basis der uebergebenen
	 * Adresse versucht, den entfernte Endpunkt zu kontaktieren und eine
	 * Verbindung einzurichten.
	 * <p>
	 * Sollte dies erfolgreich sein, wird die entsprechende Verbindung
	 * zurueckgegeben. Andernfalls wird das Scheitern des Versuches durch eine
	 * Ausnahme angezeigt
	 * 
	 * @param address
	 *            Die Adresse des zu kontaktierenden Endpunktes
	 * 
	 * @throws ConnectionException
	 *             wird geworfen, wenn Aufgrund verschiedener Probleme keine
	 *             Verbindung eingerichtet werden konnte.
	 *             <p>
	 *             Zur genaueren Beschreibung des Fehlers, sollte
	 *             {@link CommunicationException} zur Anzeige von Fehlern bei
	 *             der Kommunikation mit dem Endpunkt (ungueltige Adresse, Fehler
	 *             im Transportmedium) verwendetet werden und Instanzen von
	 *             {@link RequestException} um anzuzeigen, dass der
	 *             Verbindungsversuch vom Endpunkt abgelehnt wurde.
	 */
	IConnection connect(IAddress a_address) throws ConnectionException;
}
