package anon.transport.connection;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Eine Konkretisierung des {@link IConnection} Interface, bei welchem die
 * Uebertragung ueber ein bidirektionales Datenstrommodell erfolgt.
 * 
 * Ein- und Ausgabestrom treten dabei ausschliesslich als Paar auf und sind
 * fester Bestandteil der Verbindung.
 * 
 * Die einzelnen Stroeme kapseln die eigentlichen Uebertragung von Daten und
 * stellen eine Schnittstelle zum garantierten und konsitenten Transport bereit.
 */
public interface IStreamConnection extends IConnection {

	/**
	 * Liefert den Eingabestrom der Verbinundung, ueber welchen kontinuierlich
	 * Daten empfangen werden koennen.
	 * 
	 * Sofern sich der Zustand der Verbindung nicht aendert, sollte diese Methode
	 * immer das selbe Objekt zurueckgeben.
	 */
	InputStream getInputStream();

	/**
	 * Liefert den Ausgabestrom der verbindung, ueber welchem Daten gesendet
	 * werden koennen.
	 * 
	 * Sofern sich der Zustand der Verbindung nicht aendert, sollte diese Methode
	 * immer das selbe Objekt zurueckgeben.
	 */
	OutputStream getOutputStream();
}
